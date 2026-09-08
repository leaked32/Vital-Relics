package com.example.vitalrelics;

import com.example.vitalrelics.common.materials.MaterialLoader.Material;
import com.example.vitalrelics.common.relics.Translations;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;

/** Ordinary crafting item: deliberately not a RelicItem or a Curios item. */
public final class MaterialItem extends Item {
	private final Material material;

	public MaterialItem(final Material material, final Properties properties) {
		super(properties);
		components = DataComponentMap.builder().addAll(super.components())
				.set(DataComponents.ITEM_MODEL, ResourceLocation.fromNamespaceAndPath("vitalrelics", "relic"))
				.build();
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

	private final DataComponentMap components;

	@Override
	public DataComponentMap components() { return components; }
}
