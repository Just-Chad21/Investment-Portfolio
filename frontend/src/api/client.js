const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

// Normalizes both network failures and backend ErrorResponse bodies (Phase 5) into one
// shape components can rely on, instead of branching on fetch's own error conventions.
export class ApiError extends Error {
  constructor(message, status, fieldErrors = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      headers: { 'Content-Type': 'application/json', ...options.headers },
      ...options,
    });
  } catch {
    throw new ApiError('Could not reach the server. Is the backend running?', 0);
  }

  if (!response.ok) {
    const body = await safeJson(response);
    throw new ApiError(
      body?.message ?? `Request failed with status ${response.status}`,
      response.status,
      body?.fieldErrors ?? [],
    );
  }

  return response;
}

async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

function buildQuery(filters) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params.set(key, value);
    }
  });
  const qs = params.toString();
  return qs ? `?${qs}` : '';
}

export async function login(email) {
  const response = await request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
  return response.json();
}

export async function getPortfolio(investorId) {
  const response = await request(`/api/investors/${investorId}/portfolio`);
  return response.json();
}

export async function getWithdrawalHistory(investorId, filters = {}) {
  const response = await request(`/api/investors/${investorId}/withdrawals${buildQuery(filters)}`);
  return response.json();
}

export async function submitWithdrawal(investorId, payload) {
  const response = await request(`/api/investors/${investorId}/withdrawals`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return response.json();
}

export function csvExportUrl(investorId, filters = {}) {
  return `${BASE_URL}/api/investors/${investorId}/withdrawals/export${buildQuery(filters)}`;
}
