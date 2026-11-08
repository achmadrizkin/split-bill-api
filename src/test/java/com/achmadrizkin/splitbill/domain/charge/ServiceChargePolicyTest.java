package com.achmadrizkin.splitbill.domain.charge;

import com.achmadrizkin.splitbill.domain.money.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static com.achmadrizkin.splitbill.support.Fixtures.USD;
import static com.achmadrizkin.splitbill.support.Fixtures.idr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class ServiceChargePolicyTest {

    @ParameterizedTest(name = "{0} -> {1}%")
    @CsvSource({
            "johndoe47, 0",      // example from the challenge README: sum 850
            "achmadrizkin, 9",   // submission username: sum 1269
            "AchmadRizkin, 9",   // must be lowercased first
            "' achmadrizkin ', 9"
    })
    void percentageIsUnicodeSumOfLowercaseUsernameModTen(String username, int expectedPct) {
        assertThat(ServiceChargePolicy.forGithubUsername(username).percentage()).isEqualTo(expectedPct);
    }

    @Test
    void chargeIsPercentageOfTotalExpenses() {
        ServiceChargePolicy policy = ServiceChargePolicy.forGithubUsername("achmadrizkin");

        assertThat(policy.chargeFor(idr("1900000"))).isEqualTo(idr("171000"));
    }

    @Test
    void chargeIsRoundedHalfEvenToCurrencyScale() {
        ServiceChargePolicy policy = ServiceChargePolicy.forGithubUsername("achmadrizkin");

        assertThat(policy.chargeFor(Money.of(new BigDecimal("0.50"), USD)).amount()).isEqualByComparingTo("0.04");
    }

    @Test
    void zeroPercentMeansZeroCharge() {
        assertThat(ServiceChargePolicy.forGithubUsername("johndoe47").chargeFor(idr("1900000"))).isEqualTo(idr("0"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void usernameIsRequired(String username) {
        assertThatIllegalArgumentException().isThrownBy(() -> ServiceChargePolicy.forGithubUsername(username));
    }
}
