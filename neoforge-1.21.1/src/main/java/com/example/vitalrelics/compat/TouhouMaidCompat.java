package com.example.vitalrelics.compat;

import com.example.vitalrelics.common.Relic;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.example.vitalrelics.Utils.addRelic;

public final class TouhouMaidCompat {
	private TouhouMaidCompat() {}

	public static void gatherMaidRelics(
			final LivingEntity entity,
			final List<Relic> out) {

		if (!(entity instanceof EntityMaid maid))
			return;

		final var baubles = maid.getMaidBauble();

		for (int i = 0; i < baubles.getSlots(); ++i) {
			final ItemStack stack = baubles.getStackInSlot(i);

			addRelic(
					out,
					stack,
					"in_touhou_little_maid_curios_slots"
			);
		}
	}
}