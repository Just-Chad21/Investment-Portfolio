// Baked in at build time by Vite; override with VITE_API_BASE_URL when the backend runs elsewhere.
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

/**
 * The single error type every API call throws, for network failures and backend ErrorResponse bodies alike.
 * status is the HTTP status (0 if the server was unreachable); fieldErrors holds per-field
 * validation messages ({ field, message }) so forms can show them next to the right input.
 */
export class ApiError extends Error {
  constructor(message, status, fieldErrors = []) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

/**
 * fetch wrapper that sends JSON and throws ApiError on a network failure or any non-2xx response.
 * Returns the raw Response so each caller decides how to read the body.
 */
async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
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

/** Parses the body as JSON, or returns null if it isn't JSON (e.g. an HTML error page from a proxy). */
async function safeJson(response) {
  try {
    return await response.json();
  } catch {
    return null;
  }
}

/**
 * Turns a filters object into a query string ("?type=RETIREMENT&status=..."), or "" if none are set.
 * Empty, null and undefined values are skipped, so an "All" dropdown simply omits that filter.
 */
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

/** Resolves an email to its investor (there is no password). Throws ApiError with status 404 if no account matches. */
export async function login(email) {
  const response = await request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email }),
  });
  return response.json();
}

/** Fetches the investor's details, products with balances, and the withdrawal rule limits used for form hints. */
export async function getPortfolio(investorId) {
  const response = await request(`/api/investors/${investorId}/portfolio`);
  return response.json();
}

/** Fetches withdrawal notices, newest first, optionally filtered by type, status and from/to dates. */
export async function getWithdrawalHistory(investorId, filters = {}) {
  const response = await request(`/api/investors/${investorId}/withdrawals${buildQuery(filters)}`);
  return response.json();
}

/**
 * Submits { productId, amount }. A rule violation still resolves (status REJECTED, with a reason);
 * it only throws for invalid input or an unknown investor or product.
 */
export async function submitWithdrawal(investorId, payload) {
  const response = await request(`/api/investors/${investorId}/withdrawals`, {
    method: 'POST',
    body: JSON.stringify(payload),
  });
  return response.json();
}

/** Builds the CSV export URL for a plain download link; the browser fetches it directly, not through request(). */
export function csvExportUrl(investorId, filters = {}) {
  return `${BASE_URL}/api/investors/${investorId}/withdrawals/export${buildQuery(filters)}`;
}
