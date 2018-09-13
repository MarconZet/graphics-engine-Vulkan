package pl.marconzet.engine;

import org.lwjgl.BufferUtils;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static pl.marconzet.engine.VKUtil.translateVulkanResult;

/**
 * @author MarconZet
 * Created 10.09.2018
 */
public class SwapChainSupportDetails {
    private VkSurfaceCapabilitiesKHR capabilities;
    private VkSurfaceFormatKHR.Buffer formats;
    private IntBuffer presentModes;

    public SwapChainSupportDetails(VkPhysicalDevice physicalDevice, long surface) {
        capabilities = VkSurfaceCapabilitiesKHR.calloc();
        int err = vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, capabilities);
        if (err != VK_SUCCESS) {
            throw new AssertionError("Failed to get physical device surface capabilities: " + translateVulkanResult(err));
        }

        IntBuffer pFormatCount = BufferUtils.createIntBuffer(1);
        vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, null);
        int formatCount = pFormatCount.get(0);
        if (formatCount != 0) {
            formats = VkSurfaceFormatKHR.calloc(formatCount);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, pFormatCount, formats);
        }

        IntBuffer pPresentModeCount = BufferUtils.createIntBuffer(1);
        vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pPresentModeCount, null);
        int presentModeCount = pPresentModeCount.get(0);

        if (presentModeCount != 0) {
            presentModes = BufferUtils.createIntBuffer(presentModeCount);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pPresentModeCount, presentModes);
        }
    }

    public VkSurfaceFormatKHR chooseSwapSurfaceFormat() {
        if (formats.limit() == 1 && formats.get(0).format() == VK_FORMAT_UNDEFINED) {
            ByteBuffer container = BufferUtils.createByteBuffer(VkSurfaceFormatKHR.SIZEOF);
            container.putInt(VK_FORMAT_B8G8R8A8_UNORM).putInt(VK_COLOR_SPACE_SRGB_NONLINEAR_KHR);
            return new  VkSurfaceFormatKHR(container);
        }

        while(formats.hasRemaining()) {
            VkSurfaceFormatKHR availableFormat = formats.get();
            if (availableFormat.format() == VK_FORMAT_B8G8R8A8_UNORM && availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                formats.rewind();
                return availableFormat;
            }
        }
        formats.rewind();
        return formats.get(0);
    }

    int chooseSwapPresentMode() {
        int bestMode = VK_PRESENT_MODE_FIFO_KHR;
        while(presentModes.hasRemaining()) {
            int presentMode = presentModes.get();
            if (presentMode == VK_PRESENT_MODE_MAILBOX_KHR) {
                bestMode = presentMode;
                break;
            }else if (presentMode == VK_PRESENT_MODE_IMMEDIATE_KHR){
                bestMode = presentMode;
            }
        }
        presentModes.rewind();
        return bestMode;
    }

    VkExtent2D chooseSwapExtent() {
        if(capabilities.currentExtent().width() != Integer.MAX_VALUE){
            return capabilities.currentExtent();
        }else {
            VkExtent2D actualExtent = VkExtent2D.calloc()
                    .height(HelloTriangleApplication.HEIGHT)
                    .width(HelloTriangleApplication.WIDTH);
            actualExtent.width(Math.max(capabilities.maxImageExtent().width(), Math.min(capabilities.maxImageExtent().width(), actualExtent.width())));
            actualExtent.height(Math.max(capabilities.minImageExtent().height(), Math.min(capabilities.maxImageExtent().height(), actualExtent.height())));
            return actualExtent;
        }
    }

    public VkSurfaceCapabilitiesKHR getCapabilities() {
        return capabilities;
    }

    public VkSurfaceFormatKHR.Buffer getFormats() {
        return formats;
    }

    public IntBuffer getPresentModes() {
        return presentModes;
    }
}
