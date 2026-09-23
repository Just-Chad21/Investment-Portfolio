import { useState } from 'react';
import BrandMark from './components/BrandMark';
import LoginScreen from './components/LoginScreen';
import Modal from './components/Modal';
import PortfolioDashboard from './components/PortfolioDashboard';
import WithdrawalForm from './components/WithdrawalForm';
import WithdrawalHistoryTable from './components/WithdrawalHistoryTable';
import { usePortfolio } from './hooks/usePortfolio';

const SESSION_STORAGE_KEY = 'enviro365.session';

function loadStoredSession() {
  try {
    const raw = localStorage.getItem(SESSION_STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    // Private browsing / storage disabled — fall back to logged-out rather than crash.
    return null;
  }
}

function storeSession(session) {
  try {
    if (session) {
      localStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(session));
    } else {
      localStorage.removeItem(SESSION_STORAGE_KEY);
    }
  } catch {
    // Ignore — session still works for the current tab via React state.
  }
}

function App() {
  const [session, setSession] = useState(loadStoredSession);
  const [historyRefreshToken, setHistoryRefreshToken] = useState(0);
  const [isWithdrawalModalOpen, setWithdrawalModalOpen] = useState(false);

  const investorId = session?.investorId;
  const { portfolio, loading, error, refetch } = usePortfolio(investorId);

  function handleLoginSuccess(loginResponse) {
    const newSession = {
      investorId: loginResponse.investorId,
      firstName: loginResponse.firstName,
      lastName: loginResponse.lastName,
      email: loginResponse.email,
    };
    setSession(newSession);
    storeSession(newSession);
  }

  function handleLogout() {
    setSession(null);
    storeSession(null);
  }

  function handleWithdrawalSubmitted() {
    refetch();
    setHistoryRefreshToken((token) => token + 1);
  }

  if (!session) {
    return <LoginScreen onLoginSuccess={handleLoginSuccess} />;
  }

  return (
    <>
      <div className="app-header">
        <div className="brand">
          <BrandMark />
          <h1>Enviro365</h1>
        </div>
        <div className="session-info">
          <span className="muted">
            {session.firstName} {session.lastName}
          </span>
          <button type="button" className="logout-button" onClick={handleLogout}>
            Log out
          </button>
        </div>
      </div>

      <div className="app-content">
        <PortfolioDashboard
          investorId={investorId}
          portfolio={portfolio}
          loading={loading}
          error={error}
          onRequestWithdrawal={() => setWithdrawalModalOpen(true)}
        />

        <WithdrawalHistoryTable investorId={investorId} refreshToken={historyRefreshToken} />
      </div>

      {isWithdrawalModalOpen && portfolio && (
        <Modal title="Request a withdrawal" onClose={() => setWithdrawalModalOpen(false)}>
          <WithdrawalForm
            key={investorId}
            investorId={investorId}
            products={portfolio.products}
            investorAge={portfolio.age}
            onSubmitted={handleWithdrawalSubmitted}
          />
        </Modal>
      )}
    </>
  );
}

export default App;
