import { useCallback, useEffect, useState } from 'react';
import { getPortfolio } from '../api/client';

/**
 * Loads an investor's portfolio and reloads it whenever investorId changes. Called once in App,
 * which passes the result to both PortfolioDashboard and WithdrawalForm so they share one fetch.
 * Returns { portfolio, loading, error, refetch }; call refetch after a withdrawal to pick up new balances.
 */
export function usePortfolio(investorId) {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Returns a cleanup that ignores the response if it arrives late, e.g. after switching investors,
  // so a slow response for the previous investor can't overwrite the current one.
  const refetch = useCallback(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    getPortfolio(investorId)
      .then((data) => {
        if (!cancelled) setPortfolio(data);
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
  }, [investorId]);

  useEffect(() => refetch(), [refetch]);

  return { portfolio, loading, error, refetch };
}
