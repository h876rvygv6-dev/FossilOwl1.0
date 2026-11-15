package com.saif.fossilfinder.client;

import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.saif.fossilfinder.FossilFinderOwl;
import com.saif.fossilfinder.entity.FossilFinderOwlEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

final class BlathersMesh {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation MODEL = new ResourceLocation(FossilFinderOwl.MODID, "models/entity/blathers_mesh.json");
    private final ModelData data;

    private BlathersMesh(ModelData data) {
        this.data = data;
        if (data.bones.length == 0 || data.meshes.length == 0 || !Float.isFinite(data.scale)) {
            throw new IllegalStateException("Blathers model has no rig or geometry: " + MODEL);
        }
        for (int index = 0; index < data.bones.length; index++) {
            Bone bone = data.bones[index];
            if (bone.name == null || bone.local == null || bone.local.length != 16
                || bone.inverseBind == null || bone.inverseBind.length != 16
                || bone.parent < -1 || bone.parent >= index) {
                throw new IllegalStateException("Invalid Blathers bone data at index " + index);
            }
        }
        for (Mesh mesh : data.meshes) {
            if (mesh.texture == null || mesh.vertices == null || mesh.vertices.length % 9 != 0) {
                throw new IllegalStateException("Invalid Blathers mesh data");
            }
            for (int offset = 8; offset < mesh.vertices.length; offset += 9) {
                int joint = (int) mesh.vertices[offset];
                if (joint < 0 || joint >= data.bones.length) {
                    throw new IllegalStateException("Invalid Blathers skin joint index: " + joint);
                }
            }
            mesh.resourceLocation = new ResourceLocation(FossilFinderOwl.MODID, mesh.texture);
        }
    }

    static BlathersMesh load() {
        Resource resource = Minecraft.getInstance().getResourceManager().getResource(MODEL)
            .orElseThrow(() -> new IllegalStateException("Missing Blathers model resource: " + MODEL));
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            ModelData data = GSON.fromJson(reader, ModelData.class);
            if (data == null || data.bones == null || data.meshes == null || data.scale <= 0) {
                throw new IllegalStateException("Invalid Blathers model resource: " + MODEL);
            }
            return new BlathersMesh(data);
        }
        catch (IOException exception) {
            throw new IllegalStateException("Unable to read Blathers model resource: " + MODEL, exception);
        }
    }

    void render(FossilFinderOwlEntity owl, float partialTick, PoseStack poseStack,
                MultiBufferSource buffers, int packedLight) {
        float time = owl.tickCount + partialTick;
        boolean moving = owl.getDeltaMovement().lengthSqr() > 0.0004;
        float wave = Mth.sin(time * (moving ? 0.45f : 0.055f));
        float flap = wave * (moving ? 0.48f : 0.035f);
        float bob = wave * (moving ? 0.45f : 0.12f);
        float[][] skinMatrices = buildSkinMatrices(flap, bob, wave);

        poseStack.pushPose();
        try {
            poseStack.scale(data.scale, data.scale, -data.scale);
            PoseStack.Pose pose = poseStack.last();
            Matrix4f poseMatrix = pose.pose();
            Matrix3f normalMatrix = pose.normal();
            for (Mesh mesh : data.meshes) {
                VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(mesh.resourceLocation));
                for (int offset = 0; offset < mesh.vertices.length; offset += 9) {
                    float[] values = mesh.vertices;
                    float[] skin = skinMatrices[(int) values[offset + 8]];
                    float x = values[offset];
                    float y = values[offset + 1];
                    float z = values[offset + 2];
                    float nx = values[offset + 3];
                    float ny = values[offset + 4];
                    float nz = values[offset + 5];

                    float px = skin[0] * x + skin[1] * y + skin[2] * z + skin[3];
                    float py = skin[4] * x + skin[5] * y + skin[6] * z + skin[7];
                    float pz = skin[8] * x + skin[9] * y + skin[10] * z + skin[11];
                    float snx = skin[0] * nx + skin[1] * ny + skin[2] * nz;
                    float sny = skin[4] * nx + skin[5] * ny + skin[6] * nz;
                    float snz = skin[8] * nx + skin[9] * ny + skin[10] * nz;
                    float normalLength = Mth.sqrt(snx * snx + sny * sny + snz * snz);
                    if (normalLength > 0) {
                        snx /= normalLength;
                        sny /= normalLength;
                        snz /= normalLength;
                    }

                    consumer.vertex(poseMatrix, px, py, pz)
                        .color(255, 255, 255, 255)
                        .uv(values[offset + 6], values[offset + 7])
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(packedLight)
                        .normal(normalMatrix, snx, sny, snz)
                        .endVertex();
                }
            }
        }
        finally {
            poseStack.popPose();
        }
    }

    private float[][] buildSkinMatrices(float flap, float bob, float wave) {
        float[][] globals = new float[data.bones.length][];
        float[][] skinMatrices = new float[data.bones.length][];
        for (int index = 0; index < data.bones.length; index++) {
            Bone bone = data.bones[index];
            float[] local = bone.local.clone();
            switch (bone.name) {
                case "base" -> local = multiply(translation(0, bob, 0), local);
                case "Larm1" -> local = multiply(local, rotationZ(flap));
                case "Rarm1" -> local = multiply(local, rotationZ(-flap));
                case "head" -> local = multiply(local, rotationX(wave * 0.035f));
                case "Lfoot1" -> local = multiply(local, rotationX(wave * 0.12f));
                case "Rfoot1" -> local = multiply(local, rotationX(-wave * 0.12f));
                case "tail1" -> local = multiply(local, rotationX(wave * 0.08f));
                default -> { }
            }
            globals[index] = bone.parent < 0 ? local : multiply(globals[bone.parent], local);
            skinMatrices[index] = multiply(globals[index], bone.inverseBind);
        }
        return skinMatrices;
    }

    private static float[] multiply(float[] left, float[] right) {
        float[] result = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                for (int inner = 0; inner < 4; inner++) {
                    result[row * 4 + column] += left[row * 4 + inner] * right[inner * 4 + column];
                }
            }
        }
        return result;
    }

    private static float[] translation(float x, float y, float z) {
        return new float[] {1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1};
    }

    private static float[] rotationX(float angle) {
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        return new float[] {1, 0, 0, 0, 0, cosine, -sine, 0, 0, sine, cosine, 0, 0, 0, 0, 1};
    }

    private static float[] rotationZ(float angle) {
        float cosine = Mth.cos(angle);
        float sine = Mth.sin(angle);
        return new float[] {cosine, -sine, 0, 0, sine, cosine, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    }

    private static final class ModelData {
        float scale;
        Bone[] bones;
        Mesh[] meshes;
    }

    private static final class Bone {
        String name;
        int parent;
        float[] local;
        float[] inverseBind;
    }

    private static final class Mesh {
        String texture;
        float[] vertices;
        transient ResourceLocation resourceLocation;
    }
}
