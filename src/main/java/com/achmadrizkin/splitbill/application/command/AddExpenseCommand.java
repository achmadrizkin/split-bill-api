package com.achmadrizkin.splitbill.application.command;

import com.achmadrizkin.splitbill.domain.expense.ExpenseCategory;
import com.achmadrizkin.splitbill.domain.split.SplitPortion;
import com.achmadrizkin.splitbill.domain.split.SplitType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Records have a deterministic {@code toString()}, which the idempotency fingerprint relies on.
 *
 * @param category  {@code null} means OTHER
 * @param splitType {@code null} means EQUAL
 * @param splits    optional for EQUAL (empty = whole group), required otherwise
 */
public record AddExpenseCommand(String description,
                                ExpenseCategory category,
                                BigDecimal amount,
                                UUID paidBy,
                                SplitType splitType,
                                List<SplitPortion> splits) {
}
