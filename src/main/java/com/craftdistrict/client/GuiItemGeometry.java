package com.craftdistrict.client;

import com.craftdistrict.CraftDistrict;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Render-thread cache of the faces the normal GUI back-face cull would retain.
 * Existing quads, UVs, colors, lighting and ordering are reused without modification.
 */
public final class GuiItemGeometry {
    private static final int MAX_LISTS = 2048;
    private static final float VIEW_EPSILON = 1.0E-6F;
    private record View(float x, float y, float z, List<BakedQuad> quads) {
        boolean matches(float a, float b, float c) {
            return Math.abs(x - a) < VIEW_EPSILON && Math.abs(y - b) < VIEW_EPSILON
                    && Math.abs(z - c) < VIEW_EPSILON;
        }
    }

    private final Map<List<BakedQuad>, View> cached = new IdentityHashMap<>();
    private final Matrix4f projected = new Matrix4f();

    public static boolean eligible(ItemStack stack, ItemDisplayContext context, BakedModel model) {
        // Custom renderers/models keep complete control of their own geometry.
        return context == ItemDisplayContext.GUI && !stack.isEmpty() && model != null
                && model.getClass() == SimpleBakedModel.class && model.isGui3d()
                && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(CraftDistrict.MOD_ID)
                && !stack.hasFoil();
    }

    public List<BakedQuad> visible(List<BakedQuad> quads, Matrix4fc pose, Matrix4fc modelView, Matrix4fc projection) {
        if (quads.size() < 24) return quads;
        projected.set(projection).mul(modelView).mul(pose);
        // GUI items normally use an orthographic projection. A perspective/custom
        // view falls back rather than guessing at winding after perspective divide.
        if (projected.m03() != 0 || projected.m13() != 0 || projected.m23() != 0 || projected.m33() == 0) return quads;
        float x = projected.m10() * projected.m21() - projected.m20() * projected.m11();
        float y = projected.m20() * projected.m01() - projected.m00() * projected.m21();
        float z = projected.m00() * projected.m11() - projected.m10() * projected.m01();
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (!Float.isFinite(length) || length < 1.0E-20F) return quads;
        x /= length; y /= length; z /= length;
        View previous = cached.get(quads);
        if (previous != null) {
            // The same mesh in an animated/different GUI view must never use a
            // stale face selection. Keep the original list for that view.
            return previous.matches(x, y, z) ? previous.quads : quads;
        }
        if (cached.size() >= MAX_LISTS) return quads;
        List<BakedQuad> visible = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            if (stride < 3 || vertices.length % 4 != 0) { visible.add(quad); continue; }
            float ax = Float.intBitsToFloat(vertices[stride]) - Float.intBitsToFloat(vertices[0]);
            float ay = Float.intBitsToFloat(vertices[stride + 1]) - Float.intBitsToFloat(vertices[1]);
            float az = Float.intBitsToFloat(vertices[stride + 2]) - Float.intBitsToFloat(vertices[2]);
            float bx = Float.intBitsToFloat(vertices[2 * stride]) - Float.intBitsToFloat(vertices[0]);
            float by = Float.intBitsToFloat(vertices[2 * stride + 1]) - Float.intBitsToFloat(vertices[1]);
            float bz = Float.intBitsToFloat(vertices[2 * stride + 2]) - Float.intBitsToFloat(vertices[2]);
            float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
            float area = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            // Retain degenerate/edge-on quads and a generous numerical margin.
            if (!Float.isFinite(area) || nx * x + ny * y + nz * z >= -1.0E-4F * area) visible.add(quad);
        }
        List<BakedQuad> result = visible.size() == quads.size() ? quads : List.copyOf(visible);
        cached.put(quads, new View(x, y, z, result));
        return result;
    }

    public void clear() { cached.clear(); }
}
