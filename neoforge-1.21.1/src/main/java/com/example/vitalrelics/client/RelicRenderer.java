package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.Relic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;

public final class RelicRenderer extends BlockEntityWithoutLevelRenderer {
	public RelicRenderer() {
		super(
				Minecraft.getInstance().getBlockEntityRenderDispatcher(),
				Minecraft.getInstance().getEntityModels()
		);
	}

	@Override
	public void renderByItem(
			final ItemStack stack,
			final ItemDisplayContext context,
			final PoseStack poseStack,
			final MultiBufferSource buffers,
			final int light,
			final int overlay) {

		final ResourceLocation itemId =
				BuiltInRegistries.ITEM.getKey(stack.getItem());

		if (!itemId.getNamespace().equals(Manifest.MODID))
			return;

		final Relic relic = VitalRelics.loader.find(itemId.getPath());

		if (relic == null || relic.texture == null)
			return;

		final ResourceLocation texture =
				ResourceLocation.fromNamespaceAndPath(
						Manifest.MODID,
						"textures/item/" + relic.texture
				);

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);

		final Matrix4f matrix = poseStack.last().pose();
		final VertexConsumer vertices =
				buffers.getBuffer(RenderType.entityCutoutNoCull(texture));

		final int renderLight =
				context == ItemDisplayContext.GUI
						? 0x00F000F0
						: light;

		vertex(vertices, matrix, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F, renderLight, overlay);
		vertex(vertices, matrix,  0.5F, -0.5F, 0.0F, 1.0F, 1.0F, renderLight, overlay);
		vertex(vertices, matrix,  0.5F,  0.5F, 0.0F, 1.0F, 0.0F, renderLight, overlay);
		vertex(vertices, matrix, -0.5F,  0.5F, 0.0F, 0.0F, 0.0F, renderLight, overlay);
		
		poseStack.popPose();
	}

	private static void vertex(
			final VertexConsumer vertices,
			final Matrix4f matrix,
			final float x,
			final float y,
			final float z,
			final float u,
			final float v,
			final int light,
			final int overlay) {

		vertices.addVertex(matrix, x, y, z)
				.setColor(255, 255, 255, 255)
				.setUv(u, v)
				.setOverlay(overlay)
				.setLight(light)
				.setNormal(0.0F, 0.0F, 1.0F);
	}
}