package pl.marconzet.engine;

import org.lwjgl.BufferUtils;
import org.lwjgl.vulkan.VkLayerProperties;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkQueueFamilyProperties;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 09.09.2018
 */
public class QueueFamilyIndices {
    private int graphicsFamily = -1;

    public QueueFamilyIndices(VkPhysicalDevice physicalDevice) {
        IntBuffer pQueueFamilyCount = BufferUtils.createIntBuffer(1);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, null);
        int queueFamilyCount = pQueueFamilyCount.get(0);

        VkQueueFamilyProperties.Buffer pQueueFamilies = VkQueueFamilyProperties.calloc(queueFamilyCount);
        vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, pQueueFamilyCount, pQueueFamilies);

        int i = 0;
        while (pQueueFamilies.hasRemaining()) {
            VkQueueFamilyProperties queueFamily = pQueueFamilies.get();
            if (queueFamily.queueCount() > 0 && (queueFamily.queueFlags() & VK_QUEUE_GRAPHICS_BIT)>0 ) {
                this.graphicsFamily = i;
            }

            if (this.isComplete()) {
                break;
            }

            i++;
        }
    }

    public boolean isComplete() {
        return graphicsFamily >= 0;
    }

    public int getGraphicsFamily() {
        return graphicsFamily;
    }
}
