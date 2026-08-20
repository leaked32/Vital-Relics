package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

import static com.example.vitalrelics.common.Relic.itemDisplayName;

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

		for (final String line : relic.getTooltipLines())
			tooltip.add(Component.literal(line));
	}
	@Override
	public Component getName(final ItemStack stack) {
		if (relic.display_name != null && !relic.display_name.isBlank())
			return Component.literal(relic.display_name);

		return Component.literal(itemDisplayName(relic.id));
	}
}