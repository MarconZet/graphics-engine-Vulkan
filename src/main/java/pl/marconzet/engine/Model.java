package pl.marconzet.engine;

import org.lwjgl.BufferUtils;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import java.nio.*;

import static org.lwjgl.vulkan.VK10.*;

/**
 * @author MarconZet
 * Created 19.09.2018
 */
public class Model {
    public ByteBuffer vertices;
    public ByteBuffer indices;
    public int size = 4;

    private float[] pos = {-0.5f,-0.5f,0.5f,-0.5f,0.5f,0.5f,-0.5f,0.5f};
    private int sizePos = 2;
    private float[] colour = {1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    private int sizeColour = 3;

    private int[] index = {0, 1, 2, 2, 3, 0};

    public Model() {
        vertices = BufferUtils.createByteBuffer(4 * (pos.length + colour.length));
        FloatBuffer fb = vertices.asFloatBuffer();
        for (int i = 0; i < size; i++) {
            fb.put(pos[i*sizePos]).put(pos[i*sizePos+1]);
            fb.put(colour[i*sizeColour]).put(colour[i*sizeColour+1]).put(colour[i*sizeColour+2]);
        }
        fb.flip();

        indices = BufferUtils.createByteBuffer(index.length * 4);
        IntBuffer ib = indices.asIntBuffer();
        for (int i : index) {
            ib.put(i);
        }
        ib.flip();
    }

    VkVertexInputBindingDescription.Buffer getBindingDescription() {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.create(1);
        bindingDescription.get(0)
                .binding(0)
                .stride((sizePos + sizeColour)*4)
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        return bindingDescription;
    }

    VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.create(2);
        attributeDescriptions.get(0)
                .binding(0)
                .location(0)
                .format(VK_FORMAT_R32G32_SFLOAT)
                .offset(0);
        attributeDescriptions.get(1)
                .binding(0)
                .location(1)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(2 * 4);
        return attributeDescriptions;
    }

    public float[] getPos() {
        return pos;
    }

    public float[] getColour() {
        return colour;
    }

    public int[] getIndex() {
        return index;
    }
}
