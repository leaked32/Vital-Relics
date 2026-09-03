package com.example.vitalrelics.client;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Relic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

public final class RelicRenderer implements SpecialModelRenderer<ItemStack> {
	@Override
	public ItemStack extractArgument(final ItemStack stack) {
		return stack;
	}

	@Override
	public void render(
			final ItemStack stack,
			final ItemDisplayContext context,
			final PoseStack poseStack,
			final MultiBufferSource buffers,
			final int light,
			final int overlay,
			final boolean hasFoil) {

		if (stack == null)
			return;

		final ResourceLocation itemId =
				BuiltInRegistries.ITEM.getKey(stack.getItem());

		if (!itemId.getNamespace().equals(Manifest.MODID))
			return;

		final Relic relic = Loader.get().find(itemId.getPath());

		if (relic == null || relic.texture == null)
			return;

		final ResourceLocation texture = ExternalTextures.texture(relic.texture);

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

	@Override
	public void getExtents(final Set<Vector3f> extents) {
		extents.add(new Vector3f(0.0F, 0.0F, 0.5F));
		extents.add(new Vector3f(1.0F, 0.0F, 0.5F));
		extents.add(new Vector3f(1.0F, 1.0F, 0.5F));
		extents.add(new Vector3f(0.0F, 1.0F, 0.5F));
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

	public static final class Unbaked implements SpecialModelRenderer.Unbaked {
		public static final MapCodec<Unbaked> MAP_CODEC =
				MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<?> bake(final EntityModelSet modelSet) {
			return new RelicRenderer();
		}
	}
}
