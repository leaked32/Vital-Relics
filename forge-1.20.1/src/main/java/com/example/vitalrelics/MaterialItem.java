package com.example.vitalrelics;

import com.example.vitalrelics.common.materials.MaterialLoader.Material;
import com.example.vitalrelics.common.relics.Translations;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Ordinary crafting item: deliberately not a RelicItem or a Curios item. */
public final class MaterialItem extends Item {
	private final Material material;

	public MaterialItem(final Material material, final Properties properties) {
		super(properties);
		this.material = material;
	}

	@Override
	public Component getName(final ItemStack stack) {
		return Component.literal(Translations.get().translate("item.vitalrelics." + material.id,
				material.display_name == null ? material.id : material.display_name));
	}

	public Component description() {
		return Component.literal(Translations.get().translate(
				"tooltip.vitalrelics." + material.id, material.tooltip))
				.withStyle(net.minecraft.ChatFormatting.GRAY);
	}

	@Override
	public void initializeClient(final java.util.function.Consumer<
			net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
		consumer.accept(new com.example.vitalrelics.client.RelicClientExtensions());
	}

	@Override
	public void appendHoverText(final ItemStack stack, final net.minecraft.world.level.Level context,
			final java.util.List<Component> tooltip, final net.minecraft.world.item.TooltipFlag flag) {
		tooltip.add(description());
	}
}
