package com.rungs.readiness.web;

import com.rungs.readiness.domain.model.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Formatting shared by the web layer's view builders. */
final class Formats {

    /** Index 0 is unused; index n spells the number n for one to ten. */
    private static final String[] SPELLED_NUMBERS = {
        "", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
    };

    private Formats() {
    }

    /** Returns {@code amount} with a dollar sign and, unless it is a whole number of dollars, exactly two cents. */
    static String money(Money amount) {
        String display = amount.toDisplayString();
        return display.endsWith(".00") ? display.substring(0, display.length() - 3) : display;
    }

    /** Returns {@code amount} formatted as money, or {@code "-"} when it is zero. */
    static String moneyOrDash(Money amount) {
        return amount.isZero() ? "-" : amount.toDisplayString();
    }

    /** Returns {@code month} as e.g. "March 2041". */
    static String monthYear(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.FULL, Locale.US) + " " + month.getYear();
    }

    /** Returns {@code month} as e.g. "Sep 2026". */
    static String monthShort(YearMonth month) {
        return month.getMonth().getDisplayName(TextStyle.SHORT, Locale.US) + " " + month.getYear();
    }

    /**
     * Returns {@code percent} with no trailing zeros and never in scientific notation, e.g. {@code "6"} or
     * {@code "6.5"}.
     */
    static String plainPercent(BigDecimal percent) {
        return percent.stripTrailingZeros().toPlainString();
    }

    /** Returns {@code rate} formatted as a percentage with exactly two decimal places, e.g. "26.99%". */
    static String fixedPercent(BigDecimal rate) {
        return rate.setScale(2, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /** Returns the word for a whole {@code count} from one to ten, else its digits. */
    static String spellOrDigits(int count) {
        return spellOrDigits(BigDecimal.valueOf(count));
    }

    /**
     * Returns the word for a whole {@code value} from one to ten, else its digits (e.g. {@code "8.5"} or
     * {@code "15"}).
     */
    static String spellOrDigits(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        if (stripped.scale() <= 0) {
            int whole = stripped.intValueExact();
            if (whole >= 1 && whole <= SPELLED_NUMBERS.length - 1) {
                return SPELLED_NUMBERS[whole];
            }
            return Integer.toString(whole);
        }
        return stripped.toPlainString();
    }

    /** Returns {@code word} with its first character upper-cased. */
    static String capitalize(String word) {
        return word.isEmpty() ? word : Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

    /**
     * Returns the time from {@code from} to {@code to} as "Y years M months": singular forms and zero parts
     * omitted. Assumes {@code to} is after {@code from}.
     */
    static String duration(YearMonth from, YearMonth to) {
        long totalMonths = ChronoUnit.MONTHS.between(from, to);
        long years = totalMonths / 12;
        long months = totalMonths % 12;
        StringBuilder text = new StringBuilder();
        if (years > 0) {
            text.append(years).append(years == 1 ? " year" : " years");
        }
        if (months > 0) {
            if (text.length() > 0) {
                text.append(" ");
            }
            text.append(months).append(months == 1 ? " month" : " months");
        }
        return text.toString();
    }
}
