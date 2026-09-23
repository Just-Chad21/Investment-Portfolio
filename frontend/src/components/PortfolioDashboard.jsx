import { ApiError } from '../api/client';
import { PRODUCT_TYPE_LABELS, isRetirementProduct } from '../productTypes';

const currencyFormatter = new Intl.NumberFormat('en-ZA', {
  style: 'currency',
  currency: 'ZAR',
});

// A locked padlock — the one icon in this app that means something specific (age-gated),
// rather than decoration, so it only ever appears next to a retirement product.
function LockIcon() {
  return (
    <svg width="11" height="11" viewBox="0 0 16 16" fill="none" aria-hidden="true">
      <rect x="3.5" y="7" width="9" height="6.5" rx="1.3" stroke="currentColor" strokeWidth="1.3" />
      <path d="M5.5 7V4.8a2.5 2.5 0 0 1 5 0V7" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  );
}

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
          {portfolio.products.map((product) => {
            const retirement = isRetirementProduct(product.productType);
            return (
              <div className={`product-row${retirement ? ' product-row--retirement' : ''}`} key={product.productId}>
                <div className="product-info">
                  <span className="product-name">{product.name}</span>
                  <span className="product-category">
                    {retirement && <LockIcon />}
                    {retirement ? 'Retirement' : 'Standard'} · {PRODUCT_TYPE_LABELS[product.productType] ?? product.productType}
                  </span>
                </div>
                <span className="product-balance">{currencyFormatter.format(product.balance)}</span>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
