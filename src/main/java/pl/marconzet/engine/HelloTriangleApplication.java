package pl.marconzet.engine;

import javafx.util.Pair;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.glfw.GLFWVulkan.glfwGetRequiredInstanceExtensions;
import static org.lwjgl.stb.STBImage.STBI_rgb_alpha;
import static org.lwjgl.stb.STBImage.stbi_image_free;
import static org.lwjgl.stb.STBImage.stbi_load;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.system.libc.LibCString.memcpy;
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
    private static final String[] VALIDATION_LAYERS = {"VK_LAYER_LUNARG_standard_validation", "VK_LAYER_RENDERDOC_Capture"};
    private static final String[] VALIDATION_LAYERS_INSTANCE_EXTENSIONS = {VK_EXT_DEBUG_REPORT_EXTENSION_NAME};
    private static final String[] INSTANCE_EXTENSIONS = {VK_KHR_SURFACE_EXTENSION_NAME};
    private static final String[] DEVICE_EXTENSIONS = {VK_KHR_SWAPCHAIN_EXTENSION_NAME};

    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;

    private static long startTime;
    private Model rectangle = new Model();

    private static final int MAX_FRAMES_IN_FLIGHT = 2;
    private int currentFrame = 0;

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
    private long descriptorSetLayout;
    private List<Long> shaderModules = new ArrayList<>();
    private long pipelineLayout;
    private long graphicsPipeline;
    private long[] swapChainFramebuffers;
    private long commandPool;
    private long textureImage;
    private long textureImageMemory;
    private long textureImageView;
    private long textureSampler;
    private long vertexBuffer;
    private long vertexBufferMemory;
    private long indexBuffer;
    private long indexBufferMemory;
    private long[] uniformBuffers;
    private long[] uniformBuffersMemory;
    private long descriptorPool;
    private long[] descriptorSets;
    private VkCommandBuffer[] commandBuffers;
    private long[] imageAvailableSemaphore;
    private long[] renderFinishedSemaphore;
    private long[] inFlightFences;


    public void run() {
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
        createInstance();
        if(ENABLE_VALIDATION_LAYERS)
            setupDebugCallback();
        createSurface();
        pickPhysicalDevice();
        createLogicalDevice();
        createDeviceQueues();
        createSwapChain();
        getSwapChainImages();
        createImageViews();
        createRenderPass();
        createDescriptorSetLayout();
        createGraphicsPipeline();
        createFramebuffers();
        createCommandPoll();
        createTextureImage();
        createTextureImageView();
        createTextureSampler();
        createVertexBuffer();
        createIndexBuffer();
        createUniformBuffers();
        createDescriptorPoll();
        createDescriptorSets();
        createCommandBuffers();
        createSyncObjects();
    }

    private void createTextureSampler() {
        VkSamplerCreateInfo samplerCreateInfo = VkSamplerCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(VK_FILTER_LINEAR)
                .minFilter(VK_FILTER_LINEAR)
                .addressModeU(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeV(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .addressModeW(VK_SAMPLER_ADDRESS_MODE_REPEAT)
                .anisotropyEnable(true)
                .maxAnisotropy(16)
                .borderColor(VK_BORDER_COLOR_INT_OPAQUE_BLACK)
                .unnormalizedCoordinates(false)
                .compareEnable(false)
                .compareOp(VK_COMPARE_OP_ALWAYS)
                .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                .mipLodBias(0f)
                .minLod(0f)
                .maxLod(0f);

        LongBuffer pointer = BufferUtils.createLongBuffer(1);
        int err = vkCreateSampler(device, samplerCreateInfo, null, pointer);
        if(err != VK_SUCCESS){
            throw new RuntimeException("Failed to create texture sampler: " + translateVulkanResult(err));
        }
        textureSampler = pointer.get(0);
    }

    private void createTextureImageView() {
        textureImageView = createImageView(textureImage, VK_FORMAT_R8G8B8A8_UNORM);
    }

    private long createImageView(long image, int format) {
        VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(format);
        viewInfo.subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);

        LongBuffer pointer = BufferUtils.createLongBuffer(1);
        int err = vkCreateImageView(device, viewInfo, null, pointer);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create texture image view");
        }
        return pointer.get(0);
    }

    private void copyBufferToImage(long buffer, long image, int width, int height){
        VkCommandBuffer commandBuffer = beginSingleTimeCommands();

        VkBufferImageCopy.Buffer region = VkBufferImageCopy.create(1)
                .bufferOffset(0)
                .bufferRowLength(0)
                .bufferImageHeight(0);

        region.imageSubresource()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .mipLevel(0)
                .baseArrayLayer(0)
                .layerCount(1);

        region.imageOffset().set(0,0,0);
        region.imageExtent().set(width, height, 1);

        vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

        endSingleTimeCommands(commandBuffer);
    }

    private void transitionImageLayout(long image, long format, int oldLayout, int newLayout) {
        VkCommandBuffer commandBuffer = beginSingleTimeCommands();

        VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.create(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image);

        barrier.subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0)
                .levelCount(1)
                .baseArrayLayer(0)
                .layerCount(1);

        barrier.srcAccessMask(0).dstAccessMask(0);

        int sourceStage;
        int destinationStage;

        if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {
            barrier.srcAccessMask(0).dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

            sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
            destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
        } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
            destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
        } else {
            throw new RuntimeException("Unsupported layout transition");
        }

        vkCmdPipelineBarrier(commandBuffer, sourceStage, destinationStage, 0, null, null, barrier);

        endSingleTimeCommands(commandBuffer);
    }

    private VkCommandBuffer beginSingleTimeCommands(){
        VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandPool(commandPool)
                .commandBufferCount(1);

        PointerBuffer pointer = BufferUtils.createPointerBuffer(1);
        vkAllocateCommandBuffers(device, allocateInfo, pointer);
        VkCommandBuffer commandBuffer = new VkCommandBuffer(pointer.get(0), device);

        VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

        vkBeginCommandBuffer(commandBuffer, beginInfo);

        return commandBuffer;
    }


    private void createTextureImage() {
        IntBuffer[] textureInfo = new IntBuffer[3];
        for (int i = 0; i < textureInfo.length; i++) {
            textureInfo[i] = BufferUtils.createIntBuffer(1);
        }
        String path = HelloTriangleApplication.class.getResource("jp2.png").getFile().substring(1);
        ByteBuffer pixels = stbi_load(path, textureInfo[0], textureInfo[1], textureInfo[2], STBI_rgb_alpha);
        int texWidth = textureInfo[0].get();
        int texHeight = textureInfo[1].get();
        int texChannels = textureInfo[2].get();
        long imageSize = texWidth * texHeight * 4;
        if(pixels == null){
            throw new RuntimeException("Failed ot load texture image");
        }

        Pair<Long, Long> buffer = createBuffer(imageSize, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT);
        long stagingBuffer = buffer.getKey();
        long stagingBufferMemory = buffer.getValue();
        PointerBuffer pData = BufferUtils.createPointerBuffer(1);
        vkMapMemory(device, stagingBufferMemory, 0, imageSize, 0, pData);
        long data = pData.get(0);
        memCopy(memAddress(pixels), data, imageSize);
        vkUnmapMemory(device, stagingBufferMemory);

        stbi_image_free(pixels);

        Pair<Long, Long> imageBuffer = createImage(
                texWidth,
                texHeight,
                VK_FORMAT_R8G8B8A8_UNORM,
                VK_IMAGE_TILING_OPTIMAL,
                VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        );
        textureImage = imageBuffer.getKey();
        textureImageMemory = imageBuffer.getValue();

        transitionImageLayout(textureImage, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
        copyBufferToImage(stagingBuffer, textureImage, texWidth, texHeight);
        transitionImageLayout(textureImage, VK_FORMAT_R8G8B8A8_UNORM, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        vkDestroyBuffer(device, stagingBuffer, null);
        vkFreeMemory(device, stagingBufferMemory, null);
    }

    private Pair<Long, Long> createImage(int width, int height, int format, int tilting, int usage, int properties) {
        VkImageCreateInfo imageCreateInfo = VkImageCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .mipLevels(1)
                .arrayLayers(1)
                .format(format)
                .tiling(tilting)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .usage(usage)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .flags(0);

        imageCreateInfo.extent()
                .width(width)
                .height(height)
                .depth(1);

        LongBuffer pointer = BufferUtils.createLongBuffer(1);
        int err = vkCreateImage(device, imageCreateInfo, null, pointer);
        if(err != VK_SUCCESS){
            throw new RuntimeException("Failed to create image: " + translateVulkanResult(err));
        }
        long image = pointer.get(0);

        VkMemoryRequirements memoryRequirements = VkMemoryRequirements.create();
        vkGetImageMemoryRequirements(device, image, memoryRequirements);

        VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .allocationSize(memoryRequirements.size())
                .memoryTypeIndex(findMemoryType(memoryRequirements.memoryTypeBits(), properties));

        err = vkAllocateMemory(device, allocateInfo, null, pointer);
        if(err != VK_SUCCESS){
            throw new RuntimeException("FAiled to allocate memory: " + translateVulkanResult(err));
        }
        long imageMemory = pointer.get(0);

        vkBindImageMemory(device, image, imageMemory, 0);

        return new Pair<>(image, imageMemory);
    }

    private void createDescriptorSets() {
        LongBuffer layouts = BufferUtils.createLongBuffer(swapChainImages.length);
        for (int i = 0; i < swapChainImages.length; i++) {
            layouts.put(descriptorSetLayout);
        }
        layouts.flip();
        VkDescriptorSetAllocateInfo allocateInfo = VkDescriptorSetAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(descriptorPool)
                .pSetLayouts(layouts);


        LongBuffer array = BufferUtils.createLongBuffer(swapChainImages.length);
        int err = vkAllocateDescriptorSets(device, allocateInfo, array);
        if(err != VK_SUCCESS){
            throw new RuntimeException("Failed to create descriptor sets: " + translateVulkanResult(err));
        }
        descriptorSets = new long[swapChainImages.length];
        array.get(descriptorSets);

        for (int i = 0; i < swapChainImages.length; i++) {
            VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.create(1)
                    .buffer(uniformBuffers[i])
                    .offset(0)
                    .range(UniformBufferObject.sizeOf());

            VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.create(1)
                    .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
                    .imageView(textureImageView)
                    .sampler(textureSampler);

            VkWriteDescriptorSet.Buffer writeDescriptor = VkWriteDescriptorSet.create(2);
            writeDescriptor.get(0)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[i])
                    .dstBinding(0)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                    .pBufferInfo(bufferInfo);

            writeDescriptor.get(1)
                    .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                    .dstSet(descriptorSets[i])
                    .dstBinding(1)
                    .dstArrayElement(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .pImageInfo(imageInfo);

            vkUpdateDescriptorSets(device, writeDescriptor, null);
        }
    }

    private void createDescriptorPoll() {
        VkDescriptorPoolSize.Buffer poolSize = VkDescriptorPoolSize.create(2);
        poolSize.get(0)
                .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(swapChainImages.length);
        poolSize.get(1)
                .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(swapChainImages.length);

        VkDescriptorPoolCreateInfo poolCreateInfo = VkDescriptorPoolCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .pPoolSizes(poolSize)
                .maxSets(swapChainImages.length)
                .flags(0);

        LongBuffer pointer = BufferUtils.createLongBuffer(1);
        int err = vkCreateDescriptorPool(device, poolCreateInfo, null, pointer);
        if(err != VK_SUCCESS){
            throw new RuntimeException("Failed to create descriptor pool: " + translateVulkanResult(err));
        }
        descriptorPool = pointer.get(0);


    }

    private void createUniformBuffers() {
        long bufferSize = UniformBufferObject.sizeOf();

        uniformBuffers = new long[swapChainImages.length];
        uniformBuffersMemory = new long[swapChainImages.length];

        for (int i = 0; i < swapChainImages.length; i++) {
            Pair<Long, Long> buffer = createBuffer(
                    bufferSize,
                    VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
            );
            uniformBuffers[i] = buffer.getKey();
            uniformBuffersMemory[i] = buffer.getValue();
        }
    }

    private void createDescriptorSetLayout() {
        VkDescriptorSetLayoutBinding.Buffer layoutBinding = VkDescriptorSetLayoutBinding.create(2);
        layoutBinding.get(0)
                .binding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT)
                .pImmutableSamplers(null);
        layoutBinding.get(1)
                .binding(1)
                .descriptorCount(1)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .pImmutableSamplers(null)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

        VkDescriptorSetLayoutCreateInfo layoutCreateInfo = VkDescriptorSetLayoutCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(layoutBinding);

        LongBuffer p = BufferUtils.createLongBuffer(1);
        int err = vkCreateDescriptorSetLayout(device, layoutCreateInfo, null, p);
        if(err != VK_SUCCESS){
            throw new RuntimeException("Failed to create descriptor set layout: " + translateVulkanResult(err));
        }
        descriptorSetLayout = p.get(0);
    }

    private void createIndexBuffer() {
        long bufferSize = rectangle.indices.remaining();
        Pair<Long, Long> buffer = createBuffer(
                bufferSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );
        long stagingBuffer = buffer.getKey();
        long stagingBufferMemory = buffer.getValue();

        PointerBuffer pData = BufferUtils.createPointerBuffer(1);
        vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, pData);
        long data = pData.get(0);
        memCopy(memAddress(rectangle.indices), data, bufferSize);
        vkUnmapMemory(device, stagingBufferMemory);

        buffer = createBuffer(
                bufferSize,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        );
        indexBuffer = buffer.getKey();
        indexBufferMemory = buffer.getValue();

        copyBuffer(stagingBuffer, indexBuffer, bufferSize);

        vkDestroyBuffer(device, stagingBuffer, null);
        vkFreeMemory(device, stagingBufferMemory, null);
    }

    private void createVertexBuffer() {
        long bufferSize = rectangle.vertices.remaining();
        Pair<Long, Long> buffer = createBuffer(
                bufferSize,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT
        );
        long stagingBuffer = buffer.getKey();
        long stagingBufferMemory = buffer.getValue();

        PointerBuffer pData = BufferUtils.createPointerBuffer(1);
        vkMapMemory(device, stagingBufferMemory, 0, bufferSize, 0, pData);
        long data = pData.get(0);
        memCopy(memAddress(rectangle.vertices), data, bufferSize);
        vkUnmapMemory(device, stagingBufferMemory);

        buffer = createBuffer(
                bufferSize,
                VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
        );
        vertexBuffer = buffer.getKey();
        vertexBufferMemory = buffer.getValue();

        copyBuffer(stagingBuffer, vertexBuffer, bufferSize);

        vkDestroyBuffer(device, stagingBuffer, null);
        vkFreeMemory(device, stagingBufferMemory, null);
    }

    private void copyBuffer(long srcBuffer, long dstBuffer, long size){
        VkCommandBuffer commandBuffer = beginSingleTimeCommands();

        VkBufferCopy.Buffer copyRegion = VkBufferCopy.create(1)
                .srcOffset(0)
                .dstOffset(0)
                .size(size);
        vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);

        endSingleTimeCommands(commandBuffer);
    }

    private void endSingleTimeCommands(VkCommandBuffer commandBuffer) {
        vkEndCommandBuffer(commandBuffer);

        PointerBuffer pointerBuffer = BufferUtils.createPointerBuffer(1).put(0, commandBuffer);
        VkSubmitInfo submitInfo = VkSubmitInfo.create()
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(pointerBuffer);

        vkQueueSubmit(graphicsQueue, submitInfo, VK_NULL_HANDLE);
        vkQueueWaitIdle(graphicsQueue);
        vkFreeCommandBuffers(device, commandPool, pointerBuffer);
    }

    private Pair<Long, Long> createBuffer(long size, int usage, int properties) {
        VkBufferCreateInfo bufferCreateInfo = VkBufferCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .pNext(NULL)
                .size(size)
                .usage(usage)
                .flags(0)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

        LongBuffer lPointer = BufferUtils.createLongBuffer(1);
        int err = vkCreateBuffer(device, bufferCreateInfo, null, lPointer);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create vertex buffer: " + translateVulkanResult(err));
        }
        long buffer = lPointer.get(0);

        VkMemoryRequirements memoryRequirements = VkMemoryRequirements.create();
        vkGetBufferMemoryRequirements(device, buffer, memoryRequirements);

        VkMemoryAllocateInfo allocateInfo = VkMemoryAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                .pNext(NULL)
                .allocationSize(memoryRequirements.size())
                .memoryTypeIndex(
                        findMemoryType(
                                memoryRequirements.memoryTypeBits(),
                                properties
                        )
                );

        err = vkAllocateMemory(device, allocateInfo, null, lPointer);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate vertex buffer memory: " + translateVulkanResult(err));
        }
        long bufferMemory = lPointer.get(0);

        vkBindBufferMemory(device, buffer, bufferMemory, 0);

        return new Pair<>(buffer,bufferMemory);
    }

    private int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memoryProperties = VkPhysicalDeviceMemoryProperties.create();
        vkGetPhysicalDeviceMemoryProperties(physicalDevice, memoryProperties);
        for (int i = 0; i < memoryProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) > 0
                    && (memoryProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }
        throw new RuntimeException("Failed to find suitable memory type");
    }

    private void createDeviceQueues() {
        graphicsQueue = createDeviceQueue(indices.getGraphicsFamily());
        presentQueue = createDeviceQueue(indices.getPresentFamily());
    }

    private void createSyncObjects() {
        imageAvailableSemaphore = new long[MAX_FRAMES_IN_FLIGHT];
        renderFinishedSemaphore = new long[MAX_FRAMES_IN_FLIGHT];
        inFlightFences = new long[MAX_FRAMES_IN_FLIGHT];
        VkSemaphoreCreateInfo semaphoreCreateInfo = VkSemaphoreCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

        VkFenceCreateInfo fenceCreateInfo = VkFenceCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK_FENCE_CREATE_SIGNALED_BIT);

        LongBuffer pointer = BufferUtils.createLongBuffer(1);
        int err;
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            err = vkCreateSemaphore(device, semaphoreCreateInfo, null, pointer);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create semaphore: " + translateVulkanResult(err));
            }
            imageAvailableSemaphore[i] = pointer.get(0);
            err = vkCreateSemaphore(device, semaphoreCreateInfo, null, pointer);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create semaphore: " + translateVulkanResult(err));
            }
            renderFinishedSemaphore[i] = pointer.get(0);
            err = vkCreateFence(device, fenceCreateInfo, null, pointer);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create fence: " + translateVulkanResult(err));
            }
            inFlightFences[i] = pointer.get(0);
        }

    }

    private void createCommandBuffers() {
        VkCommandBuffer[] commandBuffers = new VkCommandBuffer[swapChainFramebuffers.length];
        VkCommandBufferAllocateInfo allocateInfo = VkCommandBufferAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(commandBuffers.length);

        PointerBuffer pointerBuffer = BufferUtils.createPointerBuffer(commandBuffers.length);
        int err = vkAllocateCommandBuffers(device, allocateInfo, pointerBuffer);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to allocate command buffers: " + translateVulkanResult(err));
        }

        for (int i = 0; i < commandBuffers.length; i++) {
            long l = pointerBuffer.get();
            commandBuffers[i] = new VkCommandBuffer(l, device);
        }

        for (int i = 0; i < commandBuffers.length; i++) {
            VkCommandBufferBeginInfo bufferBeginInfo = VkCommandBufferBeginInfo.create()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)
                    .pInheritanceInfo(null);

            err = vkBeginCommandBuffer(commandBuffers[i], bufferBeginInfo);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to begin recording command buffer: " + translateVulkanResult(err));
            }

            VkClearValue.Buffer clearValues = VkClearValue.create(1);
            clearValues.color()
                    .float32(0, 100 / 255.0f)
                    .float32(1, 149 / 255.0f)
                    .float32(2, 237 / 255.0f)
                    .float32(3, 1.0f);

            VkRenderPassBeginInfo renderPassBeginInfo = VkRenderPassBeginInfo.create()
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(swapChainFramebuffers[i])
                    .pClearValues(clearValues);
            renderPassBeginInfo.renderArea()
                    .offset(VkOffset2D.create().set(0, 0))
                    .extent(swapChainExtent);

            vkCmdBeginRenderPass(commandBuffers[i], renderPassBeginInfo, VK_SUBPASS_CONTENTS_INLINE);

            vkCmdBindPipeline(commandBuffers[i], VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

            LongBuffer vertexBuffers = BufferUtils.createLongBuffer(1).put(0, vertexBuffer);
            LongBuffer offsets = BufferUtils.createLongBuffer(1).put(0, 0);
            vkCmdBindVertexBuffers(commandBuffers[i], 0, vertexBuffers, offsets);

            vkCmdBindIndexBuffer(commandBuffers[i], indexBuffer, 0, VK_INDEX_TYPE_UINT32);

            LongBuffer descriptorSet = BufferUtils.createLongBuffer(1).put(0, descriptorSets[i]);
            vkCmdBindDescriptorSets(commandBuffers[i], VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineLayout, 0, descriptorSet, null);

            vkCmdDrawIndexed(commandBuffers[i], rectangle.getIndex().length, 1, 0, 0, 0);

            vkCmdEndRenderPass(commandBuffers[i]);

            err = vkEndCommandBuffer(commandBuffers[i]);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to record command buffer: " + translateVulkanResult(err));
            }

        }

        this.commandBuffers = commandBuffers;
    }

    private void createCommandPoll() {
        VkCommandPoolCreateInfo createInfo = VkCommandPoolCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .queueFamilyIndex(indices.getGraphicsFamily())
                .flags(0);


        LongBuffer pCommandPoll = BufferUtils.createLongBuffer(1);
        int err = vkCreateCommandPool(device, createInfo, null, pCommandPoll);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to create command pool:" + translateVulkanResult(err));
        }
        commandPool = pCommandPoll.get();
    }

    private void createFramebuffers() {
        long[] framebuffers = new long[swapChainImageViews.length];
        for (int i = 0; i < swapChainImageViews.length; i++) {
            LongBuffer attachments = BufferUtils.createLongBuffer(1).put(swapChainImageViews[i]);
            attachments.flip();

            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.create()
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(renderPass)
                    .pAttachments(attachments)
                    .width(swapChainExtent.width())
                    .height(swapChainExtent.height())
                    .layers(1);

            LongBuffer pFramebuffer = BufferUtils.createLongBuffer(1);
            int err = vkCreateFramebuffer(device, framebufferInfo, null, pFramebuffer);
            if (err != VK_SUCCESS) {
                throw new RuntimeException("Failed to create framebuffer: " + translateVulkanResult(err));
            }
            framebuffers[i] = pFramebuffer.get(0);
        }
        swapChainFramebuffers = framebuffers;
    }

    private void createRenderPass() {
        VkAttachmentDescription.Buffer colorAttachment = VkAttachmentDescription.create(1)
                .format(swapChainImageFormat)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

        VkAttachmentReference.Buffer colorAttachmentRef = VkAttachmentReference.create(1)
                .attachment(0)
                .layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

        VkSubpassDescription.Buffer subpass = VkSubpassDescription.create(1)
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorAttachmentRef);

        VkSubpassDependency.Buffer dependency = VkSubpassDependency.create(1)
                .srcSubpass(VK_SUBPASS_EXTERNAL)
                .dstSubpass(0)
                .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

        VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(colorAttachment)
                .pSubpasses(subpass).pDependencies(dependency);

        LongBuffer pRenderPass = BufferUtils.createLongBuffer(1);
        int err = vkCreateRenderPass(device, renderPassInfo, null, pRenderPass);
        long renderPass = pRenderPass.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create clear render pass: " + translateVulkanResult(err));
        }
        this.renderPass = renderPass;
    }

    private void createGraphicsPipeline() {
        VkPipelineShaderStageCreateInfo.Buffer shaderStages = VkPipelineShaderStageCreateInfo.create(2);
        try {
            shaderStages.get(0).set(loadShader(device, "vert.spv", VK_SHADER_STAGE_VERTEX_BIT));
            shaderStages.get(1).set(loadShader(device, "frag.spv", VK_SHADER_STAGE_FRAGMENT_BIT));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        VkPipelineVertexInputStateCreateInfo vertexInputInfo = VkPipelineVertexInputStateCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pNext(NULL)
                .pVertexBindingDescriptions(rectangle.getBindingDescription())
                .pVertexAttributeDescriptions(rectangle.getAttributeDescriptions());

        VkPipelineInputAssemblyStateCreateInfo inputAssembly = VkPipelineInputAssemblyStateCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .pNext(NULL)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false);

        VkViewport.Buffer viewport = VkViewport.create(1)
                .x(0.0f)
                .y(0.0f)
                .width((float) swapChainExtent.width())
                .height((float) swapChainExtent.height())
                .minDepth(0.0f)
                .maxDepth(1.0f);

        VkOffset2D offset = VkOffset2D.create()
                .set(0, 0);

        VkRect2D.Buffer scissor = VkRect2D.create(1)
                .offset(offset)
                .extent(swapChainExtent);

        VkPipelineViewportStateCreateInfo viewportState = VkPipelineViewportStateCreateInfo.create()
                .pNext(NULL)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .pViewports(viewport)
                .scissorCount(1)
                .pScissors(scissor);

        VkPipelineRasterizationStateCreateInfo rasterizer = VkPipelineRasterizationStateCreateInfo.create()
                .pNext(NULL)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .lineWidth(1.0f)
                .cullMode(VK_CULL_MODE_NONE)
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .depthBiasEnable(false)
                .depthBiasConstantFactor(0.0f)
                .depthBiasClamp(0.0f)
                .depthBiasSlopeFactor(0.0f);

        VkPipelineMultisampleStateCreateInfo multisampling = VkPipelineMultisampleStateCreateInfo.create()
                .pNext(NULL)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .sampleShadingEnable(false)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                .minSampleShading(1.0f)
                .pSampleMask(null)
                .alphaToCoverageEnable(false)
                .alphaToOneEnable(false);

        VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = VkPipelineColorBlendAttachmentState.create(1)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT)
                .blendEnable(false)
                .srcColorBlendFactor(VK_BLEND_FACTOR_ONE)
                .dstColorBlendFactor(VK_BLEND_FACTOR_ZERO)
                .colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                .alphaBlendOp(VK_BLEND_OP_ADD);

        VkPipelineColorBlendStateCreateInfo colorBlending = VkPipelineColorBlendStateCreateInfo.create()
                .pNext(NULL)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_COPY)
                .pAttachments(colorBlendAttachment);
        colorBlending.blendConstants().put(new float[]{0f, 0f, 0f, 0f}).flip();

        int dynamicStates[] = {
                VK_DYNAMIC_STATE_LINE_WIDTH
        };
        IntBuffer pDynamicStates = BufferUtils.createIntBuffer(2).put(dynamicStates);
        pDynamicStates.flip();
        VkPipelineDynamicStateCreateInfo dynamicState = VkPipelineDynamicStateCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(pDynamicStates);

        VkPipelineLayoutCreateInfo pipelineLayoutInfo = VkPipelineLayoutCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pSetLayouts(BufferUtils.createLongBuffer(1).put(0, descriptorSetLayout))
                .pPushConstantRanges(null);

        LongBuffer pPipelineLayout = BufferUtils.createLongBuffer(1);
        int err = vkCreatePipelineLayout(device, pipelineLayoutInfo, null, pPipelineLayout);
        pipelineLayout = pPipelineLayout.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create pipeline layout: " + translateVulkanResult(err));
        }

        VkGraphicsPipelineCreateInfo.Buffer pipelineInfo = VkGraphicsPipelineCreateInfo.create(1)
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .pStages(shaderStages)
                .pVertexInputState(vertexInputInfo)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState)
                .pRasterizationState(rasterizer)
                .pMultisampleState(multisampling)
                .pDepthStencilState(null)
                .pColorBlendState(colorBlending)
                .pDynamicState(dynamicState)
                .layout(pipelineLayout)
                .renderPass(renderPass)
                .subpass(0)
                .basePipelineHandle(VK_NULL_HANDLE)
                .basePipelineIndex(0);

        LongBuffer pPipeline = BufferUtils.createLongBuffer(1);
        err = vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline);
        long pipeline = pPipeline.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create pipeline: " + translateVulkanResult(err));
        }
        graphicsPipeline = pipeline;
    }

    private static long loadShader(String classPath, VkDevice device) throws IOException {
        ByteBuffer shaderCode = ioResourceToByteBuffer(classPath, 1024);
        int err;
        VkShaderModuleCreateInfo moduleCreateInfo = VkShaderModuleCreateInfo.create()
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
        long shaderModule = loadShader(classPath, device);
        shaderModules.add(shaderModule);
        return VkPipelineShaderStageCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(stage)
                .module(shaderModule)
                .pName(memUTF8("main"));
    }

    private void createImageViews() {
        swapChainImageViews = new long[swapChainImages.length];
        for (int i = 0; i < swapChainImageViews.length; i++) {
            swapChainImageViews[i] = createImageView(swapChainImages[i], swapChainImageFormat);
        }
    }

    private void createSwapChain() {
        SwapChainSupportDetails swapChainSupport = new SwapChainSupportDetails(physicalDevice, surface);

        VkSurfaceFormatKHR surfaceFormat = swapChainSupport.chooseSwapSurfaceFormat();
        int presentMode = swapChainSupport.chooseSwapPresentMode();
        swapChainExtent = swapChainSupport.chooseSwapExtent();
        swapChainImageFormat = surfaceFormat.format();

        int imageCount = swapChainSupport.getCapabilities().minImageCount() + 1;
        if (swapChainSupport.getCapabilities().maxImageCount() > 0) {
            imageCount = Math.min(imageCount, swapChainSupport.getCapabilities().maxImageCount());
        }

        VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.create()
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


        this.swapChain = swapChain;
    }

    private void getSwapChainImages() {
        IntBuffer pImageCount = BufferUtils.createIntBuffer(1);
        vkGetSwapchainImagesKHR(device, swapChain, pImageCount, null);
        LongBuffer swapChainImages = BufferUtils.createLongBuffer(pImageCount.get(0));
        vkGetSwapchainImagesKHR(device, swapChain, pImageCount, swapChainImages);
        long[] res = new long[pImageCount.get(0)];
        swapChainImages.get(res);
        this.swapChainImages = res;
    }

    private void createSurface() {
        LongBuffer pSurface = memAllocLong(1);
        int err = glfwCreateWindowSurface(instance, window, null, pSurface);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create surface: " + translateVulkanResult(err));
        }
        surface = pSurface.get();
    }

    private VkQueue createDeviceQueue(int queueFamilyIndex) {
        PointerBuffer pQueue = BufferUtils.createPointerBuffer(1);
        vkGetDeviceQueue(device, queueFamilyIndex, 0, pQueue);
        return new VkQueue(pQueue.get(), device);
    }

    private void createLogicalDevice() {
        FloatBuffer pQueuePriorities = BufferUtils.createFloatBuffer(1).put(1.0f);
        pQueuePriorities.flip();
        VkDeviceQueueCreateInfo.Buffer pQueueCreateInfo = VkDeviceQueueCreateInfo.create(2).put(
                VkDeviceQueueCreateInfo.create()
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(indices.getGraphicsFamily())
                        .pQueuePriorities(pQueuePriorities)
        ).put(
                VkDeviceQueueCreateInfo.create()
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

        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.create()
                .samplerAnisotropy(true);
        VkDeviceCreateInfo pCreateInfo = VkDeviceCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pNext(NULL)
                .pQueueCreateInfos(pQueueCreateInfo)
                .pEnabledFeatures(deviceFeatures)
                .ppEnabledExtensionNames(ppExtensionNames);

        if (ENABLE_VALIDATION_LAYERS) {
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
        device = new VkDevice(pDevice.get(0), physicalDevice, pCreateInfo);
    }

    private void pickPhysicalDevice() {
        IntBuffer pDeviceCount = BufferUtils.createIntBuffer(1);
        vkEnumeratePhysicalDevices(instance, pDeviceCount, null);
        int deviceCount = pDeviceCount.get(0);

        if (deviceCount == 0) {
            throw new RuntimeException("failed to find GPUs with Vulkan support!");
        }

        PointerBuffer pDevices = BufferUtils.createPointerBuffer(deviceCount);
        vkEnumeratePhysicalDevices(instance, pDeviceCount, pDevices);
        while (pDevices.hasRemaining()) {
            VkPhysicalDevice physicalDevice = new VkPhysicalDevice(pDevices.get(), instance);
            if (isDeviceSuitable(physicalDevice)) {
                this.physicalDevice = physicalDevice;
                return;
            }
        }

        throw new RuntimeException("failed to find a suitable GPU!");
    }

    private boolean isDeviceSuitable(VkPhysicalDevice physicalDevice) {
        VkPhysicalDeviceProperties deviceProperties = VkPhysicalDeviceProperties.create();
        VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.create();
        vkGetPhysicalDeviceProperties(physicalDevice, deviceProperties);
        vkGetPhysicalDeviceFeatures(physicalDevice, deviceFeatures);

        swapChainSupportDetails = new SwapChainSupportDetails(physicalDevice, surface);
        boolean swapChainGood = swapChainSupportDetails.getFormats().sizeof() > 0 && swapChainSupportDetails.getPresentModes().hasRemaining();


        indices = new QueueFamilyIndices(physicalDevice, surface);

        return deviceProperties.deviceType() == VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU
                && deviceFeatures.samplerAnisotropy()
                && indices.isComplete()
                && checkDeviceExtensionSupport(physicalDevice)
                && swapChainGood;
    }

    private boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        IntBuffer pExtensionCount = BufferUtils.createIntBuffer(1);
        vkEnumerateDeviceExtensionProperties(device, (CharSequence) null, pExtensionCount, null);
        int extensionCount = pExtensionCount.get(0);
        VkExtensionProperties.Buffer pAvailableExtensions = VkExtensionProperties.create(extensionCount);
        vkEnumerateDeviceExtensionProperties(device, (CharSequence) null, pExtensionCount, pAvailableExtensions);


        int found = 0;
        for (String extension : DEVICE_EXTENSIONS) {
            for (VkExtensionProperties extensionProperties : pAvailableExtensions) {
                if (extensionProperties.extensionNameString().equals(extension)) {
                    found++;
                    break;
                }
            }
        }

        return found == DEVICE_EXTENSIONS.length;
    }


    private void setupDebugCallback() {
        VkDebugReportCallbackEXT debugCallback = new VkDebugReportCallbackEXT() {
            public int invoke(int flags, int objectType, long object, long location, int messageCode, long pLayerPrefix, long pMessage, long pUserData) {
                System.out.println("validation layer: " + VkDebugReportCallbackEXT.getString(pMessage));
                return 0;
            }
        };
        debugCallbackHandle = setupDebugging(instance, VK_DEBUG_REPORT_ERROR_BIT_EXT | VK_DEBUG_REPORT_WARNING_BIT_EXT, debugCallback);
    }

    private long setupDebugging(VkInstance instance, int flags, VkDebugReportCallbackEXT callback) {
        VkDebugReportCallbackCreateInfoEXT dbgCreateInfo = VkDebugReportCallbackCreateInfoEXT.create()
                .sType(VK_STRUCTURE_TYPE_DEBUG_REPORT_CALLBACK_CREATE_INFO_EXT)
                .pNext(NULL)
                .pfnCallback(callback)
                .pUserData(NULL)
                .flags(flags);
        LongBuffer pCallback = BufferUtils.createLongBuffer(1);
        int err = vkCreateDebugReportCallbackEXT(instance, dbgCreateInfo, null, pCallback);
        long callbackHandle = pCallback.get(0);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to create VkInstance: " + translateVulkanResult(err));
        }
        return callbackHandle;
    }

    private void createInstance() {
        if (ENABLE_VALIDATION_LAYERS && !checkValidationLayerSupport()) {
            throw new RuntimeException("validation layers requested, but not available!");
        }

        VkApplicationInfo appInfo = VkApplicationInfo.create()
                .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(memUTF8("Hello Triangle"))
                .applicationVersion(VK_MAKE_VERSION(1, 0, 0))
                .pEngineName(memUTF8("No Engine"))
                .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                .apiVersion(VK_API_VERSION_1_0);

        PointerBuffer ppEnabledExtensionNames = getInstanceExtensions();
        VkInstanceCreateInfo pCreateInfo = VkInstanceCreateInfo.create()
                .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(appInfo)
                .ppEnabledExtensionNames(ppEnabledExtensionNames);

        if (ENABLE_VALIDATION_LAYERS) {
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

        this.instance = new VkInstance(instance, pCreateInfo);
    }

    private PointerBuffer getInstanceExtensions() {
        IntBuffer pExtensionCount = BufferUtils.createIntBuffer(1);
        vkEnumerateInstanceExtensionProperties((CharSequence) null, pExtensionCount, null);
        int extensionCount = pExtensionCount.get(0);
        VkExtensionProperties.Buffer ppExtensions = VkExtensionProperties.create(extensionCount);
        vkEnumerateInstanceExtensionProperties((CharSequence) null, pExtensionCount, ppExtensions);

        PointerBuffer ppRequiredExtensions = glfwGetRequiredInstanceExtensions();
        if (ppRequiredExtensions == null) {
            throw new AssertionError("Failed to find list of required Vulkan extensions");
        }

        int capacity = ppExtensions.remaining() + INSTANCE_EXTENSIONS.length;
        if (ENABLE_VALIDATION_LAYERS) {
            capacity += VALIDATION_LAYERS_INSTANCE_EXTENSIONS.length;
        }

        PointerBuffer ppEnabledExtensionNames = BufferUtils.createPointerBuffer(capacity);
        ppEnabledExtensionNames.put(ppRequiredExtensions);
        for (String extension : INSTANCE_EXTENSIONS) {
            ppEnabledExtensionNames.put(memUTF8(extension));
        }
        if (ENABLE_VALIDATION_LAYERS) {
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

        VkLayerProperties.Buffer pAvailableLayers = VkLayerProperties.create(layerCount);
        vkEnumerateInstanceLayerProperties(pLayerCount, pAvailableLayers);
        for (String validationLayer : VALIDATION_LAYERS) {
            boolean found = false;
            while (pAvailableLayers.hasRemaining()) {
                if (pAvailableLayers.get().layerNameString().equals(validationLayer)) {
                    found = true;
                    break;
                }
            }
            if (!found)
                return false;
            pAvailableLayers.rewind();
        }
        return true;
    }

    private void mainLoop() {
        startTime = System.currentTimeMillis();
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            drawFrame();
        }

        vkDeviceWaitIdle(device);
    }

    private void drawFrame() {
        vkWaitForFences(device, inFlightFences[currentFrame], true, Long.MAX_VALUE);
        vkResetFences(device, inFlightFences[currentFrame]);

        IntBuffer pointer = BufferUtils.createIntBuffer(1);
        vkAcquireNextImageKHR(device, swapChain, Long.MAX_VALUE, imageAvailableSemaphore[currentFrame], VK_NULL_HANDLE, pointer);
        int imageIndex = pointer.get(0);

        updateUniformBuffer(imageIndex);

        LongBuffer waitSemaphores = BufferUtils.createLongBuffer(1).put(0, imageAvailableSemaphore[currentFrame]);
        LongBuffer signalSemaphores = BufferUtils.createLongBuffer(1).put(0, renderFinishedSemaphore[currentFrame]);
        IntBuffer waitStages = BufferUtils.createIntBuffer(1).put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
        VkSubmitInfo submitInfo = VkSubmitInfo.create()
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .waitSemaphoreCount(1)
                .pWaitSemaphores(waitSemaphores)
                .pWaitDstStageMask(waitStages)
                .pCommandBuffers(BufferUtils.createPointerBuffer(1).put(0, commandBuffers[imageIndex]))
                .pSignalSemaphores(signalSemaphores);

        int err = vkQueueSubmit(graphicsQueue, submitInfo, inFlightFences[currentFrame]);
        if (err != VK_SUCCESS) {
            throw new RuntimeException("Failed to submit draw command buffer: " + translateVulkanResult(err));
        }

        VkPresentInfoKHR presentInfo = VkPresentInfoKHR.create()
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(signalSemaphores)
                .swapchainCount(1)
                .pSwapchains(BufferUtils.createLongBuffer(1).put(0, swapChain))
                .pImageIndices(pointer);

        err = vkQueuePresentKHR(presentQueue, presentInfo);

        vkQueueWaitIdle(presentQueue);

        currentFrame = (currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;

    }

    private void updateUniformBuffer(int currentImage) {
        long currentTime = System.currentTimeMillis();
        float time = currentTime - startTime;
        time /= 1000;

        Matrix4f transformation = new Matrix4f().translate(0, 0, 1).rotateZ(time * (float)Math.PI/2);
        Matrix4f view = new Matrix4f().translate(0f, -0.7f, 0.2f).rotateX(-(float)Math.PI/4);
        Matrix4f projection = new Matrix4f().perspectiveLH(
                (float)Math.PI/2,
                (float)swapChainExtent.width()/swapChainExtent.height(),
                0.1f, 10f);

        UniformBufferObject ubo = new UniformBufferObject(transformation, view, projection);
        //ubo = new UniformBufferObject(new Matrix4f(), new Matrix4f(), new Matrix4f());

        PointerBuffer pData = BufferUtils.createPointerBuffer(1);
        vkMapMemory(device, uniformBuffersMemory[currentImage], 0, ubo.sizeOf(), 0, pData);
        long data = pData.get(0);
        memCopy(memAddress(ubo.data), data, ubo.sizeOf());
        vkUnmapMemory(device, uniformBuffersMemory[currentImage]);

    }

    private void cleanup() {
        for (int i = 0; i < MAX_FRAMES_IN_FLIGHT; i++) {
            vkDestroySemaphore(device, renderFinishedSemaphore[i], null);
            vkDestroySemaphore(device, imageAvailableSemaphore[i], null);
            vkDestroyFence(device, inFlightFences[i], null);
        }
        cleanupSwapChain();
        for (Long shaderModule : shaderModules) {
            vkDestroyShaderModule(device, shaderModule, null);
        }
        vkDestroyCommandPool(device, commandPool, null);
        vkDestroyDescriptorPool(device, descriptorPool, null);
        vkDestroyDescriptorSetLayout(device, descriptorSetLayout, null);
        for (int i = 0; i < swapChainImages.length; i++) {
            vkDestroyBuffer(device, uniformBuffers[i], null);
            vkFreeMemory(device, uniformBuffersMemory[i], null);
        }
        vkDestroySampler(device, textureSampler, null);
        vkDestroyImageView(device, textureImageView, null);
        vkDestroyImage(device, textureImage, null);
        vkFreeMemory(device, textureImageMemory, null);
        vkDestroyBuffer(device, vertexBuffer, null);
        vkFreeMemory(device, vertexBufferMemory, null);
        vkDestroyBuffer(device, indexBuffer, null);
        vkFreeMemory(device, indexBufferMemory, null);
        vkDestroyDevice(device, null);
        if(ENABLE_VALIDATION_LAYERS)
            vkDestroyDebugReportCallbackEXT(instance, debugCallbackHandle, null);
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void cleanupSwapChain() {
        for (long framebuffer : swapChainFramebuffers) {
            vkDestroyFramebuffer(device, framebuffer, null);
        }
        PointerBuffer pointerBuffer = BufferUtils.createPointerBuffer(commandBuffers.length);
        for (VkCommandBuffer commandBuffer : commandBuffers) {
            pointerBuffer.put(commandBuffer);
        }
        pointerBuffer.flip();
        vkFreeCommandBuffers(device, commandPool, pointerBuffer);
        vkDestroyPipeline(device, graphicsPipeline, null);
        vkDestroyPipelineLayout(device, pipelineLayout, null);
        vkDestroyRenderPass(device, renderPass, null);
        for (long imageView : swapChainImageViews) {
            vkDestroyImageView(device, imageView, null);
        }
        vkDestroySwapchainKHR(device, swapChain, null);
    }
}
