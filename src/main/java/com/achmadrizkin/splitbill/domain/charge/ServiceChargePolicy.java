package com.achmadrizkin.splitbill.domain.charge;

import com.achmadrizkin.splitbill.domain.money.Money;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Personalized service charge required by the challenge:
 * <pre>
 *   pct    = (sum of Unicode code points of lowercase GitHub username) % 10
 *   amount = total group expenses * pct / 100   (half-even, currency scale)
 * </pre>
 */
public final class ServiceChargePolicy {

    private final int percentage;

    private ServiceChargePolicy(int percentage) {
        this.percentage = percentage;
    }

    public static ServiceChargePolicy forGithubUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("GitHub username is required to compute the service charge");
        }
        int codePointSum = username.trim().toLowerCase(Locale.ROOT).codePoints().sum();
        return new ServiceChargePolicy(codePointSum % 10);
    }

    public int percentage() {
        return percentage;
    }

    public Money chargeFor(Money totalExpenses) {
        return totalExpenses.percentage(BigDecimal.valueOf(percentage));
    }
}
