package pl.marconzet.engine;

import static org.lwjgl.glfw.GLFW.*;

/**
 * @author MarconZet
 * Created 09.09.2018
 */
public class HelloTriangleApplication {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private long window;


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

    }

    private void mainLoop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
        }
    }

    private void cleanup() {
        glfwDestroyWindow(window);

        glfwTerminate();
    }
}
