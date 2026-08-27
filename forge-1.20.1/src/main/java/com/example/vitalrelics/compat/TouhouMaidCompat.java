package com.example.vitalrelics.compat;

import com.example.vitalrelics.VitalRelics;
import com.example.vitalrelics.common.Relic;
import com.github.tartaricacid.touhoulittlemaid.api.ILittleMaid;
import com.github.tartaricacid.touhoulittlemaid.api.LittleMaidExtension;
import com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;

import static com.example.vitalrelics.Utils.addRelic;
@LittleMaidExtension
public final class TouhouMaidCompat implements ILittleMaid {

	public static final class RelicMaidBauble implements IMaidBauble {
	}
	@Override
	public void bindMaidBauble(final BaubleManager manager) {
		for (int i = 0; i < VitalRelics.RELIC_ITEMS.size(); ++i) {
			final var item = VitalRelics.RELIC_ITEMS.get(i).get();
			final var relic = VitalRelics.loader.relics_.get(i);

			if (relic.effective_slots.isEmpty() ||
					relic.effective_slots.contains("in_touhou_little_maid_curios_slots")) {
				manager.bind(item, new RelicMaidBauble());
			}
		}
	}

	public static void gatherMaidRelics(
			final LivingEntity entity,
			final List<Relic> out) {

		if (!(entity instanceof EntityMaid maid))
			return;

		final var baubles = maid.getMaidBauble();

		for (int i = 0; i < baubles.getSlots(); ++i) {
			final ItemStack stack = baubles.getStackInSlot(i);

			addRelic(out, stack, "in_touhou_little_maid_curios_slots");
		}
	}
}
