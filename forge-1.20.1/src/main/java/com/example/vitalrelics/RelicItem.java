package com.example.vitalrelics;

import com.example.vitalrelics.client.RelicClientExtensions;
import com.example.vitalrelics.common.Relic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class RelicItem extends Item {
	private final Relic relic;

	public RelicItem(final Relic relic, final Properties properties) {
		super(properties);
		this.relic = relic;
	}

	@Override
	public void appendHoverText(final ItemStack stack, final Level level,
			final List<Component> tooltip, final TooltipFlag flag) {

		for (final String line : relic.getTooltipLines())
			tooltip.add(Component.literal(line));
	}

	@Override
	public void initializeClient(
			final java.util.function.Consumer<
					net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {

		consumer.accept(new RelicClientExtensions());
	}
}