package com.example.vitalrelics.common.guide;

import com.example.vitalrelics.common.relics.Translations;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/** Platform-independent guide layout, rendering, search and input state. */
public final class GuideBookView {
	private static final int MARGIN = 16, LIST_WIDTH = 130, GAP = 8, HEADER_HEIGHT = 18;
	private static final int SEARCH_HEIGHT = 16, SEARCH_GAP = 4, ROW_HEIGHT = 16;
	private static final int LINE_HEIGHT = 10, SCROLL_STEP = 24;
	private static final int SCROLLBAR_WIDTH = 3, MIN_SCROLLBAR_HEIGHT = 12;
	private final List<Page> pages = new ArrayList<>();
	private final List<Page> visiblePages = new ArrayList<>();
	private int width, height, selectedIndex, listScroll, contentScroll;
	private int listContentHeight, pageContentHeight;

	public GuideBookView(final GuideBook book, final String locale,
			final Function<String, String> ingredientName,
			final Function<String, String> effectName) {
		if (book == null) throw new IllegalArgumentException("book cannot be null");
		addPage(GuidePage.introduction());
		for (final String id : List.of("configure-curios", "configure-recipes", "configure-translations")) {
			String markdown = readMarkdown(locale, id);
			if (markdown == null && !"en_us".equals(locale)) markdown = readMarkdown("en_us", id);
			if (markdown != null) addPage(MarkdownPage.parse(id, markdown));
		}
		for (final GuideBook.Entry entry : book.entries())
			addPage(GuidePage.from(entry, ingredientName, effectName));
		visiblePages.addAll(pages);
	}

	private void addPage(final GuidePage page) {
		pages.add(new Page(page.id, page.title, page, null));
	}
	private void addPage(final MarkdownPage page) {
		pages.add(new Page(page.id, page.title, null, page));
	}
	private static String readMarkdown(final String locale, final String id) {
		final String path = "/vitalrelics/guide/" + locale + "/" + id + ".md";
		try (InputStream stream = GuideBookView.class.getResourceAsStream(path)) {
			return stream == null ? null : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException exception) {
			throw new RuntimeException("Failed to load guide page " + path, exception);
		}
	}
	public void resize(final int width, final int height) {
		this.width = width;
		this.height = height;
		clampListScroll();
		clampContentScroll();
	}
	public int searchX() { return MARGIN; }
	public int searchY() { return MARGIN + HEADER_HEIGHT; }
	public int searchWidth() { return LIST_WIDTH; }
	public int searchHeight() { return SEARCH_HEIGHT; }
	public static String translate(final String key, final String fallback) {
		return Translations.get().translate(key, fallback);
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

	public void updateSearch(final String query) {
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
	public void render(final GuideCanvas graphics) {
		graphics.text(tr("guide.vitalrelics.title", "Vital Relics"), MARGIN, MARGIN, 0xFFFFFFFF);
		renderPageList(graphics);
		final Page page = selectedPage();
		if (page == null) {
			graphics.text(tr("guide.vitalrelics.empty", "No relics loaded."),
					contentLeft(), contentTop(), 0xFFAAAAAA);
		} else if (page.markdown != null) {
			renderMarkdownPage(graphics, page.markdown);
		} else {
			renderGuidePage(graphics, page.guide);
		}
		renderScrollbars(graphics);
	}

	private void renderPageList(final GuideCanvas graphics) {
		final int left = listLeft();
		final int right = listTextRight();
		final int top = listTop();
		final int bottom = paneBottom();

		listContentHeight = visiblePages.size() * ROW_HEIGHT;
		clampListScroll();

		graphics.pushClip(left, top, right, bottom);

		int y = top - listScroll;

		for (int i = 0; i < visiblePages.size(); ++i) {
			final Page page = visiblePages.get(i);
			final boolean selected = i == selectedIndex;

			if (selected)
				graphics.fill(left, y, right, y + ROW_HEIGHT - 1, 0x60404040);

			if (y + ROW_HEIGHT >= top && y <= bottom) {
				graphics.text(
						page.title,
						left + 3, y + 4,
						selected ? 0xFFFFFFFF : 0xFFCCCCCC
				);
			}

			y += ROW_HEIGHT;
		}

		graphics.popClip();
	}

	private void renderGuidePage(
			final GuideCanvas graphics,
			final GuidePage page) {

		final int left = contentLeft();
		final int right = contentTextRight();
		final int top = contentTop();
		final int bottom = paneBottom();
		final int textWidth = Math.max(80, right - left);

		graphics.pushClip(left, top, right, bottom);

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

		graphics.popClip();

		pageContentHeight = y - (top - contentScroll);
		clampContentScroll();
	}

	private void renderMarkdownPage(
			final GuideCanvas graphics,
			final MarkdownPage page) {

		final int left = contentLeft();
		final int right = contentTextRight();
		final int top = contentTop();
		final int bottom = paneBottom();
		final int textWidth = Math.max(80, right - left);

		graphics.pushClip(left, top, right, bottom);

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
							graphics.wrap(
									line.isEmpty() ? " " : line,
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

		graphics.popClip();

		pageContentHeight = y - (top - contentScroll);
		clampContentScroll();
	}

	private int drawLine(
			final GuideCanvas graphics, final String text,
			final int x, final int y, final int color) {

		if (text == null || text.isBlank())
			return y;

		graphics.text(text, x, y, color);
		return y + LINE_HEIGHT;
	}

	private int drawWrapped(
			final GuideCanvas graphics, final String text,
			final int x, final int y,
			final int width, final int color) {

		if (text == null || text.isBlank())
			return y;

		int currentY = y;

		for (final var line : graphics.wrap(text, width)) {
			graphics.text(line, x, currentY, color);
			currentY += LINE_HEIGHT;
		}

		return currentY;
	}

	private void renderScrollbars(final GuideCanvas graphics) {
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
			final GuideCanvas graphics, final int x,
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

	public boolean mouseClicked(
			final double mouseX, final double mouseY, final int button) {

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

		return false;
	}

	public boolean mouseScrolled(
			final double mouseX, final double mouseY, final double scrollY) {

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

		return false;
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

	private static final class Page {
		final String id, title;
		final GuidePage guide;
		final MarkdownPage markdown;
		Page(String id, String title, GuidePage guide, MarkdownPage markdown) {
			this.id = id; this.title = title; this.guide = guide; this.markdown = markdown;
		}
	}
}
