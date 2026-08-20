package com.example.vitalrelics;

import com.example.vitalrelics.acquisition.AcquisitionEvents;
import com.example.vitalrelics.acquisition.DynamicRelicRecipe;
import com.example.vitalrelics.common.AcquisitionLoader;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

@Mod(VitalRelics.MODID)
public class VitalRelics {
	public static final String MODID = "vitalrelics";
	public static final Logger LOGGER = LogUtils.getLogger();

	public static final DeferredRegister<Item> ITEMS =
			DeferredRegister.create(ForgeRegistries.ITEMS, MODID);

	public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
			DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

	public static RegistryObject<CreativeModeTab> RELICS_TAB;

	public static RelicLoader loader = null;

	public static final List<RegistryObject<Item>> RELIC_ITEMS =
			new ArrayList<>();
	public static AcquisitionLoader acquisition;

	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
			DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MODID);

	public static final RegistryObject<RecipeSerializer<DynamicRelicRecipe>>
			DYNAMIC_RELIC_RECIPE =
			RECIPE_SERIALIZERS.register(
					"dynamic_relic",
					() -> new SimpleCraftingRecipeSerializer<>(DynamicRelicRecipe::new)
			);

	public VitalRelics() {
		final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

		/*
		 * Load the version-independent relic definitions before registration.
		 */
		modEventBus.addListener(this::commonSetup);

		ITEMS.register(modEventBus);
		CREATIVE_MODE_TABS.register(modEventBus);
		RECIPE_SERIALIZERS.register(modEventBus);

		final Path config = FMLPaths.CONFIGDIR.get().resolve("vitalrelics/relics.json");
		try {
			if (Files.notExists(config)) {
				Files.createDirectories(config.getParent());

				try (final InputStream in = VitalRelics.class.getResourceAsStream("/vitalrelics/relics.json")) {
					if (in == null)
						throw new IllegalStateException("Bundled relics.json not found");

					Files.copy(in, config);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to create the configuration file: {}", e.toString());
			throw new RuntimeException(e);
		}
		final Path recipeConfig =
				FMLPaths.CONFIGDIR.get().resolve("vitalrelics/recipes.json");

		try{
			if (Files.notExists(recipeConfig)) {
				Files.createDirectories(recipeConfig.getParent());

				try (final InputStream in =
							 VitalRelics.class.getClassLoader()
									 .getResourceAsStream("vitalrelics/recipes.json")) {

					if (in == null)
						throw new IllegalStateException("Bundled recipes.json not found");

					Files.copy(in, recipeConfig);
				}
			}
		} catch (IOException e) {
			LOGGER.error("Failed to create the recipe file: {}", e.toString());
			throw new RuntimeException(e);
		}
		loader = new RelicLoader();
		acquisition = new AcquisitionLoader();
		loader.load(config);
		acquisition.load(recipeConfig);

		for (final var relic : loader.relics_) {
			final Rarity rarity = switch (relic.rarity.toLowerCase()) {
				case "uncommon" -> Rarity.UNCOMMON;
				case "rare" -> Rarity.RARE;
				case "epic" -> Rarity.EPIC;
				default -> Rarity.COMMON;
			};

			RELIC_ITEMS.add(
					ITEMS.register(
							relic.id,
							() -> new RelicItem(
									relic,
									new Item.Properties().rarity(rarity)
							)
					)
			);
		}

		RELICS_TAB = CREATIVE_MODE_TABS.register(
				"relics",
				() -> CreativeModeTab.builder()
						.title(Component.translatable("itemGroup.vitalrelics"))
						.icon(() ->
								RELIC_ITEMS.get(0)
										.get()
										.getDefaultInstance()
						)
						.displayItems((parameters, output) -> {
							for (final RegistryObject<Item> relic : RELIC_ITEMS)
								output.accept(relic.get());
						})
						.build()
		);

		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(VitalEvents.class);
		MinecraftForge.EVENT_BUS.register(AcquisitionEvents.class);
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

