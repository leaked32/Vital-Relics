package com.example.vitalrelics.client.guide;

import com.example.vitalrelics.Utils;
import com.example.vitalrelics.common.guide.GuideBook;
import com.example.vitalrelics.common.guide.GuideBookView;
import com.example.vitalrelics.common.guide.GuideCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

/** Adapts the game's screen and drawing API to the shared guide view. */
public class GuideBookScreen extends Screen {
	private final GuideBookView view;
	private EditBox searchBox;

	public GuideBookScreen(final GuideBook book) {
		super(Component.literal(GuideBookView.translate("guide.vitalrelics.title", "Vital Relics")));
		view = new GuideBookView(book, Minecraft.getInstance().getLanguageManager().getSelected(),
				GuideBookScreen::translatedIngredientName, Utils::effectName);
	}

	private static String translatedIngredientName(final String id) {
		final ResourceLocation location = ResourceLocation.tryParse(id);

		if (location == null || !BuiltInRegistries.ITEM.containsKey(location))
			return null;

		return BuiltInRegistries.ITEM
				.get(location)
				.getDescription()
				.getString();
	}

	@Override
	protected void init() {
		view.resize(width, height);
		final String previousQuery = searchBox == null ? "" : searchBox.getValue();
		searchBox = new EditBox(font, view.searchX(), view.searchY(),
				view.searchWidth(), view.searchHeight(),
				Component.literal(GuideBookView.translate("guide.vitalrelics.search", "Search")));
		searchBox.setHint(Component.literal(GuideBookView.translate("guide.vitalrelics.search", "Search...")));
		searchBox.setResponder(view::updateSearch);
		searchBox.setValue(previousQuery);
		addRenderableWidget(searchBox);
	}

	@Override
	public void render(final GuiGraphics graphics, final int mouseX,
			final int mouseY, final float partialTick) {
		renderBackground(graphics, mouseX, mouseY, partialTick);
		super.render(graphics, mouseX, mouseY, partialTick);
		view.render(canvas(graphics));
	}

	private GuideCanvas canvas(final GuiGraphics graphics) {
		return new GuideCanvas() {
			@Override public void text(String text, int x, int y, int argb) {
				graphics.drawString(font, Component.literal(text), x, y, argb);
			}
			@Override public List<String> wrap(String text, int width) {
				return font.split(Component.literal(text), width).stream()
						.map(line -> {
							final StringBuilder value = new StringBuilder();
							line.accept((index, style, codePoint) -> {
								value.appendCodePoint(codePoint);
								return true;
							});
							return value.toString();
						}).toList();
			}
			@Override public void fill(int left, int top, int right, int bottom, int argb) {
				graphics.fill(left, top, right, bottom, argb);
			}
			@Override public void pushClip(int left, int top, int right, int bottom) {
				graphics.enableScissor(left, top, right, bottom);
			}
			@Override public void popClip() { graphics.disableScissor(); }
		};
	}

	@Override
	public boolean mouseClicked(final double x, final double y, final int button) {
		return view.mouseClicked(x, y, button) || super.mouseClicked(x, y, button);
	}
	@Override
	public boolean mouseScrolled(final double x, final double y,
			final double scrollX, final double scrollY) {
		return view.mouseScrolled(x, y, scrollY)
				|| super.mouseScrolled(x, y, scrollX, scrollY);
	}

	@Override public boolean isPauseScreen() { return false; }
}
