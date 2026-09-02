package com.example.vitalrelics;

import com.example.vitalrelics.client.guide.GuideBookClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {
	public GuideBookItem(final Item.Properties properties) {
		super(properties.stacksTo(1));
	}

	@Override
	public InteractionResult use(
			final Level level,
			final Player player,
			final InteractionHand hand) {

		if (level.isClientSide())
			GuideBookClient.open();

		return InteractionResult.SUCCESS;
	}
}