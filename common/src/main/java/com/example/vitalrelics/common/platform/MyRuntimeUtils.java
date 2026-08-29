package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.relics.Relic;

import java.util.List;

public interface MyRuntimeUtils {
	List<Relic> gatherRelics(MyLivingEntity entity);

	MyLivingEntity pointedLivingEntity(
			MyLivingEntity source,
			double range
	);

	MyVec3 safeDestinationAlongLook(
			MyLivingEntity entity,
			double range
	);

	MyVec3 safeHorizontalDestination(
			MyLivingEntity entity,
			double range
	);

	List<MyLivingEntity> entitiesIntersectingMovement(
			MyLivingEntity entity,
			MyVec3 from,
			MyVec3 to,
			double inflate
	);

//	boolean upgradeFirstStoredEnchantment(
//			MyLivingEntity entity,
//			int experienceCost
//	);

	void syncSpellHud(
			MyLivingEntity caster,
			String spellId,
			int cooldownTicks);

	void clearSpellHud(MyLivingEntity caster);

	void showSelectedSpell(
			MyLivingEntity caster,
			String spellId);

	void showSpellCooldown(
			MyLivingEntity caster,
			String spellId,
			int remainingTicks);

	void showCurseRequiresTarget(MyLivingEntity caster);

	void summonVisualLightning(MyLivingEntity target);

	enum EnchantmentFilter {
		ENCHANTMENT_BOOK_ONLY, ALL_ENCHANTED_ITEMS
	}
	boolean upgradeFirstEnchantment(MyLivingEntity entity, int experienceCost, EnchantmentFilter option);

	boolean removeCurseOrResetRepairCost(MyLivingEntity entity, int experienceCost);
}
