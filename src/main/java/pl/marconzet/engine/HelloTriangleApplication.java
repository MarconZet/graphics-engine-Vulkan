package pl.marconzet.engine;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDebugReport.*;
import static org.lwjgl.vulkan.EXTDebugReport.vkCreateDebugReportCallbackEXT;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_KHR_SURFACE_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static pl.marconzet.engine.VKUtil.ioResourceToByteBuffer;
import static pl.marconzet.engine.VKUtil.translateVulkanResult;


/**
 * @author MarconZet
 * Created 09.09.2018
 */
public class HelloTriangleApplication {
    private static final boolean ENABLE_VALIDATION_LAYERS = true;
    private static final String[] VALIDATION_LAYERS = {"VK_LAYER_LUNARG_standard_validation"};
    private static final String[] VALIDATION_LAYERS_INSTANCE_EXTENSIONS = {VK_EXT_DEBUG_REPORT_EXTENSION_NAME};
    private static final String[] INSTANCE_EXTENSIONS = {VK_KHR_SURFACE_EXTENSION_NAME};
    private static final String[] DEVICE_EXTENSIONS = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;


    private long window;
    private VkInstance instance;
    private long debugCallbackHandle;
    private long surface;
    private VkPhysicalDevice physicalDevice;
    private QueueFamilyIndices indices;
    private SwapChainSupportDetails swapChainSupportDetails;
    private VkDevice device;
    private VkQueue graphicsQueue;
    private VkQueue presentQueue;
    private long swapChain;
    private long[] swapChainImages;
    private int swapChainImageFormat;
    private VkExtent2D swapChainExtent;
    private long[] swapChainImageViews;
    private long renderPass;
    private long pipelineLayout;
    private long graphicsPipeline;


    public void run(){
        initWindow();
        initVulkan();
        mainLoop();
        cleanup();
    }

    private void initWindow() {
        glfwInit();

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);

        window = glfwCreateWindow(WIDTH, HEIGHT, "Vulkan", 0, 0);
    }

    private void initVulkan() {
        instance = createInstance();
        debugCallbackHandle = setupDebugCallback();
        surface = createSurface();
        physicalDevice = pickPhysicalDevice();
        device = createLogicalDevice();
        graphicsQueue = createDeviceQueue(indices.getGraphicsFamily());
        presentQueue = createDeviceQueue(indices.getPresentFamily());
        swapChain = createSwapChain();
        swapChainImages = getSwapChainImages();
        swapChainImageViews = createImageViews();
        renderPass = createRenderPass();
        graphicsPipeline = createGraphicsPipeline();
    }

    private long createRenderPass() {
        VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.calloc(1)
                .format(swapChainImageFormat)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.calloc(1)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        VkSubpassDescription.Buffer subpass = VkSubpassDescription.calloc(1)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorAttachmentRef);

        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(colorAttachment)
                .pSubpasses(subpass);

        LongBuffer pRenderPass = BufferUtils.createLongBuffer(1);
        int err = vkCreateRenderPass(device, renderPassInfo, null, pRenderPass);
        long renderPass = pRenderPass.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create clear render pass: " + translateVulkanResult(err));
        }
        return renderPass;
    }

    private long createGraphicsPipeline() {
        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.calloc(2);
        try {
            shaderStages.get(0).set(loadShader(device, "vert.spv", VK_SHADER_STAGE_VERTEX_BIT));
            shaderStages.get(1).set(loadShader(device, "frag.spv", VK_SHADER_STAGE_FRAGMENT_BIT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(null)
                .pVertexAttributeDescriptions(null);

        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);

        VkViewport.Buffer viewport = VkViewport.calloc(1)
                .x(0.0f)
                .y(0.0f)
                .width((float) swapChainExtent.width())
                .height((float) swapChainExtent.height())
                .minDepth(0.0f)
                .maxDepth(1.0f);

        VkOffset2D offset = VkOffset2D.calloc()
                .set(0,0);

        VkRect2D.Buffer scissor = VkRect2D.calloc(1)
                .offset(offset)
                .extent(swapChainExtent);

        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .pViewports(viewport)
                .scissorCount(1)
                .pScissors(scissor);

        VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .lineWidth(1.0f)
                .cullMode(VK_CULL_MODE_BACK_BIT)
                .frontFace(VK_FRONT_FACE_CLOCKWISE)
                .depthBiasEnable(false)
                .depthBiasConstantFactor(0.0f)
                .depthBiasClamp(0.0f)
                .depthBiasSlopeFactor(0.0f);

        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .sampleShadingEnable(false)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                .minSampleShading(1.0f)
                .pSampleMask(null)
                .alphaToCoverageEnable(false)
                .alphaToOneEnable(false);

        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.calloc(1)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                .blendEnable(false)
                .srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
                .dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
                .colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                .alphaBlendOp(VK_BLEND_OP_ADD);

        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_COPY)
                .pAttachments(colorBlendAttachment);
        colorBlending.blendConstants().put(new float[]{0f,0f,0f,0f}).flip();

        int dynamicStates[] = {
                VK_DYNAMIC_STATE_VIEWPORT,
                VK_DYNAMIC_STATE_LINE_WIDTH
        };
        IntBuffer pDynamicStates = BufferUtils.createIntBuffer(2).put(dynamicStates);
        pDynamicStates.flip();
        VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(pDynamicStates);

        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(null)
                .pPushConstantRanges(null);

        LongBuffer pPipelineLayout = BufferUtils.createLongBuffer(1);
        int err = vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout);
        pipelineLayout = pPipelineLayout.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create pipeline layout: " + translateVulkanResult(err));
        }

        VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .pStages(shaderStages)
                .pVertexInputState(vertexInputInfo)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState)
                .pRasterizationState(rasterizer)
                .pMultisampleState(multisampling)
                .pDepthStencilState(null)
                .pColorBlendState(colorBlending)
                .pDynamicState(null)
                .layout(pipelineLayout)
                .renderPass(renderPass)
                .subpass(0)
                .basePipelineHandle(VK_NULL_HANDLE)
                .basePipelineIndex(-1);

        LongBuffer pPipeline = BufferUtils.createLongBuffer(1);
        err = vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline);
        long pipeline = pPipeline.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create pipeline: " + translateVulkanResult(err));
        }
        return pipeline;
    }

    private static long loadShader(String classPath, VkDevice device) throws IOException {
        ByteBuffer shaderCode = ioResourceToByteBuffer(classPath, 1024);
        int err;
        VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pNext(NULL)
                .pCode(shaderCode)
                .flags(0);
        LongBuffer pShaderModule = memAllocLong(1);
        err = vkCreateShaderModule(device, moduleCreateInfo, null, pShaderModule);
        long shaderModule = pShaderModule.get(0);
        memFree(pShaderModule);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create shader module: " + translateVulkanResult(err));
        }
        return shaderModule;
    }

    private VkPipelineShaderStageCreateInfo loadShader(VkDevice device, String classPath, int stage) throws IOException {
        return VkPipelineShaderStageCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(stage)
                .module(loadShader(classPath, device))
                .pName(memUTF8("main"));
    }

    private long[] createImageViews() {
        long[] imageViews = new long[swapChainImages.length];
        for (int i = 0; i < imageViews.length; i++) {
            VkImageViewCreateInfo createInfo = VkImageViewCreateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(swapChainImages[i])
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(swapChainImageFormat);
            createInfo.components()
                    .r(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .g(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .b(VK_COMPONENT_SWIZZLE_IDENTITY)
                    .a(VK_COMPONENT_SWIZZLE_IDENTITY);
            createInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0)
                    .levelCount(1)
                    .baseArrayLayer(0)
                    .layerCount(1);
            LongBuffer pBufferView = BufferUtils.createLongBuffer(1);
            int err = vkCreateImageView(device, createInfo, null, pBufferView);
            imageViews[i] = pBufferView.get(0);
            if (err != VK_SUCCESS) {
                throw new AssertionError("Failed to create image view: " + translateVulkanResult(err));
            }
        }
        return imageViews;
    }

    private long createSwapChain() {
        SwapChainSupportDetails swapChainSupport = new SwapChainSupportDetails(physicalDevice, surface);

        VkSurfaceFormatKHR surfaceFormat = swapChainSupport.chooseSwapSurfaceFormat();
        int presentMode = swapChainSupport.chooseSwapPresentMode();
        swapChainExtent = swapChainSupport.chooseSwapExtent();
        swapChainImageFormat = surfaceFormat.format();

        int imageCount = swapChainSupport.getCapabilities().minImageCount() + 1;
        if(swapChainSupport.getCapabilities().maxImageCount() > 0){
            imageCount = Math.min(imageCount, swapChainSupport.getCapabilities().maxImageCount());
        }

        VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.calloc()
                .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                .surface(surface)
                .minImageCount(imageCount)
                .imageFormat(surfaceFormat.format())
                .imageColorSpace(surfaceFormat.colorSpace())
                .imageExtent(swapChainExtent)
                .imageArrayLayers(1)
                .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
                .preTransform(swapChainSupport.getCapabilities().currentTransform())
                .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                .presentMode(presentMode)
                .clipped(true)
                .oldSwapchain(VK_NULL_HANDLE);

        int graphicsFamily = indices.getGraphicsFamily();
        int presentFamily = indices.getPresentFamily();
        IntBuffer queueFamilyIndices = BufferUtils.createIntBuffer(2)
                .put(graphicsFamily)
                .put(presentFamily);
        queueFamilyIndices.flip();
        if (graphicsFamily != presentFamily) {
            createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .pQueueFamilyIndices(queueFamilyIndices);
        } else {
            createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .pQueueFamilyIndices(null);
        }

        LongBuffer pSwapChain = BufferUtils.createLongBuffer(1);
        int err = vkCreateSwapchainKHR(device, createInfo, null, pSwapChain);
        long swapChain = pSwapChain.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create swap chain: " + translateVulkanResult(err));
        }




        return swapChain;
    }

    private long[] getSwapChainImages() {
        IntBuffer pImageCount = BufferUtils.createIntBuffer(1);
        vkGetSwapchainImagesKHR(device, swapChain, pImageCount, null);
        LongBuffer swapChainImages = BufferUtils.createLongBuffer(pImageCount.get(0));
        vkGetSwapchainImagesKHR(device, swapChain, pImageCount, swapChainImages);
        long[] res = new long[pImageCount.get(0)];
        swapChainImages.get(res);
        return res;
    }

    private long createSurface() {
        LongBuffer pSurface = memAllocLong(1);
        int err = glfwCreateWindowSurface(instance, window, null, pSurface);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create surface: " + translateVulkanResult(err));
        }
        return pSurface.get();
    }

    private VkQueue createDeviceQueue(int queueFamilyIndex) {
        PointerBuffer pQueue = BufferUtils.createPointerBuffer(1);
        vkGetDeviceQueue(device, queueFamilyIndex, 0, pQueue);
        return new VkQueue(pQueue.get(), device);
    }

    private VkDevice createLogicalDevice() {
        FloatBuffer pQueuePriorities = BufferUtils.createFloatBuffer(1).put(1.0f);
        pQueuePriorities.flip();
        VkDeviceQueueCreateInfo.Buffer pQueueCreateInfo = VkDeviceQueueCreateInfo.calloc(2).put(
                VkDeviceQueueCreateInfo.calloc()
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(indices.getGraphicsFamily())
                        .pQueuePriorities(pQueuePriorities)
        ).put(
                VkDeviceQueueCreateInfo.calloc()
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(indices.getPresentFamily())
                        .pQueuePriorities(pQueuePriorities)
        );
        pQueueCreateInfo.flip();

        PointerBuffer ppExtensionNames = BufferUtils.createPointerBuffer(DEVICE_EXTENSIONS.length);
        for (String extension : DEVICE_EXTENSIONS) {
            ppExtensionNames.put(memUTF8(extension));
        }
        ppExtensionNames.flip();

        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc();
        VkDeviceCreateInfo pCreateInfo = VkDeviceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pNext(NULL)
                .pQueueCreateInfos(pQueueCreateInfo)
                .pEnabledFeatures(deviceFeatures)
                .ppEnabledExtensionNames(ppExtensionNames);

        if(ENABLE_VALIDATION_LAYERS) {
            PointerBuffer ppValidationLayers = BufferUtils.createPointerBuffer(VALIDATION_LAYERS.length);
            for (String layer : VALIDATION_LAYERS) {
                ppValidationLayers.put(memUTF8(layer));
            }
            pCreateInfo.ppEnabledLayerNames(ppValidationLayers.flip());
        }
        PointerBuffer pDevice = BufferUtils.createPointerBuffer(1);
        int err = vkCreateDevice(physicalDevice, pCreateInfo, null, pDevice);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create device: " + translateVulkanResult(err));
        }
        return new VkDevice(pDevice.get(0), physicalDevice, pCreateInfo);
    }

    private VkPhysicalDevice pickPhysicalDevice() {
        IntBuffer pDeviceCount = BufferUtils.createIntBuffer(1);
        vkEnumeratePhysicalDevices(instance, pDeviceCount, null);
        int deviceCount = pDeviceCount.get(0);

        if (deviceCount == 0) {
            throw new RuntimeException("failed to find GPUs with Vulkan support!");
        }

        PointerBuffer pDevices = BufferUtils.createPointerBuffer(deviceCount);
        vkEnumeratePhysicalDevices(instance, pDeviceCount, pDevices);
        while(pDevices.hasRemaining()){
            VkPhysicalDevice physicalDevice = new VkPhysicalDevice(pDevices.get(), instance);
            if(isDeviceSuitable(physicalDevice))
                return physicalDevice;
        }

        throw new RuntimeException("failed to find a suitable GPU!");
    }

    private boolean isDeviceSuitable(VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.calloc();
        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.calloc();
        vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);
        vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);

        swapChainSupportDetails = new SwapChainSupportDetails(physicalDevice, surface);
        boolean swapChainGood = swapChainSupportDetails.getFormats().sizeof() > 0 && swapChainSupportDetails.getPresentModes().hasRemaining();



        indices = new QueueFamilyIndices(physicalDevice, surface);

        return deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU
                && deviceFeatures.geometryShader()
                && indices.isComplete()
                && checkDeviceExtensionSupport(physicalDevice)
                && swapChainGood;
    }

    private boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        IntBuffer pExtensionCount = BufferUtils.createIntBuffer(1);
        vkEnumerateDeviceExtensionProperties(device, (CharSequence) null, pExtensionCount, null);
        int extensionCount = pExtensionCount.get(0);
        VkExtensionProperties.Buffer pAvailableExtensions = VkExtensionProperties.calloc(extensionCount);
        vkEnumerateDeviceExtensionProperties(device, (CharSequence) null, pExtensionCount, pAvailableExtensions);


        int found = 0;
        for (String extension : DEVICE_EXTENSIONS) {
            for (VkExtensionProperties extensionProperties : pAvailableExtensions) {
                if(extensionProperties.extensionNameString().equals(extension)){
                    found++;
                    break;
                }
            }
        }

        return found == DEVICE_EXTENSIONS.length;
    }


    private long setupDebugCallback() {
        VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
            public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
                System.out.println("validation layer: " + VkDebugReportCallbackEXT.getString(pMessage));
                return 0;
            }
        };
        return setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);
    }

    private long setupDebugging(VkInstance instance, int flags, VkDebugReportCallbackEXT callback) {
        VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.calloc()
                .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                .pNext(NULL)
                .pfnCallback(callback)
                .pUserData(NULL)
                .flags(flags);
        LongBuffer pCallback = memAllocLong(1);
        int err = vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, null, pCallback);
        long callbackHandle = pCallback.get(0);
        memFree(pCallback);
        dbgCreateInfo.free();
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create VkInstance: " + translateVulkanResult(err));
        }
        return callbackHandle;
    }

    private VkInstance createInstance() {
        if (ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport()) {
            throw new RuntimeException("validation layers requested, but not available!");
        }

        VkApplicationInfo appInfo = VkApplicationInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(memUTF8("Hello Triangle"))
                .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(memUTF8("No Engine"))
                .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                .apiVersion(VK_API_VERSION_1_0);

        PointerBuffer ppEnabledExtensionNames = getInstanceExtensions();
        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames);

        if(ENABLE_VALIDATION_LAYERS){
            PointerBuffer ppValidationLayers = BufferUtils.createPointerBuffer(VALIDATION_LAYERS.length);
            for (String layer : VALIDATION_LAYERS) {
                ppValidationLayers.put(memUTF8(layer));
            }
            pCreateInfo.ppEnabledLayerNames(ppValidationLayers.flip());
        }

        PointerBuffer pInstance = BufferUtils.createPointerBuffer(1);
        int err = vkCreateInstance(pCreateInfo, null, pInstance);
        long instance = pInstance.get();
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create VkInstance: " + translateVulkanResult(err));
        }

        return new VkInstance(instance, pCreateInfo);
    }

    private PointerBuffer getInstanceExtensions() {
        IntBuffer pExtensionCount = BufferUtils.createIntBuffer(1);
        vkEnumerateInstanceExtensionProperties((CharSequence) null, pExtensionCount, null);
        int extensionCount = pExtensionCount.get(0);
        VkExtensionProperties.Buffer ppExtensions = VkExtensionProperties.calloc(extensionCount);
        vkEnumerateInstanceExtensionProperties((CharSequence) null, pExtensionCount, ppExtensions);

        PointerBuffer ppRequiredExtensions = glfwGetRequiredInstanceExtensions();
        if (ppRequiredExtensions == null) {
            throw new AssertionError("Failed to find list of required Vulkan extensions");
        }

        int capacity = ppExtensions.remaining() + INSTANCE_EXTENSIONS.length;
        if(ENABLE_VALIDATION_LAYERS) {
            capacity += VALIDATION_LAYERS_INSTANCE_EXTENSIONS.length;
        }

        PointerBuffer ppEnabledExtensionNames = BufferUtils.createPointerBuffer(capacity);
        ppEnabledExtensionNames.put(ppRequiredExtensions);
        for (String extension : INSTANCE_EXTENSIONS) {
            ppEnabledExtensionNames.put(memUTF8(extension));
        }
        if(ENABLE_VALIDATION_LAYERS) {
            for (String extension : VALIDATION_LAYERS_INSTANCE_EXTENSIONS) {
                ppEnabledExtensionNames.put(memUTF8(extension));
            }
        }
        ppEnabledExtensionNames.flip();

        return ppEnabledExtensionNames;
    }

    private boolean checkValidationLayerSupport() {
        IntBuffer pLayerCount = BufferUtils.createIntBuffer(1);
        vkEnumerateInstanceLayerProperties(pLayerCount, null);
        int layerCount = pLayerCount.get(0);

        VkLayerProperties.Buffer pAvailableLayers = VkLayerProperties.calloc(layerCount);
        vkEnumerateInstanceLayerProperties(pLayerCount, pAvailableLayers);
        /*System.out.println("Available layers:");
        while (pAvailableLayers.hasRemaining()) {
            System.out.println(pAvailableLayers.get().layerNameString());
        }
        pAvailableLayers.rewind();*/
        for (String validationLayer : VALIDATION_LAYERS) {
            boolean found = false;
            while(pAvailableLayers.hasRemaining()) {
                if(pAvailableLayers.get().layerNameString().equals(validationLayer)) {
                    found = true;
                    break;
                }
            }
            if(!found)
                return false;
            pAvailableLayers.rewind();
        }
        return true;
    }

    private void mainLoop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
    }

    private void cleanup() {
        vkDestroyPipeline(device, graphicsPipeline, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
        vkDestroyRenderPass(device, renderPass, null);
        for (long imageView : swapChainImageViews) {
            vkDestroyImageView(device, imageView, null);
        }
        vkDestroySwapchainKHR(device, swapChain, null);
        vkDestroyDevice(device, null);
        vkDestroyDebugReportCallbackEXT(instance, debugCallbackHandle, null);
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
