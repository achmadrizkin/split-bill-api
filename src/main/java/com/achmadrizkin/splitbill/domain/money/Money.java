package com.achmadrizkin.splitbill.domain.money;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Objects;

/**
 * Immutable monetary amount in a single currency.
 *
 * <ul>
 *   <li>Always backed by {@link BigDecimal}, normalized to the currency's minor-unit scale
 *       (IDR/USD = 2, JPY = 0) so that {@code equals} is reliable.</li>
 *   <li>Input with more precision than the currency supports is <b>rejected</b>, never silently rounded.</li>
 *   <li>Rounding only happens on <i>derived</i> values (e.g. percentages), using banker's rounding.</li>
 *   <li>Arithmetic across currencies is refused.</li>
 * </ul>
 */
public record Money(BigDecimal amount, Currency currency) implements Comparable<Money> {

    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = normalize(amount, currency);
    }

    public static Money of(BigDecimal amount, Currency currency) {
        return new Money(amount, currency);
    }

    public static Money zero(Currency currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public static Money ofMinorUnits(BigInteger minorUnits, Currency currency) {
        return new Money(new BigDecimal(minorUnits, scaleOf(currency)), currency);
    }

    /** Number of decimal places for the currency; fails for pseudo-currencies such as XAU. */
    public static int scaleOf(Currency currency) {
        int digits = currency.getDefaultFractionDigits();
        if (digits < 0) {
            throw new DomainException(ErrorCode.INVALID_CURRENCY,
                    "Currency " + currency.getCurrencyCode() + " is not a monetary currency");
        }
        return digits;
    }

    private static BigDecimal normalize(BigDecimal amount, Currency currency) {
        int scale = scaleOf(currency);
        try {
            return amount.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new DomainException(ErrorCode.AMOUNT_SCALE_EXCEEDED,
                    "Amount %s has more than %d decimal place(s), the maximum for %s"
                            .formatted(amount.toPlainString(), scale, currency.getCurrencyCode()));
        }
    }

    /** Amount in the smallest currency unit (e.g. cents). */
    public BigInteger toMinorUnits() {
        return amount.unscaledValue();
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public Money abs() {
        return new Money(amount.abs(), currency);
    }

    /** {@code percent}% of this amount, rounded half-even to the currency scale. */
    public Money percentage(BigDecimal percent) {
        BigDecimal result = amount.multiply(percent).divide(ONE_HUNDRED, scaleOf(currency), ROUNDING);
        return new Money(result, currency);
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    @Override
    public int compareTo(Money other) {
        requireSameCurrency(other);
        return amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new DomainException(ErrorCode.CURRENCY_MISMATCH,
                    "Cannot combine %s with %s".formatted(currency.getCurrencyCode(), other.currency.getCurrencyCode()));
        }
    }

    /** Plain decimal string without exponent, e.g. {@code "1500000.00"}. Used for API output. */
    public String toPlainString() {
        return amount.toPlainString();
    }

    @Override
    public String toString() {
        return amount.toPlainString() + " " + currency.getCurrencyCode();
    }
}
