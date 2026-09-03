package com.example.vitalrelics.client.guide;

import com.example.vitalrelics.Utils;
import com.example.vitalrelics.common.guide.GuideBook;
import com.example.vitalrelics.common.guide.GuidePage;
import com.example.vitalrelics.common.guide.MarkdownPage;
import com.example.vitalrelics.common.relics.Translations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GuideBookScreen extends Screen {
	private static final int MARGIN = 16;
	private static final int LIST_WIDTH = 130;
	private static final int GAP = 8;
	private static final int HEADER_HEIGHT = 18;

	private static final int SEARCH_HEIGHT = 16;
	private static final int SEARCH_GAP = 4;

	private static final int ROW_HEIGHT = 16;
	private static final int LINE_HEIGHT = 10;
	private static final int SCROLL_STEP = 24;

	private static final int SCROLLBAR_WIDTH = 3;
	private static final int MIN_SCROLLBAR_HEIGHT = 12;

	private final List<Page> pages = new ArrayList<>();
	private final List<Page> visiblePages = new ArrayList<>();

	private EditBox searchBox;

	private int selectedIndex;
	private int listScroll;
	private int contentScroll;

	private int listContentHeight;
	private int pageContentHeight;

	public GuideBookScreen(final GuideBook guideBook) {
		super(Component.literal(tr("guide.vitalrelics.title", "Vital Relics")));

		if (guideBook == null)
			throw new IllegalArgumentException("guideBook cannot be null");

		addPage(GuidePage.introduction());
		addConfigurationPages();

		for (final GuideBook.Entry entry : guideBook.entries()) {
			addPage(GuidePage.from(
					entry,
					GuideBookScreen::translatedIngredientName,
					Utils::effectName
			));
		}

		visiblePages.addAll(pages);
	}

	private void addPage(final GuidePage page) {
		pages.add(new Page(page.id, page.title, page, null));
	}

	private void addPage(final MarkdownPage page) {
		pages.add(new Page(page.id, page.title, null, page));
	}

	private void addConfigurationPages() {
		final String locale = Minecraft.getInstance()
				.getLanguageManager()
				.getSelected();

		addMarkdownPage("configure-curios", locale);
		addMarkdownPage("configure-recipes", locale);
		addMarkdownPage("configure-translations", locale);
	}

	private void addMarkdownPage(final String id, final String locale) {
		String markdown = readMarkdown(locale, id);

		if (markdown == null && !"en_us".equals(locale))
			markdown = readMarkdown("en_us", id);

		if (markdown != null)
			addPage(MarkdownPage.parse(id, markdown));
	}

	private static String readMarkdown(final String locale, final String id) {
		final String path =
				"/vitalrelics/guide/" + locale + "/" + id + ".md";

		try (InputStream stream =
					 GuideBookScreen.class.getResourceAsStream(path)) {
			if (stream == null)
				return null;

			return new String(
					stream.readAllBytes(),
					StandardCharsets.UTF_8
			);
		} catch (IOException exception) {
			throw new RuntimeException(
					"Failed to load guide page " + path,
					exception
			);
		}
	}

	private static String translatedIngredientName(final String id) {
		final Identifier location = Identifier.tryParse(id);

		if (location == null || !BuiltInRegistries.ITEM.containsKey(location))
			return null;

		return BuiltInRegistries.ITEM
				.get(location)
				.map(holder -> Component.translatable(holder.value().getDescriptionId()).getString())
				.orElse(null);
	}

	@Override
	protected void init() {
		searchBox = new EditBox(
				font,
				listLeft(), MARGIN + HEADER_HEIGHT,
				LIST_WIDTH, SEARCH_HEIGHT,
				Component.literal(tr("guide.vitalrelics.search", "Search"))
		);

		searchBox.setHint(
				Component.literal(tr("guide.vitalrelics.search", "Search..."))
		);

		searchBox.setResponder(this::updateSearch);

		addRenderableWidget(searchBox);
	}

	private int listLeft() {
		return MARGIN;
	}

	private int listRight() {
		return MARGIN + LIST_WIDTH;
	}

	private int contentLeft() {
		return listRight() + GAP;
	}

	private int listTop() {
		return MARGIN + HEADER_HEIGHT + SEARCH_HEIGHT + SEARCH_GAP;
	}

	private int contentTop() {
		return MARGIN + HEADER_HEIGHT;
	}

	private int paneBottom() {
		return height - MARGIN;
	}

	private int listHeight() {
		return Math.max(0, paneBottom() - listTop());
	}

	private int contentHeight() {
		return Math.max(0, paneBottom() - contentTop());
	}

	private int listTextRight() {
		return listRight() - SCROLLBAR_WIDTH - 3;
	}

	private int contentTextRight() {
		return width - MARGIN - SCROLLBAR_WIDTH - 3;
	}

	private Page selectedPage() {
		if (visiblePages.isEmpty())
			return null;

		selectedIndex = Math.max(
				0,
				Math.min(selectedIndex, visiblePages.size() - 1)
		);

		return visiblePages.get(selectedIndex);
	}

	private void updateSearch(final String query) {
		final Page previous = selectedPage();

		final String normalized = query == null
				? ""
				: query.strip().toLowerCase(Locale.ROOT);

		visiblePages.clear();

		for (final Page page : pages) {
			if (matchesSearch(page, normalized))
				visiblePages.add(page);
		}

		listScroll = 0;
		contentScroll = 0;

		if (visiblePages.isEmpty()) {
			selectedIndex = 0;
			return;
		}

		final int previousIndex = visiblePages.indexOf(previous);
		selectedIndex = previousIndex >= 0 ? previousIndex : 0;
	}

	private static boolean matchesSearch(
			final Page page,
			final String query) {

		if (query.isEmpty())
			return true;

		return page.title.toLowerCase(Locale.ROOT).contains(query)
				|| page.id.toLowerCase(Locale.ROOT).contains(query);
	}

	@Override
	public void extractRenderState(
			final GuiGraphicsExtractor graphics, final int mouseX,
			final int mouseY, final float partialTick) {

		// renderBackground(graphics, mouseX, mouseY, partialTick);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);

		graphics.drawString(font, title, MARGIN, MARGIN, 0xFFFFFFFF);

		renderPageList(graphics);

		final Page page = selectedPage();

		if (page == null) {
			graphics.drawString(
					font,
					Component.literal(tr(
							"guide.vitalrelics.empty",
							"No relics loaded."
					)),
					contentLeft(), contentTop(), 0xFFAAAAAA
			);
		} else if (page.markdown != null) {
			renderMarkdownPage(graphics, page.markdown);
		} else {
			renderGuidePage(graphics, page.guide);
		}

		renderScrollbars(graphics);
	}

	private void renderPageList(final GuiGraphicsExtractor graphics) {
		final int left = listLeft();
		final int right = listTextRight();
		final int top = listTop();
		final int bottom = paneBottom();

		listContentHeight = visiblePages.size() * ROW_HEIGHT;
		clampListScroll();

		graphics.enableScissor(left, top, right, bottom);

		int y = top - listScroll;

		for (int i = 0; i < visiblePages.size(); ++i) {
			final Page page = visiblePages.get(i);
			final boolean selected = i == selectedIndex;

			if (selected)
				graphics.fill(left, y, right, y + ROW_HEIGHT - 1, 0x60404040);

			if (y + ROW_HEIGHT >= top && y <= bottom) {
				graphics.drawString(
						font,
						Component.literal(page.title),
						left + 3, y + 4,
						selected ? 0xFFFFFFFF : 0xFFCCCCCC
				);
			}

			y += ROW_HEIGHT;
		}

		graphics.disableScissor();
	}

	private void renderGuidePage(
			final GuiGraphicsExtractor graphics,
			final GuidePage page) {

		final int left = contentLeft();
		final int right = contentTextRight();
		final int top = contentTop();
		final int bottom = paneBottom();
		final int textWidth = Math.max(80, right - left);

		graphics.enableScissor(left, top, right, bottom);

		int y = top - contentScroll;

		y = drawLine(graphics, page.title, left, y, 0xFFFFFFFF);

		if (!page.rarity.isBlank() || !page.slot.isBlank()) {
			y = drawLine(
					graphics,
					trf(
							"guide.vitalrelics.rarity_slot", "%s · %s",
							tr(
									"guide.vitalrelics.rarity." + page.rarity,
									humanize(page.rarity)
							),
							tr(
									"guide.vitalrelics.slot." + page.slot,
									humanize(page.slot)
							)
					),
					left, y, 0xFFAAAAAA
			);
		}

		y += 3;

		y = drawWrapped(
				graphics,
				page.description,
				left, y, textWidth,
				0xFFDDDDDD
		);

		y += 5;

		for (final GuidePage.Section section : page.sections) {
			y = drawLine(graphics, section.title, left, y, 0xFFFFFFFF);

			for (final String line : section.lines) {
				y = drawWrapped(
						graphics,
						line,
						left + 5, y,
						Math.max(40, textWidth - 5),
						0xFFCCCCCC
				);
			}

			y += 4;
		}

		graphics.disableScissor();

		pageContentHeight = y - (top - contentScroll);
		clampContentScroll();
	}

	private void renderMarkdownPage(
			final GuiGraphicsExtractor graphics,
			final MarkdownPage page) {

		final int left = contentLeft();
		final int right = contentTextRight();
		final int top = contentTop();
		final int bottom = paneBottom();
		final int textWidth = Math.max(80, right - left);

		graphics.enableScissor(left, top, right, bottom);

		int y = top - contentScroll;

		y = drawLine(graphics, page.title, left, y, 0xFFFFFFFF);
		y += 4;

		for (final MarkdownPage.Block block : page.blocks) {
			if (block instanceof MarkdownPage.Heading heading) {
				y += heading.level() <= 2 ? 5 : 2;
				y = drawLine(
						graphics,
						heading.text(),
						left + Math.min(8, Math.max(0, heading.level() - 2) * 3),
						y,
						heading.level() <= 2 ? 0xFFFFFFFF : 0xFFE0E0E0
				);
				y += 2;
				continue;
			}

			if (block instanceof MarkdownPage.Paragraph paragraph) {
				y = drawWrapped(
						graphics,
						paragraph.text(),
						left, y,
						textWidth,
						0xFFDDDDDD
				);
				y += 4;
				continue;
			}

			if (block instanceof MarkdownPage.ListItem item) {
				final int indent = Math.min(24, item.indent() * 2);
				final String marker = item.ordered()
						? item.marker() + ". "
						: "• ";

				y = drawWrapped(
						graphics,
						marker + item.text(),
						left + 5 + indent, y,
						Math.max(40, textWidth - 5 - indent),
						0xFFCCCCCC
				);
				continue;
			}

			if (block instanceof MarkdownPage.CodeBlock code) {
				if (!code.language().isBlank()) {
					y = drawLine(
							graphics,
							code.language(),
							left + 4, y,
							0xFF999999
					);
				}

				for (final String line : code.lines()) {
					final int lineHeight = Math.max(
							LINE_HEIGHT,
							font.split(
									Component.literal(line.isEmpty() ? " " : line),
									Math.max(40, textWidth - 10)
							).size() * LINE_HEIGHT
					);

					graphics.fill(
							left,
							y - 1,
							right,
							y + lineHeight,
							0x50202020
					);

					y = drawWrapped(
							graphics,
							line.isEmpty() ? " " : line,
							left + 5, y,
							Math.max(40, textWidth - 10),
							0xFFD0D0D0
					);
				}

				y += 5;
				continue;
			}

			if (block instanceof MarkdownPage.Table table) {
				if (MarkdownPage.isTableSeparator(table))
					continue;

				y = drawWrapped(
						graphics,
						String.join("  |  ", table.cells()),
						left + 4, y,
						Math.max(40, textWidth - 8),
						0xFFCCCCCC
				);
			}
		}

		graphics.disableScissor();

		pageContentHeight = y - (top - contentScroll);
		clampContentScroll();
	}

	private int drawLine(
			final GuiGraphicsExtractor graphics, final String text,
			final int x, final int y, final int color) {

		if (text == null || text.isBlank())
			return y;

		graphics.drawString(font, Component.literal(text), x, y, color);
		return y + LINE_HEIGHT;
	}

	private int drawWrapped(
			final GuiGraphicsExtractor graphics, final String text,
			final int x, final int y,
			final int width, final int color) {

		if (text == null || text.isBlank())
			return y;

		int currentY = y;

		for (final var line : font.split(Component.literal(text), width)) {
			graphics.drawString(font, line, x, currentY, color);
			currentY += LINE_HEIGHT;
		}

		return currentY;
	}

	private void renderScrollbars(final GuiGraphicsExtractor graphics) {
		renderScrollbar(
				graphics,
				listRight() - SCROLLBAR_WIDTH,
				listTop(), listHeight(),
				listScroll, listContentHeight
		);

		renderScrollbar(
				graphics,
				width - MARGIN - SCROLLBAR_WIDTH,
				contentTop(), contentHeight(),
				contentScroll, pageContentHeight
		);
	}

	private void renderScrollbar(
			final GuiGraphicsExtractor graphics, final int x,
			final int top, final int viewportHeight,
			final int scroll, final int contentHeight) {

		if (contentHeight <= viewportHeight || viewportHeight <= 0)
			return;

		final int maxScroll = contentHeight - viewportHeight;

		final int thumbHeight = Math.max(
				MIN_SCROLLBAR_HEIGHT,
				(int) ((double) viewportHeight * viewportHeight / contentHeight)
		);

		final int travel = viewportHeight - thumbHeight;
		final int thumbOffset = (int) ((double) scroll / maxScroll * travel);

		graphics.fill(
				x, top,
				x + SCROLLBAR_WIDTH,
				top + viewportHeight,
				0x40202020
		);

		graphics.fill(
				x, top + thumbOffset,
				x + SCROLLBAR_WIDTH,
				top + thumbOffset + thumbHeight,
				0xFFAAAAAA
		);
	}

	@Override
	public boolean mouseClicked(
			final MouseButtonEvent event, final boolean doubleClick) {
		final double mouseX = event.x();
		final double mouseY = event.y();
		final int button = event.button();

		if (button == 0 && inside(
				mouseX, mouseY,
				listLeft(), listTop(),
				listTextRight(), paneBottom()
		)) {
			final int index =
					((int) mouseY - listTop() + listScroll) / ROW_HEIGHT;

			if (index >= 0 && index < visiblePages.size()) {
				selectedIndex = index;
				contentScroll = 0;
				return true;
			}
		}

		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseScrolled(
			final double mouseX, final double mouseY,
			final double scrollX, final double scrollY) {

		if (inside(
				mouseX, mouseY,
				listLeft(), listTop(),
				listRight(), paneBottom()
		)) {
			listScroll -= (int) Math.round(scrollY * SCROLL_STEP);
			clampListScroll();
			return true;
		}

		if (inside(
				mouseX, mouseY,
				contentLeft(), contentTop(),
				width - MARGIN, paneBottom()
		)) {
			contentScroll -= (int) Math.round(scrollY * SCROLL_STEP);
			clampContentScroll();
			return true;
		}

		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void clampListScroll() {
		listScroll = clampScroll(listScroll, listContentHeight, listHeight());
	}

	private void clampContentScroll() {
		contentScroll = clampScroll(
				contentScroll,
				pageContentHeight,
				contentHeight()
		);
	}

	private static int clampScroll(
			final int scroll, final int contentHeight,
			final int viewportHeight) {

		final int max = Math.max(0, contentHeight - viewportHeight);
		return Math.max(0, Math.min(scroll, max));
	}

	private static boolean inside(
			final double x, final double y,
			final int left, final int top,
			final int right, final int bottom) {

		return x >= left && x < right && y >= top && y < bottom;
	}

	private static String tr(final String key, final String fallback) {
		return Translations.get().translate(key, fallback);
	}

	private static String trf(
			final String key, final String fallback,
			final Object... arguments) {

		return String.format(Locale.ROOT, tr(key, fallback), arguments);
	}

	private static String humanize(final String value) {
		if (value == null || value.isBlank())
			return "";

		final String normalized = value.replace('_', ' ').replace('-', ' ');
		final String[] words = normalized.split("\\s+");
		final StringBuilder result = new StringBuilder();

		for (final String word : words) {
			if (word.isEmpty())
				continue;

			if (!result.isEmpty())
				result.append(' ');

			result.append(Character.toUpperCase(word.charAt(0)));

			if (word.length() > 1)
				result.append(word.substring(1));
		}

		return result.toString();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static class Page {
		private final String id;
		private final String title;
		private final GuidePage guide;
		private final MarkdownPage markdown;

		private Page(
				final String id,
				final String title,
				final GuidePage guide,
				final MarkdownPage markdown) {

			this.id = id;
			this.title = title;
			this.guide = guide;
			this.markdown = markdown;
		}
	}
}
