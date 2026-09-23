import { ApiError } from '../api/client';

const currencyFormatter = new Intl.NumberFormat('en-ZA', {
  style: 'currency',
  currency: 'ZAR',
});

const PRODUCT_TYPE_LABELS = {
  UNIT_TRUST: 'Unit Trust',
  MONEY_MARKET: 'Money Market',
  TAX_FREE_SAVINGS: 'Tax-Free Savings',
  RETIREMENT_ANNUITY: 'Retirement Annuity',
  PRESERVATION_FUND: 'Preservation Fund',
};

export default function PortfolioDashboard({ investorId, portfolio, loading, error, onRequestWithdrawal }) {
  if (loading) {
    return <p className="muted">Loading portfolio...</p>;
  }

  if (error) {
    return (
      <p role="alert" className="error-banner">
        {error instanceof ApiError && error.status === 404
          ? `No investor found with id ${investorId}.`
          : `Couldn't load the portfolio: ${error.message}`}
      </p>
    );
  }

  const total = portfolio.products.reduce((sum, product) => sum + product.balance, 0);

  return (
    <section className="portfolio-panel" aria-labelledby="portfolio-heading">
      <div className="portfolio-identity">
        <h2 id="portfolio-heading">
          {portfolio.firstName} {portfolio.lastName}
        </h2>
        <span className="portfolio-number-tag">{portfolio.portfolioNumber}</span>
      </div>

      <div className="portfolio-body">
        <div className="portfolio-hero">
          <p className="hero-figure-label">Total portfolio value</p>
          <p className="hero-figure-value">{currencyFormatter.format(total)}</p>
          <button type="button" className="withdraw-trigger" onClick={onRequestWithdrawal}>
            Request a withdrawal
          </button>
        </div>

        <div className="product-list">
          {portfolio.products.map((product) => (
            <div className="product-row" key={product.productId}>
              <span className="product-name">
                {product.name}
                <span className="product-type-tag">{PRODUCT_TYPE_LABELS[product.productType] ?? product.productType}</span>
              </span>
              <span className="product-balance">{currencyFormatter.format(product.balance)}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}
