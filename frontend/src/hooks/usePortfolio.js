import { useCallback, useEffect, useState } from 'react';
import { getPortfolio } from '../api/client';

// Shared by PortfolioDashboard (renders it) and WithdrawalForm (needs the product list
// for its dropdown) so there's one fetch and one source of truth, not two independent ones.
export function usePortfolio(investorId) {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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
