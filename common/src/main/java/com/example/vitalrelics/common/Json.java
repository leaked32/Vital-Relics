package com.example.vitalrelics.common;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class Json {
	private Json() {}

	public static Object parse(final String text) {
		return new Parser(text).parse();
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> parseObject(final String text) {
		return (Map<String, Object>) parse(text);
	}

	@SuppressWarnings("unchecked")
	public static List<Object> parseArray(final String text) {
		return (List<Object>) parse(text);
	}

	private static final class Parser {
		private final String text_;
		private int pos_;

		Parser(final String text) {
			text_ = text;
		}

		Object parse() {
			skipWhitespace();
			final Object value = parseValue();
			skipWhitespace();

			if (pos_ != text_.length())
				throw error("Unexpected trailing content");

			return value;
		}

		private Object parseValue() {
			skipWhitespace();

			if (pos_ >= text_.length())
				throw error("Unexpected end of JSON");

			return switch (text_.charAt(pos_)) {
				case '{' -> parseObjectValue();
				case '[' -> parseArrayValue();
				case '"' -> parseString();
				case 't' -> parseLiteral("true", true);
				case 'f' -> parseLiteral("false", false);
				case 'n' -> parseLiteral("null", null);
				default -> parseNumber();
			};
		}

		private Map<String, Object> parseObjectValue() {
			expect('{');
			skipWhitespace();

			final Map<String, Object> object = new LinkedHashMap<>();

			if (consume('}'))
				return object;

			while (true) {
				skipWhitespace();

				if (peek() != '"')
					throw error("Expected object key");

				final String key = parseString();

				skipWhitespace();
				expect(':');

				final Object value = parseValue();
				object.put(key, value);

				skipWhitespace();

				if (consume('}'))
					return object;

				expect(',');
			}
		}

		private List<Object> parseArrayValue() {
			expect('[');
			skipWhitespace();

			final List<Object> array = new ArrayList<>();

			if (consume(']'))
				return array;

			while (true) {
				array.add(parseValue());
				skipWhitespace();

				if (consume(']'))
					return array;

				expect(',');
			}
		}

		private String parseString() {
			expect('"');

			final StringBuilder result = new StringBuilder();

			while (pos_ < text_.length()) {
				final char c = text_.charAt(pos_++);

				if (c == '"')
					return result.toString();

				if (c != '\\') {
					result.append(c);
					continue;
				}

				if (pos_ >= text_.length())
					throw error("Incomplete escape sequence");

				final char escaped = text_.charAt(pos_++);

				switch (escaped) {
					case '"' -> result.append('"');
					case '\\' -> result.append('\\');
					case '/' -> result.append('/');
					case 'b' -> result.append('\b');
					case 'f' -> result.append('\f');
					case 'n' -> result.append('\n');
					case 'r' -> result.append('\r');
					case 't' -> result.append('\t');
					case 'u' -> result.append(parseUnicode());
					default -> throw error("Invalid escape sequence");
				}
			}

			throw error("Unterminated string");
		}

		private char parseUnicode() {
			if (pos_ + 4 > text_.length())
				throw error("Incomplete unicode escape");

			final String hex = text_.substring(pos_, pos_ + 4);
			pos_ += 4;

			try {
				return (char) Integer.parseInt(hex, 16);
			} catch (NumberFormatException e) {
				throw error("Invalid unicode escape");
			}
		}

		private Object parseNumber() {
			final int start = pos_;

			if (consume('-')) {}

			if (consume('0')) {
				// Leading zero is valid only by itself.
			} else {
				requireDigit();

				while (isDigit(peek()))
					pos_++;
			}

			boolean floatingPoint = false;

			if (consume('.')) {
				floatingPoint = true;
				requireDigit();

				while (isDigit(peek()))
					pos_++;
			}

			final char exponent = peek();

			if (exponent == 'e' || exponent == 'E') {
				floatingPoint = true;
				pos_++;

				if (peek() == '+' || peek() == '-')
					pos_++;

				requireDigit();

				while (isDigit(peek()))
					pos_++;
			}

			final String number = text_.substring(start, pos_);

			try {
				if (floatingPoint)
					return Double.parseDouble(number);

				final long value = Long.parseLong(number);

				if (value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE)
					return (int) value;

				return value;
			} catch (NumberFormatException e) {
				throw error("Invalid number");
			}
		}

		private Object parseLiteral(final String literal, final Object value) {
			if (!text_.startsWith(literal, pos_))
				throw error("Expected '" + literal + "'");

			pos_ += literal.length();
			return value;
		}

		private void requireDigit() {
			if (!isDigit(peek()))
				throw error("Expected digit");
		}

		private boolean consume(final char expected) {
			if (peek() != expected)
				return false;

			pos_++;
			return true;
		}

		private void expect(final char expected) {
			if (!consume(expected))
				throw error("Expected '" + expected + "'");
		}

		private char peek() {
			if (pos_ >= text_.length())
				return '\0';

			return text_.charAt(pos_);
		}

		private void skipWhitespace() {
			while (pos_ < text_.length()) {
				final char c = text_.charAt(pos_);

				if (c != ' ' && c != '\n' && c != '\r' && c != '\t')
					break;

				pos_++;
			}
		}

		private static boolean isDigit(final char c) {
			return c >= '0' && c <= '9';
		}

		private IllegalArgumentException error(final String message) {
			return new IllegalArgumentException(message + " at position " + pos_);
		}
	}
}