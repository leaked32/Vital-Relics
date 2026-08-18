package com.example.vitalrelics;

import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicLoader;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

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

    /*
    // Creates a new Block with the id "vitalrelics:example_block", combining the namespace and path
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "vitalrelics:example_block", combining the namespace and path
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new food item with the id "vitalrelics:example_id", nutrition 1 and saturation 2
    public static final DeferredItem<Item> EXAMPLE_ITEM = ITEMS.registerSimpleItem("example_item", new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEdible().nutrition(1).saturationModifier(2f).build()));

    // Creates a creative tab with the id "vitalrelics:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.vitalrelics"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get()); // Add the example item to the tab. For your own tabs, this method is preferred over the event
            }).build());
     */

	public static RelicLoader loader = null;
	public static final List<DeferredItem<Item>> RELIC_ITEMS = new ArrayList<>();
	// The constructor for the mod class is the first code that is run when your mod is loaded.
	// FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
	public final static class RelicItem extends Item {
		private final Relic relic;

		public RelicItem(final Relic relic, final Properties properties) {
			super(properties);
			this.relic = relic;
		}

		@Override
		public void appendHoverText(
				final ItemStack stack,
				final TooltipContext context,
				final List<Component> tooltip,
				final TooltipFlag flag) {

			tooltip.add(Component.literal(relic.tooltip));
			tooltip.add(Component.literal(""));

			addPropertyTooltip(tooltip, "Attack Damage", relic.properties.attack_damage);
			addPropertyTooltip(tooltip, "Max Health", relic.properties.max_health);

			addTickTooltip(tooltip, "Regeneration", relic.ticks.heal);
			addTickTooltip(tooltip, "Hunger", relic.ticks.feed);

			addCallbackTooltip(tooltip, "Damage Taken", relic.callbacks.damage_taken);
			addCallbackTooltip(tooltip, "Damage Dealt", relic.callbacks.damage_dealt);
		}

		private static void addPropertyTooltip(
				final List<Component> tooltip,
				final String name,
				final Relic.Properties.Info info) {

			if (info == null)
				return;

			if (info.add != null && info.add != 0.0) {
				tooltip.add(Component.literal(
						signed(info.add) + " " + name
				));
			}

			if (info.mul_base != null && info.mul_base != 0.0) {
				tooltip.add(Component.literal(
						signedPercent(info.mul_base) + " " + name + " from base"
				));
			}

			if (info.mul_total != null && info.mul_total != 1.0) {
				tooltip.add(Component.literal(
						"x" + fmt(info.mul_total) + " total " + name
				));
			}
		}

		private static void addTickTooltip(
				final List<Component> tooltip,
				final String name,
				final Relic.Ticks.Info info) {

			if (info == null)
				return;

			final double seconds = info.interval_ticks / 20.0;

			if (info.add != null && info.add != 0.0) {
				tooltip.add(Component.literal(
						"Every " + fmt(seconds) + "s: +" +
								fmt(info.add) + " " + name
				));
			}

			if (info.ratio_add != null && info.ratio_add != 0.0) {
				tooltip.add(Component.literal(
						"Every " + fmt(seconds) + "s: +" +
								fmt(info.ratio_add * 100.0) + "% Max " + name
				));
			}
		}

		private static void addCallbackTooltip(
				final List<Component> tooltip,
				final String name,
				final Relic.Callbacks.Info info) {

			if (info == null)
				return;

			if (info.modifier != null) {
				final double change = (info.modifier - 1.0) * 100.0;

				tooltip.add(Component.literal(
						signedPercentRaw(change) + " " + name
				));
			}

			if (info.minimum != null)
				tooltip.add(Component.literal(
						"Minimum " + name + ": " + fmt(info.minimum)
				));

			if (info.ratio_minimum != null)
				tooltip.add(Component.literal(
						"Minimum " + name + ": " +
								fmt(info.ratio_minimum * 100.0) + "% of Max Health"
				));

			if (info.maximum != null)
				tooltip.add(Component.literal(
						"Maximum " + name + ": " + fmt(info.maximum)
				));

			if (info.ratio_maximum != null)
				tooltip.add(Component.literal(
						"Maximum " + name + ": " +
								fmt(info.ratio_maximum * 100.0) + "% of Max Health"
				));
		}

		private static String signed(final double value) {
			return (value >= 0.0 ? "+" : "") + fmt(value);
		}

		private static String signedPercent(final double value) {
			return signedPercentRaw(value * 100.0);
		}

		private static String signedPercentRaw(final double value) {
			return (value >= 0.0 ? "+" : "") + fmt(value) + "%";
		}

		private static String fmt(final double value) {
			if (Math.abs(value - Math.round(value)) < 0.000001)
				return Long.toString(Math.round(value));

			return String.format("%.2f", value)
					.replaceAll("0+$", "")
					.replaceAll("\\.$", "");
		}
	}

	public VitalRelics(IEventBus modEventBus, ModContainer modContainer)
	{
		// Register the commonSetup method for modloading
		modEventBus.addListener(this::commonSetup);

		BLOCKS.register(modEventBus);
		ITEMS.register(modEventBus);
		CREATIVE_MODE_TABS.register(modEventBus);

		loader = new RelicLoader();
		loader.load(null);
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
			// RELIC_ITEMS.add(ITEMS.registerSimpleItem(loader.relics_.get(i).id, new Item.Properties().rarity(rarity)));
		}

		NeoForge.EVENT_BUS.register(this);
		// modEventBus.addListener(this::addCreative);
		NeoForge.EVENT_BUS.register(VitalEvents.class);
		modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
	}

	private void commonSetup(final FMLCommonSetupEvent event)
	{
		// Some common setup code
		LOGGER.info("HELLO FROM COMMON SETUP");

		if (Config.logDirtBlock)
			LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));

		LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);

		Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event)
	{
		// Do something when the server starts
		LOGGER.info("HELLO from server starting");
	}



}
