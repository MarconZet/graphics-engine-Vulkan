package pl.marconzet.engine;

import java.nio.FloatBuffer;

/**
 * @author MarconZet
 * Created 28.09.2018
 */
public class Vertex {
    private float[] coordinate;
    private float[] color;
    private float[] texCord;

    public Vertex(float x, float y, float z, float u, float v) {
        coordinate = new float[]{x,y,z};
        color = new float[]{0,0,0};
        texCord = new float[]{u,v};
    }

    public void put(FloatBuffer fb){
        fb.put(coordinate).put(color).put(texCord);
    }

    public int coordinateOffset(){
        return 0;
    }

    public int colorOffset(){
        return 4 * coordinate.length;
    }

    public int texCordOffset(){
        return 4 * (coordinate.length + color.length);
    }


    public int sizeOf(){
        return 4 * (coordinate.length + color.length + texCord.length);
    }
}
