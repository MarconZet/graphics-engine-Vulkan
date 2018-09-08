package pl.marconzet.engine;

/**
 * @author MarconZet
 * Created 09.09.2018
 */

public class Main {

    public static void main(String[] args) {
        HelloTriangleApplication application = new HelloTriangleApplication();

        try {
            application.run();
        } catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }

        System.exit(0);
    }
}
