package com.example.vitalrelics.client;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

public final class RelicClientExtensions implements IClientItemExtensions {
	private final RelicRenderer renderer = new RelicRenderer();

	@Override
	public BlockEntityWithoutLevelRenderer getCustomRenderer() {
		return renderer;
	}
}