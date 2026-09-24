package com.bettercontent.economy.trader;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Draws a thin, slightly draped cloth sheet instead of a block-built roof. */
public final class TraderCampPostRenderer implements BlockEntityRenderer<TraderCampPostBlockEntity> {
    private static final ResourceLocation WOOL = new ResourceLocation("minecraft", "textures/block/white_wool.png");
    private static final ResourceLocation SPRUCE = new ResourceLocation("minecraft", "textures/block/spruce_planks.png");

    public TraderCampPostRenderer(final BlockEntityRendererProvider.Context context) { }

    @Override
    public void render(final TraderCampPostBlockEntity post, final float partialTick, final PoseStack poseStack,
                       final MultiBufferSource buffers, final int packedLight, final int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        Direction facing = post.getBlockState().getValue(TraderCampPostBlock.FACING);
        poseStack.mulPose(Axis.YP.rotationDegrees(yawFor(facing)));
        VertexConsumer support = buffers.getBuffer(RenderType.entityCutoutNoCull(SPRUCE));
        drawBox(poseStack, support, -0.055F, 0.0F, -0.055F, 0.055F, 2.85F, 0.055F, 0xFFFFFFFF, packedLight);
        drawBox(poseStack, support, -0.57F, 2.75F, -0.045F, 0.57F, 2.83F, 0.045F, 0xFFFFFFFF, packedLight);
        drawBox(poseStack, support, -1.04F, 0.0F, 2.12F, -0.98F, 1.18F, 2.12F, 0xFFFFFFFF, packedLight);
        drawBox(poseStack, support, 0.98F, 0.0F, 2.12F, 1.04F, 1.18F, 2.12F, 0xFFFFFFFF, packedLight);
        drawAwning(poseStack, buffers.getBuffer(RenderType.entityCutoutNoCull(WOOL)),
                colorFor(post.themeId()), packedLight);
        poseStack.popPose();
    }

    @Override
    public boolean shouldRender(final TraderCampPostBlockEntity post, final Vec3 cameraPosition) {
        return TraderCampPostBlockEntity.renderBounds(post.getBlockPos()).distanceToSqr(cameraPosition)
                < (double) getViewDistance() * getViewDistance();
    }

    private static float yawFor(final Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 90.0F;
            case NORTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static int colorFor(final String theme) {
        return switch (theme) {
            case "wicked" -> 0xFF9D78C4;
            case "arcane" -> 0xFF757FE0;
            case "aerial" -> 0xFF8EC9E8;
            case "aqueous" -> 0xFF54B9B1;
            case "earthen" -> 0xFFB9895F;
            case "infernal" -> 0xFFC75D4D;
            case "tempo" -> 0xFFD8BE62;
            default -> 0xFFF0E7CC;
        };
    }

    private static void drawAwning(final PoseStack poseStack, final VertexConsumer consumer,
                                   final int color, final int light) {
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();
        int columns = 6;
        int rows = 5;
        for (int row = 0; row < rows; row++) {
            float v0 = row / (float) rows;
            float v1 = (row + 1) / (float) rows;
            for (int column = 0; column < columns; column++) {
                float u0 = column / (float) columns;
                float u1 = (column + 1) / (float) columns;
                float shade = 0.92F + 0.08F * (1.0F - Math.abs((u0 + u1) * 0.5F - 0.5F) * 2.0F);
                int shaded = shade(color, shade);
                vertex(consumer, pose, normal, point(u0, v0), u0, v0, shaded, light);
                vertex(consumer, pose, normal, point(u1, v0), u1, v0, shaded, light);
                vertex(consumer, pose, normal, point(u1, v1), u1, v1, shaded, light);
                vertex(consumer, pose, normal, point(u0, v1), u0, v1, shaded, light);
                vertex(consumer, pose, normal, point(u0, v1), u0, v1, shaded, light);
                vertex(consumer, pose, normal, point(u1, v1), u1, v1, shaded, light);
                vertex(consumer, pose, normal, point(u1, v0), u1, v0, shaded, light);
                vertex(consumer, pose, normal, point(u0, v0), u0, v0, shaded, light);
            }
        }
    }

    private static float[] point(final float u, final float v) {
        float x = -0.55F + 1.1F * u;
        float y = 2.78F - 1.46F * v + (float) Math.sin(Math.PI * u) * (float) Math.sin(Math.PI * v) * 0.09F;
        float z = 0.03F + 2.02F * v;
        return new float[]{x, y, z};
    }

    private static int shade(final int color, final float amount) {
        int a = color >>> 24;
        int r = Math.min(255, Math.round(((color >>> 16) & 0xFF) * amount));
        int g = Math.min(255, Math.round(((color >>> 8) & 0xFF) * amount));
        int b = Math.min(255, Math.round((color & 0xFF) * amount));
        return a << 24 | r << 16 | g << 8 | b;
    }

    private static void vertex(final VertexConsumer consumer, final Matrix4f pose, final Matrix3f normal,
                               final float[] point, final float u, final float v, final int color, final int light) {
        consumer.vertex(pose, point[0], point[1], point[2]).color(color)
                .uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                .normal(normal, 0.0F, 1.0F, 0.0F).endVertex();
    }

    private static void drawBox(final PoseStack stack, final VertexConsumer consumer,
                                final float x0, final float y0, final float z0,
                                final float x1, final float y1, final float z1,
                                final int color, final int light) {
        Matrix4f pose = stack.last().pose();
        Matrix3f normal = stack.last().normal();
        face(consumer, pose, normal, color, light, new float[][]{{x0,y0,z0},{x1,y0,z0},{x1,y1,z0},{x0,y1,z0}}, 0, 0, 1);
        face(consumer, pose, normal, color, light, new float[][]{{x1,y0,z1},{x0,y0,z1},{x0,y1,z1},{x1,y1,z1}}, 0, 0, -1);
        face(consumer, pose, normal, color, light, new float[][]{{x0,y0,z1},{x0,y0,z0},{x0,y1,z0},{x0,y1,z1}}, -1, 0, 0);
        face(consumer, pose, normal, color, light, new float[][]{{x1,y0,z0},{x1,y0,z1},{x1,y1,z1},{x1,y1,z0}}, 1, 0, 0);
        face(consumer, pose, normal, color, light, new float[][]{{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}}, 0, 1, 0);
        face(consumer, pose, normal, color, light, new float[][]{{x0,y0,z1},{x1,y0,z1},{x1,y0,z0},{x0,y0,z0}}, 0, -1, 0);
    }

    private static void face(final VertexConsumer consumer, final Matrix4f pose, final Matrix3f normal,
                             final int color, final int light, final float[][] points,
                             final float nx, final float ny, final float nz) {
        float[][] uv = {{0,1},{1,1},{1,0},{0,0}};
        for (int i = 0; i < points.length; i++) {
            float[] p = points[i];
            consumer.vertex(pose, p[0], p[1], p[2]).color(color).uv(uv[i][0], uv[i][1])
                    .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(normal, nx, ny, nz).endVertex();
        }
    }
}
