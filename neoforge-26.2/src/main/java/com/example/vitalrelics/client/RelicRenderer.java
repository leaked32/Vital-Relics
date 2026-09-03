package com.example.vitalrelics.client;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Relic;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.function.Consumer;

public final class RelicRenderer implements SpecialModelRenderer<ItemStack> {
	@Override
	public ItemStack extractArgument(final ItemStack stack) {
		return stack;
	}

	@Override
	public void submit(
			final ItemStack stack,
			final PoseStack poseStack,
			final SubmitNodeCollector collector,
			final int light,
			final int overlay,
			final boolean hasFoil,
			final int outlineColor) {

		if (stack == null)
			return;

		final Identifier itemId =
				BuiltInRegistries.ITEM.getKey(stack.getItem());

		if (!itemId.getNamespace().equals(Manifest.MODID))
			return;

		final Relic relic = Loader.get().find(itemId.getPath());

		if (relic == null || relic.texture == null)
			return;

		final Identifier texture = ExternalTextures.texture(relic.texture);

		poseStack.pushPose();
		poseStack.translate(0.5F, 0.5F, 0.5F);

		collector.submitCustomGeometry(
				poseStack,
				RenderTypes.entityCutoutNoCull(texture),
				(pose, vertices) -> {
					final Matrix4f matrix = pose.pose();
					vertex(vertices, matrix, -0.5F, -0.5F, 0.0F, 0.0F, 1.0F, light, overlay);
					vertex(vertices, matrix,  0.5F, -0.5F, 0.0F, 1.0F, 1.0F, light, overlay);
					vertex(vertices, matrix,  0.5F,  0.5F, 0.0F, 1.0F, 0.0F, light, overlay);
					vertex(vertices, matrix, -0.5F,  0.5F, 0.0F, 0.0F, 0.0F, light, overlay);
				}
		);

		poseStack.popPose();
	}

	@Override
	public void getExtents(final Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0.0F, 0.0F, 0.5F));
		output.accept(new Vector3f(1.0F, 0.0F, 0.5F));
		output.accept(new Vector3f(1.0F, 1.0F, 0.5F));
		output.accept(new Vector3f(0.0F, 1.0F, 0.5F));
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

	public static final class Unbaked implements SpecialModelRenderer.Unbaked<ItemStack> {
		public static final MapCodec<Unbaked> MAP_CODEC =
				MapCodec.unit(new Unbaked());

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}

		@Override
		public SpecialModelRenderer<ItemStack> bake(final SpecialModelRenderer.BakingContext context) {
			return new RelicRenderer();
		}
	}
}
