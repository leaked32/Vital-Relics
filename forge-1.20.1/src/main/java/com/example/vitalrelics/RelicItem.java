package com.example.vitalrelics;

import com.example.vitalrelics.client.RelicClientExtensions;
import com.example.vitalrelics.common.Relic;
import com.example.vitalrelics.common.RelicText;
import com.example.vitalrelics.common.RelicTranslations;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

import static com.example.vitalrelics.common.Relic.itemDisplayName;

public class RelicItem extends Item {
	private final Relic relic;

	public RelicItem(final Relic relic, final Properties properties) {
		super(properties);
		this.relic = relic;
	}


	@Override
	public void initializeClient(
			final java.util.function.Consumer<
					net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {

		consumer.accept(new RelicClientExtensions());
	}

	@Override
	public void appendHoverText(
			final ItemStack stack,
			final Level level,
			final List<Component> tooltip,
			final TooltipFlag flag) {

		for (final RelicText.Text line : RelicText.tooltipLines(relic))
			tooltip.add(component(line));
	}

	@Override
	public Component getName(final ItemStack stack) {
		return component(RelicText.itemName(relic));
	}

	private static Component component(final RelicText.Text text) {
		final Component result = switch (text.source()) {
			case LITERAL -> Component.literal(text.fallback());

			case VANILLA -> Component.translatableWithFallback(
					text.translationKey(),
					text.fallback()
			);

			case EXTERNAL -> externalComponent(text);
		};

		return applyStyle(result, text.style());
	}

	private static Component applyStyle(
			final Component component,
			final RelicText.Style style) {

		final MutableComponent result = component.copy();

		return switch (style) {
			case DESCRIPTION -> result.withStyle(ChatFormatting.GRAY);
			case POSITIVE -> result.withStyle(ChatFormatting.GREEN);
			case NEGATIVE -> result.withStyle(ChatFormatting.RED);
			case PROPERTY -> result.withStyle(ChatFormatting.AQUA);
			case EFFECT -> result.withStyle(ChatFormatting.LIGHT_PURPLE);
			case ABILITY -> result.withStyle(ChatFormatting.GOLD);
			case SPELL -> result.withStyle(ChatFormatting.BLUE);
			case IMMUNITY -> result.withStyle(ChatFormatting.YELLOW);
			case DEFAULT -> result;
		};
	}
	private static Component externalComponent(final RelicText.Text text) {
		final String pattern = RelicTranslations.INSTANCE.translate(
				text.translationKey(), text.fallback()
		);

		final MutableComponent result = Component.literal("");
		int start = 0;
		int argumentIndex = 0;

		while (true) {
			final int marker = pattern.indexOf("%s", start);

			if (marker < 0) {
				result.append(Component.literal(pattern.substring(start)));
				return result;
			}

			result.append(Component.literal(pattern.substring(start, marker)));

			if (argumentIndex < text.arguments().size()) {
				result.append(component(text.arguments().get(argumentIndex)));
			} else {
				result.append(Component.literal("%s"));
			}

			argumentIndex++;
			start = marker + 2;
		}
	}
}
