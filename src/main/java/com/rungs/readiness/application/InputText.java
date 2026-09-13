package com.rungs.readiness.application;

/**
 * The one definition of blank submitted text shared by normalization and validation (PRD FR-4). A character counts
 * as space when it is Java whitespace or a Unicode space separator, the same test {@code Money.parse} trims with, so
 * text of only non-breaking spaces is blank.
 */
public final class InputText {

    private InputText() {
    }

    /** Returns whether {@code text} is null, empty, or made only of space characters. */
    public static boolean isBlank(String text) {
        return text == null || strip(text).isEmpty();
    }

    /** Returns {@code text} without leading and trailing space characters; throws NullPointerException for null. */
    public static String strip(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && isSpace(text.charAt(start))) {
            start++;
        }
        while (end > start && isSpace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(start, end);
    }

    private static boolean isSpace(char symbol) {
        return Character.isWhitespace(symbol) || Character.isSpaceChar(symbol);
    }
}
