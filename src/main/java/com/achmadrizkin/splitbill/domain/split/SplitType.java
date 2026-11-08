package com.achmadrizkin.splitbill.domain.split;

public enum SplitType {
    /** Divide equally among the listed participants (everyone, if none listed). */
    EQUAL,
    /** Each participant's exact amount; must add up to the expense total. */
    EXACT,
    /** Each participant's percentage; must add up to exactly 100. */
    PERCENTAGE,
    /** Whole-number weights, e.g. 2:1:1 (someone who ordered double). */
    SHARES
}
