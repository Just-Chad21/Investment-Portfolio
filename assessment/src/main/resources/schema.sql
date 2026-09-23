CREATE TABLE investor (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    date_of_birth  DATE NOT NULL,
    email          VARCHAR(255) NOT NULL,
    CONSTRAINT uq_investor_email UNIQUE (email)
);

CREATE TABLE portfolio (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    investor_id       BIGINT NOT NULL,
    portfolio_number  VARCHAR(50) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_portfolio_investor UNIQUE (investor_id),
    CONSTRAINT uq_portfolio_number UNIQUE (portfolio_number),
    CONSTRAINT fk_portfolio_investor FOREIGN KEY (investor_id) REFERENCES investor(id)
);

CREATE TABLE product (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id  BIGINT NOT NULL,
    name          VARCHAR(100) NOT NULL,
    product_type  VARCHAR(30) NOT NULL,
    balance       DECIMAL(19,2) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_product_type CHECK (product_type IN
        ('UNIT_TRUST', 'MONEY_MARKET', 'TAX_FREE_SAVINGS', 'RETIREMENT_ANNUITY', 'PRESERVATION_FUND')),
    CONSTRAINT fk_product_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

CREATE TABLE withdrawal_notice (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT NOT NULL,
    investor_id       BIGINT NOT NULL,
    type              VARCHAR(20) NOT NULL,
    amount            DECIMAL(19,2) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    rejection_reason  VARCHAR(255),
    balance_after     DECIMAL(19,2),
    requested_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_withdrawal_type CHECK (type IN ('STANDARD', 'RETIREMENT')),
    CONSTRAINT ck_withdrawal_status CHECK (status IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ck_withdrawal_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_withdrawal_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_withdrawal_investor FOREIGN KEY (investor_id) REFERENCES investor(id)
);

CREATE INDEX idx_product_portfolio ON product (portfolio_id);
CREATE INDEX idx_withdrawal_investor_requested ON withdrawal_notice (investor_id, requested_at);
