package com.example.vitalrelics.common.guide;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownPage {
	public final String id;
	public final String title;
	public final List<Block> blocks;

	private MarkdownPage(
			final String id,
			final String title,
			final List<Block> blocks) {

		this.id = id;
		this.title = title;
		this.blocks = List.copyOf(blocks);
	}

	public static MarkdownPage parse(
			final String id,
			final String markdown) {

		if (id == null || id.isBlank())
			throw new IllegalArgumentException("id cannot be blank");
		if (markdown == null)
			throw new IllegalArgumentException("markdown cannot be null");

		final List<Block> blocks = new ArrayList<>();
		final List<String> paragraph = new ArrayList<>();
		final List<String> code = new ArrayList<>();
		final String[] lines = markdown
				.replace("\r\n", "\n")
				.replace('\r', '\n')
				.split("\n", -1);

		String title = id;
		String codeLanguage = "";
		boolean inCode = false;

		for (final String line : lines) {
			if (line.startsWith("```")) {
				flushParagraph(blocks, paragraph);

				if (inCode) {
					blocks.add(new CodeBlock(codeLanguage, List.copyOf(code)));
					code.clear();
					codeLanguage = "";
					inCode = false;
				} else {
					codeLanguage = line.substring(3).strip();
					inCode = true;
				}

				continue;
			}

			if (inCode) {
				code.add(line);
				continue;
			}

			if (line.isBlank()) {
				flushParagraph(blocks, paragraph);
				continue;
			}

			final int headingLevel = headingLevel(line);

			if (headingLevel > 0) {
				flushParagraph(blocks, paragraph);

				final String text = stripInline(
						line.substring(headingLevel + 1).strip()
				);

				if (headingLevel == 1 && blocks.isEmpty())
					title = text;
				else
					blocks.add(new Heading(headingLevel, text));

				continue;
			}

			final ListItem listItem = parseListItem(line);

			if (listItem != null) {
				flushParagraph(blocks, paragraph);
				blocks.add(listItem);
				continue;
			}

			if (isTableLine(line)) {
				flushParagraph(blocks, paragraph);
				blocks.add(new Table(parseTableRow(line)));
				continue;
			}

			paragraph.add(line.strip());
		}

		flushParagraph(blocks, paragraph);

		if (inCode)
			blocks.add(new CodeBlock(codeLanguage, List.copyOf(code)));

		return new MarkdownPage(id, title, blocks);
	}

	private static int headingLevel(final String line) {
		int level = 0;

		while (level < line.length() && line.charAt(level) == '#')
			++level;

		return level > 0
				&& level <= 6
				&& level < line.length()
				&& line.charAt(level) == ' '
				? level
				: 0;
	}

	private static ListItem parseListItem(final String line) {
		final String stripped = line.stripLeading();
		final int indent = line.length() - stripped.length();

		if (stripped.startsWith("- ") || stripped.startsWith("* ")) {
			return new ListItem(
					false,
					"",
					indent,
					stripInline(stripped.substring(2))
			);
		}

		int index = 0;

		while (index < stripped.length()
				&& Character.isDigit(stripped.charAt(index))) {
			++index;
		}

		if (index > 0
				&& index + 1 < stripped.length()
				&& stripped.charAt(index) == '.'
				&& stripped.charAt(index + 1) == ' ') {

			return new ListItem(
					true,
					stripped.substring(0, index),
					indent,
					stripInline(stripped.substring(index + 2))
			);
		}

		return null;
	}

	private static boolean isTableLine(final String line) {
		final String stripped = line.strip();

		return stripped.startsWith("|")
				&& stripped.endsWith("|")
				&& stripped.length() > 2;
	}

	private static List<String> parseTableRow(final String line) {
		final String stripped = line.strip();
		final String body = stripped.substring(1, stripped.length() - 1);
		final String[] cells = body.split("\\|", -1);
		final List<String> result = new ArrayList<>();

		for (final String cell : cells)
			result.add(stripInline(cell.strip()));

		return result;
	}

	private static void flushParagraph(
			final List<Block> blocks,
			final List<String> paragraph) {

		if (paragraph.isEmpty())
			return;

		blocks.add(new Paragraph(
				stripInline(String.join(" ", paragraph))
		));

		paragraph.clear();
	}

	public static boolean isTableSeparator(final Table table) {
		if (table.cells().isEmpty())
			return false;

		for (final String cell : table.cells()) {
			final String stripped = cell
					.replace(":", "")
					.replace("-", "")
					.strip();

			if (!stripped.isEmpty())
				return false;
		}

		return true;
	}

	public static String stripInline(final String text) {
		return text
				.replace("**", "")
				.replace("__", "")
				.replace("`", "");
	}

	public sealed interface Block
			permits Heading, Paragraph, ListItem, CodeBlock, Table {}

	public record Heading(int level, String text) implements Block {}
	public record Paragraph(String text) implements Block {}
	public record ListItem(
			boolean ordered,
			String marker,
			int indent,
			String text) implements Block {}
	public record CodeBlock(
			String language,
			List<String> lines) implements Block {}
	public record Table(List<String> cells) implements Block {}
}
