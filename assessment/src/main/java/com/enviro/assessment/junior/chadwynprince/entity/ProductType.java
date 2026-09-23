package com.enviro.assessment.junior.chadwynprince.entity;

/**
 * The standardized set of product types a Product can be. This replaces a client-supplied
 * withdrawal "type" (STANDARD/RETIREMENT chosen per request) with a permanent property of
 * the product itself: each type maps to exactly one WithdrawalType category, and that
 * category is what WithdrawalService uses to decide whether the age > 65 rule applies —
 * never what the withdrawal request claims.
 */
public enum ProductType {
    UNIT_TRUST(WithdrawalType.STANDARD),
    MONEY_MARKET(WithdrawalType.STANDARD),
    TAX_FREE_SAVINGS(WithdrawalType.STANDARD),
    RETIREMENT_ANNUITY(WithdrawalType.RETIREMENT),
    PRESERVATION_FUND(WithdrawalType.RETIREMENT);

    private final WithdrawalType category;

    ProductType(WithdrawalType category) {
        this.category = category;
    }

    public WithdrawalType getCategory() {
        return category;
    }

    public boolean isRetirement() {
        return category == WithdrawalType.RETIREMENT;
    }
}
