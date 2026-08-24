package com.example.vitalrelics;

import com.example.vitalrelics.client.RelicClientExtensions;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicText;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static com.example.vitalrelics.common.Relic.itemDisplayName;

public class RelicItem extends Item {
	private final Relic relic;

	public RelicItem(final Relic relic, final Properties properties) {
		super(properties);
		this.relic = relic;
	}


	@Override
	public void initializeClient(
			final java.util.function.Consumer<
					net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {

		consumer.accept(new RelicClientExtensions());
	}

	@Override
	public void appendHoverText(
			final ItemStack stack,
			final Level level,
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