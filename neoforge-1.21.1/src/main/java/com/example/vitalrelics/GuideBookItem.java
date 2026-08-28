package com.example.vitalrelics;

import com.example.vitalrelics.client.guide.GuideBookClient;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class GuideBookItem extends Item {
	public GuideBookItem() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(
			final Level level, final Player player,
			final InteractionHand hand) {

		if (level.isClientSide())
			GuideBookClient.open();

		return InteractionResultHolder.sidedSuccess(
				player.getItemInHand(hand),
				level.isClientSide()
		);
	}
}