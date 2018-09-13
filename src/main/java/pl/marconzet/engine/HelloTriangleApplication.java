package pl.marconzet.engine;

import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

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
        vkDestroySwapchainKHR(device, swapChain, null);
        vkDestroyDevice(device, null);
        vkDestroyDebugReportCallbackEXT(instance, debugCallbackHandle, null);
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
