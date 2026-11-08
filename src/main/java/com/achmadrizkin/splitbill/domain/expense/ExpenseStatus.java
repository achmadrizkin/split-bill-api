package com.achmadrizkin.splitbill.domain.expense;

/** Expenses are never deleted; a mistake is corrected by voiding it (kept for audit). */
public enum ExpenseStatus {
    ACTIVE,
    VOIDED
}
