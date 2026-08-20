package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class RelicItem extends Item {
	private final Relic relic;

	public RelicItem(final Relic relic, final Properties properties) {
		super(properties);
		this.relic = relic;
	}

	@Override
	public void appendHoverText(
			final ItemStack stack,
			final TooltipContext context,
			final List<Component> tooltip,
			final TooltipFlag flag) {

		// tooltip.add(Component.literal(relic.tooltip));
		// tooltip.add(Component.literal(""));

		for (final String line : relic.getTooltipLines())
			tooltip.add(Component.literal(line));
	}

}