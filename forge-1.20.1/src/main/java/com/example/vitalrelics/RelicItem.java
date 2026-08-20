package com.example.vitalrelics;

import com.example.vitalrelics.client.RelicClientExtensions;
import com.example.vitalrelics.common.Relic;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

public class RelicItem extends Item {
	private final Relic relic;

	public RelicItem(final Relic relic, final Properties properties) {
		super(properties);
		this.relic = relic;
	}

	@Override
	public void appendHoverText(
			final ItemStack stack,
			@Nullable final Level level,
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

		if (info.flat != null && info.flat != 0.0) {
			tooltip.add(Component.literal(
					signed(info.flat) + " " + name
			));
		}

		if (info.minimum != null) {
			tooltip.add(Component.literal(
					"Minimum " + name + ": " + fmt(info.minimum)
			));
		}

		if (info.ratio_minimum != null) {
			tooltip.add(Component.literal(
					"Minimum " + name + ": " +
							fmt(info.ratio_minimum * 100.0) + "% of Max Health"
			));
		}

		if (info.maximum != null) {
			tooltip.add(Component.literal(
					"Maximum " + name + ": " + fmt(info.maximum)
			));
		}

		if (info.ratio_maximum != null) {
			tooltip.add(Component.literal(
					"Maximum " + name + ": " +
							fmt(info.ratio_maximum * 100.0) + "% of Max Health"
			));
		}
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

	@Override
	public void initializeClient(
			final java.util.function.Consumer<
					net.minecraftforge.client.extensions.common.IClientItemExtensions
					> consumer) {

		consumer.accept(new RelicClientExtensions());
	}
}