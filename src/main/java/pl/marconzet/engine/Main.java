package pl.marconzet.engine;

/**
 * @author MarconZet
 * Created 09.09.2018
 */

public class Main {

    private static float[] vQuad = {
            -0.5f, -0.5f, 0.0f,
            0.5f, -0.5f, 0.0f,
            0.5f, 0.5f, 0.0f,
            -0.5f, 0.5f, 0.0f,
    };

    private static float[] tQuad = {
            0f, 0f,
            1f, 0f,
            1f, 1f,
            0f, 1f
    };

    private static int[] iQuad = {
            0, 1, 2, 2, 3, 0,
    };

    public static void main(String[] args) {
        HelloTriangleApplication application = new HelloTriangleApplication();
        application.model = ObjFile.read(Main.class.getResourceAsStream("dragon.obj"), true).toModel();
        //application.model = new Model(vQuad, tQuad, iQuad, 4);

        application.textureName = "jp2.png";
        //application.textureName  = "polishFlag.png";

        try {
            application.run();
        } catch (Exception e){
            e.printStackTrace();
            System.exit(-1);
        }
        System.exit(0);
    }
}
