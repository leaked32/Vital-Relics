package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.example.vitalrelics.common.RelicText;

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

		for (final RelicText.Text line : RelicText.tooltipLines(relic))
			tooltip.add(component(line));
	}

	@Override
	public Component getName(final ItemStack stack) {
		return component(RelicText.itemName(relic));
	}

	private static Component component(final RelicText.Text text) {
		return Component.literal(RelicText.render(text));
	}
}