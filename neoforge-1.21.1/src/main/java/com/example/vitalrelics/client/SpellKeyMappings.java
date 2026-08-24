package com.example.vitalrelics.client;

import com.example.vitalrelics.VitalRelics;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
		modid = VitalRelics.MODID,
		value = Dist.CLIENT,
		bus = EventBusSubscriber.Bus.MOD
)
public final class SpellKeyMappings {
	public static final KeyMapping CAST_SPELL = new KeyMapping(
			"key.vitalrelics.cast_spell",
			InputConstants.Type.KEYSYM,
			GLFW.GLFW_KEY_Q,
			"key.categories.vitalrelics"
	);

	private SpellKeyMappings() {}

	@SubscribeEvent
	public static void register(final RegisterKeyMappingsEvent event) {
		event.register(CAST_SPELL);
	}
}
