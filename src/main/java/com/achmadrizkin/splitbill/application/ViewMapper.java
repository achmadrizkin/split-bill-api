package com.achmadrizkin.splitbill.application;

import com.achmadrizkin.splitbill.application.view.Views.ExpenseView;
import com.achmadrizkin.splitbill.application.view.Views.GroupView;
import com.achmadrizkin.splitbill.application.view.Views.ParticipantView;
import com.achmadrizkin.splitbill.application.view.Views.PaymentView;
import com.achmadrizkin.splitbill.domain.money.Money;
import com.achmadrizkin.splitbill.domain.split.Share;
import com.achmadrizkin.splitbill.infrastructure.persistence.BillGroupEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.ExpenseEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.ParticipantEntity;
import com.achmadrizkin.splitbill.infrastructure.persistence.PaymentEntity;

import java.util.Currency;

/** Entity -> read model mapping. Re-wraps stored decimals as {@link Money} in the group currency. */
final class ViewMapper {

    private ViewMapper() {
    }

    static GroupView group(BillGroupEntity group) {
        return new GroupView(group.getId(), group.getName(), group.currencyUnit(),
                group.getParticipants().stream().map(ViewMapper::participant).toList(),
                group.getCreatedAt());
    }

    static ParticipantView participant(ParticipantEntity participant) {
        return new ParticipantView(participant.getId(), participant.getName());
    }

    static ExpenseView expense(ExpenseEntity e, Currency currency) {
        return new ExpenseView(e.getId(), e.getGroupId(), e.getDescription(), e.getCategory(),
                Money.of(e.getAmount(), currency), e.getPayerId(), e.getSplitType(), e.getStatus(),
                e.getVoidReason(),
                e.getShares().stream()
                        .map(s -> new Share(s.getParticipantId(), Money.of(s.getAmount(), currency)))
                        .toList(),
                e.getCreatedAt(), e.getVoidedAt());
    }

    static PaymentView payment(PaymentEntity p, Currency currency) {
        return new PaymentView(p.getId(), p.getGroupId(), p.getFromId(), p.getToId(),
                Money.of(p.getAmount(), currency), p.getNote(), p.getCreatedAt());
    }
}
