package com.achmadrizkin.splitbill.domain.settlement;

import com.achmadrizkin.splitbill.domain.money.Money;

import java.util.UUID;

/**
 * A participant's position in the group ledger.
 *
 * <pre>net = paid - share + paymentsSent - paymentsReceived</pre>
 * net &gt; 0: the group owes them (creditor). net &lt; 0: they owe the group (debtor).
 */
public record ParticipantBalance(UUID participantId,
                                 Money paid,
                                 Money share,
                                 Money paymentsSent,
                                 Money paymentsReceived) {

    public Money net() {
        return paid.minus(share).plus(paymentsSent).minus(paymentsReceived);
    }
}
