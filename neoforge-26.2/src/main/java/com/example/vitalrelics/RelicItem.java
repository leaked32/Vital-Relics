package com.example.vitalrelics;

import com.example.vitalrelics.common.Manifest;
import com.example.vitalrelics.common.relics.Relic;
import com.example.vitalrelics.common.relics.Translations;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import com.example.vitalrelics.common.RelicText;

import java.util.List;

public class RelicItem extends Item {
	private static final Identifier MODEL = Identifier.fromNamespaceAndPath(
			Manifest.MODID, "relic"
	);

	public final Relic relic;
	private DataComponentMap components;

	public RelicItem(final Relic relic, final Properties properties) {
		super(properties);
		this.relic = relic;
	}

	@Override
	public DataComponentMap components() {
		if (components == null) {
			components = DataComponentMap.builder()
					.addAll(super.components())
					.set(DataComponents.ITEM_MODEL, MODEL)
					.build();
		}

		return components;
	}

	public Relic relic() {
		return relic;
	}

	@Override
	public Component getName(final ItemStack stack) {
		return component(RelicText.itemName(relic));
	}


	public static Component component(final RelicText.Text text) {
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
		final String pattern = Translations.get().translate(
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
