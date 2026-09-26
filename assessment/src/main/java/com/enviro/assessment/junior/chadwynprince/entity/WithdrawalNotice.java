package com.enviro.assessment.junior.chadwynprince.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * investorId is denormalized from product.portfolio.investor: the dominant query
 * ("this investor's withdrawal history") would otherwise need a 3-table join, and
 * the value never changes after creation so there's no sync risk.
 */
@Entity
@Table(name = "withdrawal_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class WithdrawalNotice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "investor_id", nullable = false)
    private Long investorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WithdrawalType type;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WithdrawalStatus status;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "balance_after", precision = 19, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    public static WithdrawalNotice approved(Product product, Long investorId, WithdrawalType type,
                                             BigDecimal amount, BigDecimal balanceAfter) {
        return new WithdrawalNotice(null, product, investorId, type, amount, WithdrawalStatus.APPROVED,
                null, balanceAfter, LocalDateTime.now());
    }

    public static WithdrawalNotice rejected(Product product, Long investorId, WithdrawalType type,
                                             BigDecimal amount, String rejectionReason) {
        return new WithdrawalNotice(null, product, investorId, type, amount, WithdrawalStatus.REJECTED,
                rejectionReason, null, LocalDateTime.now());
    }
}
