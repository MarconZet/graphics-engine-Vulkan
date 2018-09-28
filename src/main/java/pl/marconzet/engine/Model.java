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

    private Vertex[] model;
    private int[] index;

    public Model(float[] v, float[] t, int[] indices, int size){
        index = indices;
        model = new Vertex[size];
        int j = 0, k = 0;
        for (int i = 0; i < size; i++) {
            model[i] = new Vertex(v[j++],v[j++],v[j++],t[k++],t[k++]);
        }
        fillBuffers();
    }

    private void fillBuffers() {
        vertices = BufferUtils.createByteBuffer(model.length * model[0].sizeOf());
        FloatBuffer fb = vertices.asFloatBuffer();
        for (Vertex vertex : model) {
            vertex.put(fb);
        }

        indices = BufferUtils.createByteBuffer(index.length * 4);
        IntBuffer ib = indices.asIntBuffer();
        for (int i : index) {
            ib.put(i);
        }
    }

    VkVertexInputBindingDescription.Buffer getBindingDescription() {
        VkVertexInputBindingDescription.Buffer bindingDescription = VkVertexInputBindingDescription.create(1);
        bindingDescription.get(0)
                .binding(0)
                .stride(model[0].sizeOf())
                .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);
        return bindingDescription;
    }

    VkVertexInputAttributeDescription.Buffer getAttributeDescriptions() {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions = VkVertexInputAttributeDescription.create(3);
        attributeDescriptions.get(0)
                .binding(0)
                .location(0)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(model[0].coordinateOffset());
        attributeDescriptions.get(1)
                .binding(0)
                .location(1)
                .format(VK_FORMAT_R32G32B32_SFLOAT)
                .offset(model[0].colorOffset());
        attributeDescriptions.get(2)
                .binding(0)
                .location(2)
                .format(VK_FORMAT_R32G32_SFLOAT)
                .offset(model[0].texCordOffset());
        return attributeDescriptions;
    }

    public int getIndexLength() {
        return index.length;
    }
}
