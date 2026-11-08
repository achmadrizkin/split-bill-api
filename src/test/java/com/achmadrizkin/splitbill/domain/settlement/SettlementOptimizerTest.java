package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.money.Money;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.achmadrizkin.splitbill.support.Fixtures.idr;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class SettlementOptimizerTest {

    private static final UUID ANDI = UUID.randomUUID();
    private static final UUID BUDI = UUID.randomUUID();
    private static final UUID CITRA = UUID.randomUUID();
    private static final UUID DEWI = UUID.randomUUID();

    private static Map<UUID, Money> nets(Object... pairs) {
        Map<UUID, Money> map = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((UUID) pairs[i], idr((String) pairs[i + 1]));
        }
        return map;
    }

    @Test
    void settlesBaliTripWithTwoTransfersLargestDebtFirst() {
        List<Transfer> transfers = SettlementOptimizer.optimize(
                nets(ANDI, "850000", BUDI, "-350000", CITRA, "-500000"));

        assertThat(transfers).containsExactly(
                new Transfer(CITRA, ANDI, idr("500000")),
                new Transfer(BUDI, ANDI, idr("350000")));
    }

    @Test
    void collapsesDebtChainsIntoASingleTransfer() {
        // Andi owes Budi 10, Budi owes Citra 10 -> Budi is square, Andi pays Citra directly.
        List<Transfer> transfers = SettlementOptimizer.optimize(nets(ANDI, "-10", BUDI, "0", CITRA, "10"));

        assertThat(transfers).containsExactly(new Transfer(ANDI, CITRA, idr("10")));
    }

    @Test
    void oneDebtorPaysSeveralCreditors() {
        List<Transfer> transfers = SettlementOptimizer.optimize(
                nets(ANDI, "-90", BUDI, "30", CITRA, "60"));

        assertThat(transfers).containsExactly(
                new Transfer(ANDI, CITRA, idr("60")),
                new Transfer(ANDI, BUDI, idr("30")));
    }

    @Test
    void neverNeedsMoreThanNMinusOneTransfers() {
        List<Transfer> transfers = SettlementOptimizer.optimize(
                nets(ANDI, "-40", BUDI, "-35", CITRA, "45", DEWI, "30"));

        assertThat(transfers).hasSizeLessThanOrEqualTo(3);
        assertThat(transfers).allSatisfy(t -> assertThat(t.amount().isPositive()).isTrue());
    }

    @Test
    void tiesAreBrokenByInputOrderSoOutputIsDeterministic() {
        List<Transfer> transfers = SettlementOptimizer.optimize(
                nets(ANDI, "-10", BUDI, "-10", CITRA, "10", DEWI, "10"));

        assertThat(transfers).containsExactly(
                new Transfer(ANDI, CITRA, idr("10")),
                new Transfer(BUDI, DEWI, idr("10")));
    }

    @Test
    void nothingToSettleWhenEveryoneIsSquare() {
        assertThat(SettlementOptimizer.optimize(nets(ANDI, "0", BUDI, "0"))).isEmpty();
        assertThat(SettlementOptimizer.optimize(Map.of())).isEmpty();
    }

    @Test
    void refusesUnbalancedInput() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                SettlementOptimizer.optimize(nets(ANDI, "-10", BUDI, "9.99")));
    }
}
