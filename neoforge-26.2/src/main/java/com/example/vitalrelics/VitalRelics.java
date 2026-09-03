package com.example.vitalrelics;

import com.example.vitalrelics.acquisition.AcquisitionEvents;
import com.example.vitalrelics.acquisition.DynamicRelicRecipe;
import com.example.vitalrelics.client.VitalClientEvents;
import com.example.vitalrelics.common.*;
import com.example.vitalrelics.common.MyRuntime;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Acquisition;
import com.example.vitalrelics.common.relics.Loader;
import com.example.vitalrelics.common.relics.Translations;
import com.example.vitalrelics.network.NeoNetwork;
import com.example.vitalrelics.platform.NeoRuntimeUtils;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
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
import top.theillusivec4.curios.api.CuriosSlotTypes;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Manifest.MODID)
public class VitalRelics
{
	// Define mod id in a common place for everything to reference
	// public static final String MODID = "vitalrelics";
	// Directly reference a slf4j logger
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Manifest.MODID);
	public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Manifest.MODID);
	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Manifest.MODID);
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
			DeferredRegister.create(Registries.RECIPE_SERIALIZER, Manifest.MODID);
	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<DynamicRelicRecipe>>
			DYNAMIC_RELIC_RECIPE =
			RECIPE_SERIALIZERS.register(
					"dynamic_relic",
					() -> {
						final DynamicRelicRecipe recipe = new DynamicRelicRecipe();
						return new RecipeSerializer<>(MapCodec.unit(recipe), StreamCodec.unit(recipe));
					}
			);

	public static DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = null;

	// public final static RelicLoader loader  = RelicLoader.get();
	// public final static AcquisitionLoader acquisition = AcquisitionLoader.INSTANCE;
	public static final List<DeferredItem<Item>> RELIC_ITEMS = new ArrayList<>();

	public static final DeferredItem<GuideBookItem> GUIDE_BOOK = ITEMS.registerItem("guide_book", GuideBookItem::new);

	public VitalRelics(
			final IEventBus modEventBus,
			final Dist dist) {
		MyRuntime.initialize(NeoRuntimeUtils.INSTANCE);

		if (dist == Dist.CLIENT) {
			VitalClientEvents.registerListeners(modEventBus);
		}

		// existing constructor body...

		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(NeoNetwork::registerPayloadHandlers);

		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		CREATIVE_MODE_TABS.register(modEventBus);
		RECIPE_SERIALIZERS.register(modEventBus);

		final Path config = FMLPaths.CONFIGDIR.get().resolve("vitalrelics/relics.json");
		final Path recipeConfig = FMLPaths.CONFIGDIR.get().resolve("vitalrelics/recipes.json");
		final Path translationConfig = FMLPaths.CONFIGDIR.get().resolve("vitalrelics/lang");
		Loader.load(config);
		Acquisition.load(recipeConfig);
		Translations.load(translationConfig);

		for (final var relic : Loader.get().relics_) {
			final Rarity rarity = switch (relic.rarity.toLowerCase()) {
				case "uncommon" -> Rarity.UNCOMMON;
				case "rare" -> Rarity.RARE;
				case "epic" -> Rarity.EPIC;
				default -> Rarity.COMMON;
			};
			RELIC_ITEMS.add(ITEMS.register(
					relic.id,
					() -> new RelicItem(
							relic,
							new Item.Properties()
									.setId(ResourceKey.create(
											Registries.ITEM,
											Identifier.fromNamespaceAndPath(
													Manifest.MODID,
													relic.id
											)
									))
									.rarity(rarity)
					)
			));
		}

		EXAMPLE_TAB = CREATIVE_MODE_TABS.register("relics", () -> CreativeModeTab.builder()
				.title(Component.translatable("itemGroup.vitalrelics"))
				.withTabsBefore(CreativeModeTabs.COMBAT)
				.icon(() -> RELIC_ITEMS.get(0).get().getDefaultInstance())
				.displayItems((parameters, output) -> {
					output.accept(GUIDE_BOOK.get());

					for (final var relic : RELIC_ITEMS)
						output.accept(relic.get());
				})
				.build());

		// NeoForge.EVENT_BUS.register(this);
		NeoForge.EVENT_BUS.register(VitalEvents.class);
		NeoForge.EVENT_BUS.register(AcquisitionEvents.class);
	}

	private void commonSetup(final FMLCommonSetupEvent event) {
		final Identifier validator =
				Identifier.fromNamespaceAndPath(Manifest.MODID, "relic_slot");

		CuriosSlotTypes.registerPredicate(validator, (slotContext, stack) -> {
			final Identifier itemId =
					BuiltInRegistries.ITEM.getKey(stack.getItem());

			if (!itemId.getNamespace().equals(Manifest.MODID))
				return false;

			final Relic relic = Loader.get().find(itemId.getPath());

			return relic != null && relic.curio_slot.equals(slotContext.identifier());
		});

		event.enqueueWork(() -> {
			for (final var holder : RELIC_ITEMS) {
				final Item item = holder.get();

				CuriosApi.registerCurio(item, new ICurioItem() {
					@Override
					public boolean canEquip(
							final SlotContext context,
							final ItemStack stack) {

						final Identifier id =
								BuiltInRegistries.ITEM.getKey(stack.getItem());

						final Relic relic = Loader.get().find(id.getPath());

						return relic != null &&
								relic.curio_slot.equals(context.identifier());
					}
				});
			}
		});

	}

}
