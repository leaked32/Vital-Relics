


```java
public static void applyRelicEffects(
		final LivingEntity livingEntity, final List<Relic> relics) {

	for (final Relic relic : relics) {
		for (final var entry : relic.granted_effects.entrySet()) {
			final ResourceLocation id = ResourceLocation.fromNamespaceAndPath("minecraft", entry.getKey());

			final var effect = BuiltInRegistries.MOB_EFFECT.get(id);

			if (effect == null)
				continue;

			final int amplifier = Math.max(0, entry.getValue() - 1);

			livingEntity.addEffect(new MobEffectInstance(
					BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect), 240, amplifier, true, false
			));
		}
	}
}
```