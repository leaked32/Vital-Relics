package com.example.vitalrelics.common.platform;

import com.example.vitalrelics.common.relics.Relic;

import java.util.List;
import java.util.UUID;

public interface MyRuntimeUtils {

	// Ray-cast
	MyLivingEntity pointedLivingEntity(MyLivingEntity source, double range);
	MyVec3 safeDestinationAlongLook(MyLivingEntity entity, double range);
	MyVec3 safeHorizontalDestination(MyLivingEntity entity, double range);
	List<MyLivingEntity> entitiesIntersectingMovement(MyLivingEntity entity, MyVec3 from, MyVec3 to, double inflate);

	// HUD
	void syncSpellHud(MyLivingEntity caster, String spellId, int cooldownTicks);
	void clearSpellHud(MyLivingEntity caster);
	void showSelectedSpell(MyLivingEntity caster, String spellId);
	void showSpellCooldown(MyLivingEntity caster, String spellId, int remainingTicks);

	// Utils
	List<Relic> gatherRelics(MyLivingEntity entity);
	boolean isEntityValid(UUID uuid);
	void showCurseRequiresTarget(MyLivingEntity caster);
	void summonVisualLightning(MyLivingEntity target);
	default MyDamageSource extraDamageSource(final MyLivingEntity attacker) {
		return attacker.extraDamageSource();
	}

	// Enchantments
	enum EnchantmentFilter {ENCHANTMENT_BOOK_ONLY, ALL_ENCHANTED_ITEMS}
	boolean upgradeFirstEnchantment(MyLivingEntity entity, int experienceCost, EnchantmentFilter option);
	boolean removeCurse(MyLivingEntity entity, int experienceCost);
	boolean resetRepairCost(MyLivingEntity entity, int experienceCost);
	boolean disenchantToOffhandBook(MyLivingEntity entity, int experienceCost);
}
