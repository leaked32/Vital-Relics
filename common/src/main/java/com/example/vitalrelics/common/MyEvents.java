package com.example.vitalrelics.common;

import com.example.vitalrelics.common.platform.MyLivingEntity;
import com.example.vitalrelics.common.platform.MyUtils;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Relic;

import java.util.List;

public class MyEvents {

	public static void onLivingEntityTick(
			MyLivingEntity myLivingEntity, final int currentTick, List<Relic> relics) {

		// Scheduled to update on each half seconds
		if (currentTick % 10 == 0) {
			MyUtils.removeImmuneEffects(myLivingEntity, relics, MyLivingEntity.MyEffectCategory.ALL);
			MyUtils.applyRelicEffects(myLivingEntity, relics);
		}


		if (currentTick % 20 == 0) {
			// Passive Skill: reality_severance

			final double reality_severance_level =
					Loader.levelOfSuchPassiveSkill(relics, Relic.PASSIVE_SKILL_REALITY_SEVERANCE);

			if (reality_severance_level > 0.0) {
				final float ratioDamage = (float) (reality_severance_level / 100.0);
				final float rangeDamage = ratioDamage * myLivingEntity.attackDamage();

				MyDamageInfo.directRangedAttack(
						myLivingEntity,
						rangeDamage,
						Math.round((float) reality_severance_level),
						1,
						Math.round((float) (reality_severance_level / 4.0))
				);
			}

			// Client HUD
			if (myLivingEntity.isServerPlayer()) {
				MySpellSystem.INSTANCE.syncSpellHud(myLivingEntity);
			}
		}

	}

}
