package com.achmadrizkin.splitbill.support;

import com.achmadrizkin.splitbill.domain.DomainException;
import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.money.Money;
import org.assertj.core.api.ThrowableAssert;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public final class Fixtures {

    public static final Currency IDR = Currency.getInstance("IDR");
    public static final Currency USD = Currency.getInstance("USD");
    public static final Currency JPY = Currency.getInstance("JPY");

    private Fixtures() {
    }

    public static Money idr(String amount) {
        return Money.of(new java.math.BigDecimal(amount), IDR);
    }

    public static void assertDomainError(ThrowableAssert.ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.errorCode()).isEqualTo(expected));
    }
}
