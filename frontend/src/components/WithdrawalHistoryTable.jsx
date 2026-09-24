import { useEffect, useState } from 'react';
import { ApiError, getWithdrawalHistory } from '../api/client';
import CsvDownloadButton from './CsvDownloadButton';

const dateFormatter = new Intl.DateTimeFormat('en-ZA', { dateStyle: 'medium', timeStyle: 'short' });
const currencyFormatter = new Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR' });

const EMPTY_FILTERS = { type: '', status: '', from: '', to: '' };

export default function WithdrawalHistoryTable({ investorId, refreshToken }) {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [filters, setFilters] = useState(EMPTY_FILTERS);

  function updateFilter(field, value) {
    setFilters((prev) => ({ ...prev, [field]: value }));
  }

  const hasActiveFilters = Object.values(filters).some((value) => value !== '');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    getWithdrawalHistory(investorId, filters)
      .then((data) => {
        if (!cancelled) setHistory(data);
      })
      .catch((err) => {
        if (!cancelled) setError(err);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [investorId, refreshToken, filters.type, filters.status, filters.from, filters.to]);

  return (
    <section className="history-panel" aria-labelledby="history-heading">
      <div className="history-header">
        <h2 id="history-heading">Withdrawal history</h2>
        <CsvDownloadButton investorId={investorId} filters={filters} />
      </div>

      <div className="history-filters">
        <div className="form-field">
          <label htmlFor="filter-type">Type</label>
          <select id="filter-type" value={filters.type} onChange={(event) => updateFilter('type', event.target.value)}>
            <option value="">All</option>
            <option value="STANDARD">Standard</option>
            <option value="RETIREMENT">Retirement</option>
          </select>
        </div>

        <div className="form-field">
          <label htmlFor="filter-status">Status</label>
          <select id="filter-status" value={filters.status} onChange={(event) => updateFilter('status', event.target.value)}>
            <option value="">All</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>
        </div>

        <div className="form-field">
          <label htmlFor="filter-from">From</label>
          <input
            id="filter-from"
            type="date"
            value={filters.from}
            onChange={(event) => updateFilter('from', event.target.value)}
          />
        </div>

        <div className="form-field">
          <label htmlFor="filter-to">To</label>
          <input
            id="filter-to"
            type="date"
            value={filters.to}
            onChange={(event) => updateFilter('to', event.target.value)}
          />
        </div>

        {hasActiveFilters && (
          <button type="button" className="clear-filters-button" onClick={() => setFilters(EMPTY_FILTERS)}>
            Clear filters
          </button>
        )}
      </div>

      {loading && <p className="muted">Loading history...</p>}

      {error && (
        <p role="alert" className="error-banner">
          {error instanceof ApiError ? error.message : "Couldn't load withdrawal history."}
        </p>
      )}

      {!loading && !error && history.length === 0 && (
        <p className="muted">{hasActiveFilters ? 'No withdrawals match these filters.' : 'No withdrawals yet.'}</p>
      )}

      {!loading && !error && history.length > 0 && (
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Date</th>
                <th>Product</th>
                <th>Type</th>
                <th>Status</th>
                <th className="amount-header">Amount</th>
                <th>Reason</th>
              </tr>
            </thead>
            <tbody>
              {history.map((notice) => (
                <tr key={notice.id}>
                  <td>{dateFormatter.format(new Date(notice.requestedAt))}</td>
                  <td>{notice.productName}</td>
                  <td>{notice.type}</td>
                  <td>
                    {notice.status === 'APPROVED' ? (
                      <span className="status-badge approved">
                        <span aria-hidden="true">&#10003;</span> Approved
                      </span>
                    ) : (
                      <span className="status-badge rejected">
                        <span aria-hidden="true">&#10005;</span> Rejected
                      </span>
                    )}
                  </td>
                  <td className="amount-cell">{currencyFormatter.format(notice.amount)}</td>
                  <td className="muted">{notice.rejectionReason ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}
