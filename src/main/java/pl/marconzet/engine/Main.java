package pl.marconzet.engine;

import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 09.09.2018
 */

public class Main {

    public static void main(String[] args) {
        glfwInit();

        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        long window = glfwCreateWindow(800, 600, "Vulkan window", 0, 0);

        IntBuffer extensionCount = BufferUtils.createIntBuffer(1);
        vkEnumerateInstanceExtensionProperties((ByteBuffer) null, extensionCount, null);

        System.out.println(extensionCount.get() + " extensions supported");

        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }

        glfwDestroyWindow(window);

        glfwTerminate();

        System.exit(0);
    }
}
