package com.rungs.readiness.application.validation;

import com.rungs.readiness.application.InputText;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.math.BigDecimal;
import java.util.regex.Pattern;

/** Checks {@link Percentage}. */
public final class PercentageValidator implements ConstraintValidator<Percentage, String> {

    /** Longest text inspected, counted before trimming, as for money; bounds the cost of the decimal conversion. */
    private static final int MAX_TEXT_LENGTH = 32;

    /** A plain decimal in ASCII digits; {@code [0-9]} rather than {@code \d} so other scripts' digits never match. */
    private static final Pattern PLAIN_DECIMAL = Pattern.compile("-?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)");

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private int scale;

    @Override
    public void initialize(Percentage constraint) {
        scale = constraint.scale();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (InputText.isBlank(value)) {
            return true;
        }
        String trimmed = InputText.strip(value);
        if (value.length() > MAX_TEXT_LENGTH || !PLAIN_DECIMAL.matcher(trimmed).matches()) {
            return Violations.reject(context, ValidationCode.INVALID_PERCENT);
        }
        BigDecimal percent = new BigDecimal(trimmed);
        if (percent.signum() < 0 || percent.compareTo(ONE_HUNDRED) > 0) {
            return Violations.reject(context, ValidationCode.PERCENT_RANGE);
        }
        if (percent.stripTrailingZeros().scale() > scale) {
            return Violations.reject(context, ValidationCode.TOO_PRECISE);
        }
        return true;
    }
}
