package com.rungs.readiness.adapter.in.web.api;

import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import tools.jackson.core.JacksonException;

/**
 * Builds RFC 6901 JSON Pointers to request fields (story 011) from a validation property path or a Jackson binding
 * path, and orders them for the problem body. Every pointer starts with a prefix: empty for a top-level snapshot, or
 * {@code /snapshot} where the snapshot is nested (REQ-011).
 */
final class JsonPointers {

    /**
     * Orders pointers segment by segment. Two segments of ASCII digits compare by numeric value, so {@code /debts/2}
     * precedes {@code /debts/10}; a digit segment precedes any other; other segments compare as text; a pointer
     * precedes those it is a prefix of. Equal numbers written differently fall back to text, so the order is total.
     */
    static final Comparator<String> ORDER = JsonPointers::compare;

    private static final String SEPARATOR = "/";

    private JsonPointers() {
    }

    /**
     * Returns the pointer for a Jakarta Validation {@code path}: a node's list index precedes its property name, and a
     * list element node (a missing debt entry) is just its index. The input models hold no maps, so keys are unused.
     */
    static String fromViolation(String prefix, Path path) {
        List<String> segments = new ArrayList<>();
        for (Path.Node node : path) {
            if (node.getIndex() != null) {
                segments.add(Integer.toString(node.getIndex()));
            }
            if (node.getKind() == ElementKind.PROPERTY && node.getName() != null) {
                segments.add(node.getName());
            }
        }
        return join(prefix, segments);
    }

    /** Returns the pointer for a Jackson binding {@code path}: property names and array indexes become segments. */
    static String fromJackson(String prefix, List<JacksonException.Reference> path) {
        List<String> segments = new ArrayList<>();
        for (JacksonException.Reference reference : path) {
            if (reference.getPropertyName() != null) {
                segments.add(reference.getPropertyName());
            } else if (reference.getIndex() >= 0) {
                segments.add(Integer.toString(reference.getIndex()));
            }
        }
        return join(prefix, segments);
    }

    private static String join(String prefix, List<String> segments) {
        StringBuilder pointer = new StringBuilder(prefix);
        for (String segment : segments) {
            pointer.append(SEPARATOR).append(segment.replace("~", "~0").replace("/", "~1"));
        }
        return pointer.toString();
    }

    private static int compare(String left, String right) {
        String[] leftSegments = left.split(SEPARATOR, -1);
        String[] rightSegments = right.split(SEPARATOR, -1);
        int shared = Math.min(leftSegments.length, rightSegments.length);
        for (int index = 0; index < shared; index++) {
            int order = compareSegments(leftSegments[index], rightSegments[index]);
            if (order != 0) {
                return order;
            }
        }
        return Integer.compare(leftSegments.length, rightSegments.length);
    }

    private static int compareSegments(String left, String right) {
        boolean leftNumeric = isDigits(left);
        boolean rightNumeric = isDigits(right);
        if (leftNumeric && rightNumeric) {
            String leftValue = withoutLeadingZeros(left);
            String rightValue = withoutLeadingZeros(right);
            // Compared as text by length first, so indexes of any size never overflow.
            int order = leftValue.length() != rightValue.length()
                    ? Integer.compare(leftValue.length(), rightValue.length())
                    : leftValue.compareTo(rightValue);
            return order != 0 ? order : left.compareTo(right);
        }
        if (leftNumeric != rightNumeric) {
            return leftNumeric ? -1 : 1;
        }
        return left.compareTo(right);
    }

    private static boolean isDigits(String segment) {
        return !segment.isEmpty() && segment.chars().allMatch(symbol -> symbol >= '0' && symbol <= '9');
    }

    private static String withoutLeadingZeros(String digits) {
        int start = 0;
        while (start < digits.length() - 1 && digits.charAt(start) == '0') {
            start++;
        }
        return digits.substring(start);
    }
}
