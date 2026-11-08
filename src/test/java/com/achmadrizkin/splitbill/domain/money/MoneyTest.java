package com.achmadrizkin.splitbill.domain.money;

import com.achmadrizkin.splitbill.domain.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.BigInteger;

import static com.achmadrizkin.splitbill.support.Fixtures.IDR;
import static com.achmadrizkin.splitbill.support.Fixtures.JPY;
import static com.achmadrizkin.splitbill.support.Fixtures.USD;
import static com.achmadrizkin.splitbill.support.Fixtures.assertDomainError;
import static com.achmadrizkin.splitbill.support.Fixtures.idr;
import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Nested
    @DisplayName("creation")
    class Creation {

        @Test
        void normalizesScaleToCurrencyFractionDigits() {
            assertThat(idr("10").amount().toPlainString()).isEqualTo("10.00");
            assertThat(Money.of(new BigDecimal("100"), JPY).amount().scale()).isZero();
        }

        @Test
        void sameValueWithDifferentInputScaleIsEqual() {
            assertThat(idr("10.0")).isEqualTo(idr("10.00"));
        }

        @Test
        void rejectsMoreDecimalsThanTheCurrencyAllowsInsteadOfSilentlyRounding() {
            assertDomainError(() -> idr("10.005"), ErrorCode.AMOUNT_SCALE_EXCEEDED);
            assertDomainError(() -> Money.of(new BigDecimal("100.5"), JPY), ErrorCode.AMOUNT_SCALE_EXCEEDED);
        }

        @Test
        void convertsToAndFromMinorUnits() {
            Money money = idr("1234.56");
            assertThat(money.toMinorUnits()).isEqualTo(BigInteger.valueOf(123456));
            assertThat(Money.ofMinorUnits(BigInteger.valueOf(123456), IDR)).isEqualTo(money);
        }
    }

    @Nested
    @DisplayName("arithmetic")
    class Arithmetic {

        @Test
        void addsAndSubtractsExactly() {
            assertThat(idr("0.10").plus(idr("0.20"))).isEqualTo(idr("0.30"));
            assertThat(idr("100").minus(idr("150.50"))).isEqualTo(idr("-50.50"));
        }

        @Test
        void refusesToMixCurrencies() {
            Money usd = Money.of(BigDecimal.ONE, USD);
            assertDomainError(() -> idr("1").plus(usd), ErrorCode.CURRENCY_MISMATCH);
            assertDomainError(() -> idr("1").compareTo(usd), ErrorCode.CURRENCY_MISMATCH);
        }

        @Test
        void reportsSign() {
            assertThat(idr("0").isZero()).isTrue();
            assertThat(idr("0.01").isPositive()).isTrue();
            assertThat(idr("-0.01").isNegative()).isTrue();
            assertThat(idr("-5").abs()).isEqualTo(idr("5"));
            assertThat(idr("5").negate()).isEqualTo(idr("-5"));
        }

        @ParameterizedTest(name = "{1}% of {0} = {2} (HALF_EVEN)")
        @CsvSource({
                "0.50, 9, 0.04",   // 0.045 -> banker's rounding goes to even (0.04)
                "1.50, 9, 0.14",   // 0.135 -> 0.14
                "0.70, 9, 0.06",   // 0.063 -> 0.06
                "1900000, 9, 171000.00",
                "123.45, 0, 0.00"
        })
        void percentageUsesBankersRounding(String amount, int percent, String expected) {
            Money result = Money.of(new BigDecimal(amount), USD).percentage(BigDecimal.valueOf(percent));
            assertThat(result.amount()).isEqualByComparingTo(expected);
            assertThat(result.amount().scale()).isEqualTo(2);
        }
    }
}
