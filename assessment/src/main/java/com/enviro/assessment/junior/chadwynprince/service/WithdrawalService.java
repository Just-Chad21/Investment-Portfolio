package com.enviro.assessment.junior.chadwynprince.service;

import com.enviro.assessment.junior.chadwynprince.dto.request.WithdrawalRequest;
import com.enviro.assessment.junior.chadwynprince.dto.response.WithdrawalResponse;
import com.enviro.assessment.junior.chadwynprince.entity.Investor;
import com.enviro.assessment.junior.chadwynprince.entity.Product;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalNotice;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalStatus;
import com.enviro.assessment.junior.chadwynprince.entity.WithdrawalType;
import com.enviro.assessment.junior.chadwynprince.exception.InvalidDateRangeException;
import com.enviro.assessment.junior.chadwynprince.exception.InvestorNotFoundException;
import com.enviro.assessment.junior.chadwynprince.exception.ProductNotFoundException;
import com.enviro.assessment.junior.chadwynprince.mapper.WithdrawalCsvWriter;
import com.enviro.assessment.junior.chadwynprince.mapper.WithdrawalMapper;
import com.enviro.assessment.junior.chadwynprince.repository.InvestorRepository;
import com.enviro.assessment.junior.chadwynprince.repository.ProductRepository;
import com.enviro.assessment.junior.chadwynprince.repository.WithdrawalNoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * A rejection is a valid, recorded domain outcome here, not a thrown exception — see
 * Phase 5's "Design Reconciliation": submitWithdrawal always returns a WithdrawalResponse
 * (APPROVED or REJECTED); exceptions are reserved for requests that never reach a rule
 * evaluation at all (unknown investor/product).
 */
@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private static final int MIN_RETIREMENT_AGE = 65;
    private static final BigDecimal MAX_WITHDRAWAL_RATIO = new BigDecimal("0.90");

    private final InvestorRepository investorRepository;
    private final ProductRepository productRepository;
    private final WithdrawalNoticeRepository withdrawalNoticeRepository;
    private final WithdrawalMapper withdrawalMapper;
    private final WithdrawalCsvWriter withdrawalCsvWriter;

    @Transactional
    public WithdrawalResponse submitWithdrawal(Long investorId, WithdrawalRequest request) {
        var investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new InvestorNotFoundException(investorId));
        var product = productRepository.findByIdAndPortfolio_Investor_Id(request.productId(), investorId)
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));

        WithdrawalType type = product.getProductType().getCategory();
        WithdrawalNotice notice = evaluateRules(investor, product, request)
                .map(reason -> WithdrawalNotice.rejected(product, investorId, type, request.amount(), reason))
                .orElseGet(() -> approve(product, investorId, type, request));

        withdrawalNoticeRepository.save(notice);
        return withdrawalMapper.toResponse(notice);
    }

    @Transactional(readOnly = true)
    public List<WithdrawalResponse> getHistory(Long investorId, WithdrawalType type, WithdrawalStatus status,
                                                LocalDate from, LocalDate to) {
        return findFiltered(investorId, type, status, from, to).stream()
                .map(withdrawalMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CsvExport exportCsv(Long investorId, WithdrawalType type, WithdrawalStatus status,
                                LocalDate from, LocalDate to) {
        var investor = investorRepository.findById(investorId)
                .orElseThrow(() -> new InvestorNotFoundException(investorId));
        String csv = withdrawalCsvWriter.write(findFiltered(investorId, type, status, from, to));
        return new CsvExport(csvFilename(investor), csv);
    }

    // e.g. "Thabo_Nkosi_withdrawalhistory_2026-09-23.csv" — sanitized so a name with spaces,
    // apostrophes, etc. (e.g. "Grace van der Merwe") can't produce a malformed header value.
    private String csvFilename(Investor investor) {
        String namePart = (investor.getFirstName() + "_" + investor.getLastName())
                .replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return "%s_withdrawalhistory_%s.csv".formatted(namePart, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE));
    }

    public record CsvExport(String filename, String content) {
    }

    private List<WithdrawalNotice> findFiltered(Long investorId, WithdrawalType type, WithdrawalStatus status,
                                                 LocalDate from, LocalDate to) {
        if (!investorRepository.existsById(investorId)) {
            throw new InvestorNotFoundException(investorId);
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException();
        }
        // requestedAt is a LocalDateTime; treat "to" as inclusive of the whole day by using
        // an exclusive upper bound at the start of the following day.
        LocalDateTime fromInclusive = from == null ? null : from.atStartOfDay();
        LocalDateTime toExclusive = to == null ? null : to.plusDays(1).atStartOfDay();
        return withdrawalNoticeRepository.findByFilters(investorId, type, status, fromInclusive, toExclusive);
    }

    private WithdrawalNotice approve(Product product, Long investorId, WithdrawalType type, WithdrawalRequest request) {
        product.debit(request.amount());
        productRepository.save(product);
        return WithdrawalNotice.approved(product, investorId, type, request.amount(), product.getBalance());
    }

    // Rule order is deliberate: the balance check is evaluated before the 90% check even
    // though 90% of any positive balance is always <= the balance itself (so a request that
    // fails the balance check would always fail the 90% check too). Checking balance first
    // just gives a more specific, useful message when the request wildly overshoots, instead
    // of the generic "exceeds 90%" message. A request for exactly 100% of the balance passes
    // the balance check but is still rejected by the 90% check — withdrawing the full balance
    // is never allowed.
    //
    // The retirement check keys off product.getProductType().isRetirement() — a permanent
    // property of the product — rather than a client-supplied request field, so a withdrawal
    // can't dodge the age rule by simply not declaring itself RETIREMENT.
    private Optional<String> evaluateRules(Investor investor, Product product, WithdrawalRequest request) {
        if (product.getProductType().isRetirement() && investor.getAge() <= MIN_RETIREMENT_AGE) {
            return Optional.of("Retirement withdrawals require age > 65 (investor is %d)".formatted(investor.getAge()));
        }
        if (request.amount().compareTo(product.getBalance()) > 0) {
            return Optional.of("Withdrawal amount exceeds available balance");
        }
        BigDecimal maxAllowed = product.getBalance().multiply(MAX_WITHDRAWAL_RATIO);
        if (request.amount().compareTo(maxAllowed) > 0) {
            return Optional.of("Withdrawal amount exceeds 90% of available balance");
        }
        return Optional.empty();
    }
}
