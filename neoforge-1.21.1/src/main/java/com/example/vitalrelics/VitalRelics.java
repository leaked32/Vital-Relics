package com.example.vitalrelics;

import com.example.vitalrelics.acquisition.AcquisitionEvents;
import com.example.vitalrelics.acquisition.DynamicRelicRecipe;
import com.example.vitalrelics.client.VitalClientEvents;
import com.example.vitalrelics.common.AcquisitionLoader;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import com.example.vitalrelics.common.RelicTranslations;
import com.example.vitalrelics.network.Network;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(VitalRelics.MODID)
public class VitalRelics
{
	// Define mod id in a common place for everything to reference
	public static final String MODID = "vitalrelics";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
			DeferredRegister.create(Registries.RECIPE_SERIALIZER, MODID);
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DynamicRelicRecipe>>
			DYNAMIC_RELIC_RECIPE =
			RECIPE_SERIALIZERS.register(
					"dynamic_relic",
					() -> new SimpleCraftingRecipeSerializer<>(DynamicRelicRecipe::new)
			);

	public static DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = null;

	public static RelicLoader loader = null;
	public static AcquisitionLoader acquisition = null;
	public static final List<DeferredItem<Item>> RELIC_ITEMS = new ArrayList<>();

	public VitalRelics(
			final IEventBus modEventBus,
			final Dist dist) {

		if (dist == Dist.CLIENT) {
			VitalClientEvents.registerListeners(modEventBus);
		}

		// existing constructor body...

		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(Network::registerPayloadHandlers);

		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		CREATIVE_MODE_TABS.register(modEventBus);
		RECIPE_SERIALIZERS.register(modEventBus);

		final Path config = FMLPaths.CONFIGDIR.get().resolve("vitalrelics/relics.json");
		final Path recipeConfig = FMLPaths.CONFIGDIR.get().resolve("vitalrelics/recipes.json");
		final Path translationConfig = FMLPaths.CONFIGDIR.get().resolve("vitalrelics/lang");
		loader = new RelicLoader();
		acquisition = new AcquisitionLoader();
		loader.load(config);
		acquisition.load(recipeConfig);

		RelicTranslations.INSTANCE.load(translationConfig);

		for (final var relic : loader.relics_) {
			final Rarity rarity = switch (relic.rarity.toLowerCase()) {
				case "uncommon" -> Rarity.UNCOMMON;
				case "rare" -> Rarity.RARE;
				case "epic" -> Rarity.EPIC;
				default -> Rarity.COMMON;
			};
			RELIC_ITEMS.add(ITEMS.register(
					relic.id,
					() -> new RelicItem(relic, new Item.Properties().rarity(rarity))
			));
		}

		EXAMPLE_TAB = CREATIVE_MODE_TABS.register("relics", () -> CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.vitalrelics"))
				.withTabsBefore(CreativeModeTabs.COMBAT)
				.icon(() -> RELIC_ITEMS.get(0).get().getDefaultInstance())
				.displayItems((parameters, output) -> {
					for (final var relic : RELIC_ITEMS)
						output.accept(relic.get());
				})
				.build());

		// NeoForge.EVENT_BUS.register(this);
		NeoForge.EVENT_BUS.register(VitalEvents.class);
		NeoForge.EVENT_BUS.register(AcquisitionEvents.class);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		final ResourceLocation validator =
				ResourceLocation.fromNamespaceAndPath(MODID, "relic_slot");

		CuriosApi.registerCurioPredicate(validator, result -> {
			final ItemStack stack = result.stack();

			final ResourceLocation itemId =
					BuiltInRegistries.ITEM.getKey(stack.getItem());

			if (!itemId.getNamespace().equals(MODID))
				return false;

			final Relic relic = loader.find(itemId.getPath());

			return relic != null && relic.curio_slot.equals(result.slotContext().identifier());
		});

		event.enqueueWork(() -> {
			for (final var holder : RELIC_ITEMS) {
				final Item item = holder.get();

				CuriosApi.registerCurio(item, new ICurioItem() {
					@Override
					public boolean canEquip(
							final SlotContext context,
							final ItemStack stack) {

						final ResourceLocation id =
								BuiltInRegistries.ITEM.getKey(stack.getItem());

						final Relic relic = loader.find(id.getPath());

						return relic != null &&
								relic.curio_slot.equals(context.identifier());
					}
				});
			}
		});

	}

}
