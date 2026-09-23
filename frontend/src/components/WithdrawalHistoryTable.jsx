import { useEffect, useState } from 'react';
import { ApiError, getWithdrawalHistory } from '../api/client';
import CsvDownloadButton from './CsvDownloadButton';

const dateFormatter = new Intl.DateTimeFormat('en-ZA', { dateStyle: 'medium', timeStyle: 'short' });
const currencyFormatter = new Intl.NumberFormat('en-ZA', { style: 'currency', currency: 'ZAR' });

export default function WithdrawalHistoryTable({ investorId, refreshToken }) {
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    getWithdrawalHistory(investorId)
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
  }, [investorId, refreshToken]);

  return (
    <section className="history-panel" aria-labelledby="history-heading">
      <div className="history-header">
        <h2 id="history-heading">Withdrawal history</h2>
        <CsvDownloadButton investorId={investorId} />
      </div>

      {loading && <p className="muted">Loading history...</p>}

      {error && (
        <p role="alert" className="error-banner">
          {error instanceof ApiError ? error.message : "Couldn't load withdrawal history."}
        </p>
      )}

      {!loading && !error && history.length === 0 && <p className="muted">No withdrawals yet.</p>}

      {!loading && !error && history.length > 0 && (
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
      )}
    </section>
  );
}
