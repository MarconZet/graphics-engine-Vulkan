package pl.marconzet.engine;

import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

/**
 * @author MarconZet
 * Created 25.09.2018
 */
public class UniformBufferObject {
    public ByteBuffer data;

    private Matrix4f modelMatrix;
    private Matrix4f viewMatrix;
    private Matrix4f projMatrix;

    public UniformBufferObject(Matrix4f modelMatrix, Matrix4f viewMatrix, Matrix4f projMatrix) {
        this.modelMatrix = modelMatrix;
        this.viewMatrix = viewMatrix;
        this.projMatrix = projMatrix;

        data = BufferUtils.createByteBuffer((int)sizeOf());
        FloatBuffer fb = data.asFloatBuffer();
        modelMatrix.get(fb);
        fb.position(16);
        viewMatrix.get(fb);
        fb.position(32);
        projMatrix.get(fb);
    }

    public static long sizeOf(){
        return 3 * 4 * 4 * 4;
    }
}
