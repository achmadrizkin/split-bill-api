package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.ErrorCode;
import com.achmadrizkin.splitbill.domain.split.Share;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.achmadrizkin.splitbill.support.Fixtures.IDR;
import static com.achmadrizkin.splitbill.support.Fixtures.assertDomainError;
import static com.achmadrizkin.splitbill.support.Fixtures.idr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BalanceCalculatorTest {

    private static final UUID ANDI = UUID.randomUUID();
    private static final UUID BUDI = UUID.randomUUID();
    private static final UUID CITRA = UUID.randomUUID();
    private static final List<UUID> MEMBERS = List.of(ANDI, BUDI, CITRA);

    /** The "Bali Trip" example from CLAUDE.md, calculated by hand. */
    private static List<LedgerExpense> baliTrip() {
        return List.of(
                new LedgerExpense(ANDI, idr("1500000"), List.of(
                        new Share(ANDI, idr("500000")), new Share(BUDI, idr("500000")), new Share(CITRA, idr("500000")))),
                new LedgerExpense(BUDI, idr("300000"), List.of(
                        new Share(ANDI, idr("100000")), new Share(BUDI, idr("100000")), new Share(CITRA, idr("100000")))),
                new LedgerExpense(CITRA, idr("100000"), List.of(
                        new Share(ANDI, idr("50000")), new Share(BUDI, idr("50000")))));
    }

    @Test
    void computesPaidShareAndNetPerParticipant() {
        List<ParticipantBalance> balances = BalanceCalculator.calculate(IDR, MEMBERS, baliTrip(), List.of());

        assertThat(balances).extracting(ParticipantBalance::participantId).containsExactly(ANDI, BUDI, CITRA);
        assertThat(balances.get(0).paid()).isEqualTo(idr("1500000"));
        assertThat(balances.get(0).share()).isEqualTo(idr("650000"));
        assertThat(balances).extracting(ParticipantBalance::net)
                .containsExactly(idr("850000"), idr("-350000"), idr("-500000"));
    }

    @Test
    void paymentsMoveNetPositionsTowardsZero() {
        List<ParticipantBalance> balances = BalanceCalculator.calculate(IDR, MEMBERS, baliTrip(),
                List.of(new LedgerPayment(BUDI, ANDI, idr("350000"))));

        assertThat(balances).extracting(ParticipantBalance::net)
                .containsExactly(idr("500000"), idr("0"), idr("-500000"));
        assertThat(balances.get(1).paymentsSent()).isEqualTo(idr("350000"));
        assertThat(balances.get(0).paymentsReceived()).isEqualTo(idr("350000"));
    }

    @Test
    void emptyLedgerMeansEveryoneIsSquare() {
        assertThat(BalanceCalculator.calculate(IDR, MEMBERS, List.of(), List.of()))
                .allSatisfy(b -> assertThat(b.net().isZero()).isTrue());
    }

    @Test
    void rejectsLedgerEntriesForUnknownParticipants() {
        UUID stranger = UUID.randomUUID();
        List<LedgerExpense> expenses = List.of(
                new LedgerExpense(stranger, idr("10"), List.of(new Share(ANDI, idr("10")))));

        assertDomainError(() -> BalanceCalculator.calculate(IDR, MEMBERS, expenses, List.of()),
                ErrorCode.PARTICIPANT_NOT_IN_GROUP);
    }

    @Test
    void ledgerExpenseRefusesSharesThatDoNotAddUp() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                new LedgerExpense(ANDI, idr("100"), List.of(new Share(ANDI, idr("99.99")))));
    }
}
