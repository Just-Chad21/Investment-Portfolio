import { useState } from 'react';
import { ApiError, submitWithdrawal } from '../api/client';
import { isRetirementProduct } from '../productTypes';

const currencyFormatter = new Intl.NumberFormat('en-ZA', {
  style: 'currency',
  currency: 'ZAR',
});

export default function WithdrawalForm({ investorId, products, investorAge, withdrawalRules, onSubmitted }) {
  const { minRetirementAge, maxWithdrawalRatio } = withdrawalRules;
  const [productId, setProductId] = useState('');
  const [amount, setAmount] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [validationErrors, setValidationErrors] = useState({});
  const [submitError, setSubmitError] = useState(null);
  const [result, setResult] = useState(null);

  // Only catches input that isn't a real withdrawal attempt at all (nothing selected,
  // no usable amount) — those are safe to stop at the field without ever reaching the
  // server. Business-rule violations (over the withdrawal cap, blocked by the age rule) are
  // deliberately NOT caught here: they're real attempts, and the server recording them
  // as REJECTED (with the reason) is the audit trail this app is built to keep. The
  // field-hint warnings below tell the user about those rules up front, but never stop
  // the submit.
  function validate() {
    const errors = {};
    if (!productId) {
      errors.productId = 'Select a product.';
    }
    if (!amount || Number.isNaN(Number(amount)) || Number(amount) <= 0) {
      errors.amount = 'Enter an amount greater than 0.';
    }
    return errors;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    const errors = validate();
    setValidationErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setSubmitting(true);
    setSubmitError(null);
    setResult(null);

    try {
      const response = await submitWithdrawal(investorId, {
        productId: Number(productId),
        amount: Number(amount),
      });
      setResult(response);
      // Both APPROVED and REJECTED are persisted outcomes, so
      // either way the portfolio balance and history may have changed — refresh both.
      onSubmitted?.();
      if (response.status === 'APPROVED') {
        setAmount('');
      }
    } catch (err) {
      setSubmitError(err);
    } finally {
      setSubmitting(false);
    }
  }

  function fieldErrorFor(field) {
    if (validationErrors[field]) {
      return validationErrors[field];
    }
    if (submitError instanceof ApiError) {
      return submitError.fieldErrors.find((fe) => fe.field === field)?.message;
    }
    return undefined;
  }

  const hasFieldErrors = submitError instanceof ApiError && submitError.fieldErrors.length > 0;

  const isUnderRetirementAge = typeof investorAge === 'number' && investorAge <= minRetirementAge;
  const selectedProduct = products.find((product) => String(product.productId) === productId);
  const isRetirementSelected = selectedProduct && isRetirementProduct(selectedProduct.productType);
  const ageRuleWarning = Boolean(isRetirementSelected && isUnderRetirementAge);
  const maxWithdrawable = selectedProduct ? selectedProduct.balance * maxWithdrawalRatio : null;

  return (
    <div className="modal-body">
      {result && (
        <p
          className={result.status === 'APPROVED' ? 'success-banner' : 'error-banner'}
          role="status"
        >
          {result.status === 'APPROVED'
            ? `Approved: withdrew ${currencyFormatter.format(result.amount)} from ${result.productName}. New balance: ${currencyFormatter.format(result.balanceAfter)}.`
            : `Rejected: ${result.rejectionReason}`}
        </p>
      )}

      {submitError && !hasFieldErrors && (
        <p className="error-banner" role="alert">
          {submitError.message}
        </p>
      )}

      <form onSubmit={handleSubmit} noValidate>
        <div className="form-field">
          <label htmlFor="withdrawal-product">Product</label>
          <select
            id="withdrawal-product"
            value={productId}
            onChange={(event) => setProductId(event.target.value)}
          >
            <option value="">Select a product...</option>
            {products.map((product) => (
              <option key={product.productId} value={product.productId}>
                {product.name} ({currencyFormatter.format(product.balance)})
              </option>
            ))}
          </select>
          {fieldErrorFor('productId') && <span className="field-error">{fieldErrorFor('productId')}</span>}
          {ageRuleWarning && !fieldErrorFor('productId') && (
            <span className="field-hint warning">
              <span aria-hidden="true">⚠ </span>
              This is a retirement product — withdrawals require age over {minRetirementAge}. You are{' '}
              {investorAge}, so this will likely be rejected.
            </span>
          )}
        </div>

        <div className="form-field">
          <label htmlFor="withdrawal-amount">Amount (ZAR)</label>
          <input
            id="withdrawal-amount"
            type="number"
            min="0.01"
            max={maxWithdrawable ?? undefined}
            step="0.01"
            value={amount}
            onChange={(event) => setAmount(event.target.value)}
          />
          {fieldErrorFor('amount') && <span className="field-error">{fieldErrorFor('amount')}</span>}
          {!fieldErrorFor('amount') && maxWithdrawable != null && (
            <span className="field-hint">
              You can withdraw up to {currencyFormatter.format(maxWithdrawable)} ({Math.round(maxWithdrawalRatio * 100)}% of the balance).
            </span>
          )}
        </div>

        <button type="submit" disabled={submitting || products.length === 0}>
          {submitting ? 'Submitting...' : 'Submit withdrawal'}
        </button>
      </form>
    </div>
  );
}
