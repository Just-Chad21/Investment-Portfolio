// Single source of truth for the ProductType enum's display label and category, mirroring
// the backend's ProductType (entity/ProductType.java). Shared by WithdrawalForm (age-rule
// hint) and PortfolioDashboard (retirement/standard badge) so the two can't drift apart.
export const PRODUCT_TYPE_LABELS = {
  UNIT_TRUST: 'Unit Trust',
  MONEY_MARKET: 'Money Market',
  TAX_FREE_SAVINGS: 'Tax-Free Savings',
  RETIREMENT_ANNUITY: 'Retirement Annuity',
  PRESERVATION_FUND: 'Preservation Fund',
};

const RETIREMENT_PRODUCT_TYPES = new Set(['RETIREMENT_ANNUITY', 'PRESERVATION_FUND']);

export function isRetirementProduct(productType) {
  return RETIREMENT_PRODUCT_TYPES.has(productType);
}

// Mirrors WithdrawalService's MIN_RETIREMENT_AGE and MAX_WITHDRAWAL_RATIO — used to show
// the same caps client-side (as a hint, never authoritative) instead of letting the user
// find out only after a round trip to the server.
export const MIN_RETIREMENT_AGE = 65;
export const MAX_WITHDRAWAL_RATIO = 0.9;
