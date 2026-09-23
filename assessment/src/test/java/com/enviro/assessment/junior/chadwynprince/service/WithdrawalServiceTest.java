package com.enviro.assessment.junior.chadwynprince.service;

import com.enviro.assessment.junior.chadwynprince.dto.request.WithdrawalRequest;
import com.enviro.assessment.junior.chadwynprince.dto.response.WithdrawalResponse;
import com.enviro.assessment.junior.chadwynprince.entity.Investor;
import com.enviro.assessment.junior.chadwynprince.entity.Portfolio;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    private static final Long INVESTOR_ID = 1L;
    private static final Long PRODUCT_ID = 10L;

    @Mock
    private InvestorRepository investorRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WithdrawalNoticeRepository withdrawalNoticeRepository;

    private WithdrawalService withdrawalService;
    private Portfolio portfolio;

    @BeforeEach
    void setUp() {
        withdrawalService = new WithdrawalService(
                investorRepository, productRepository, withdrawalNoticeRepository,
                new WithdrawalMapper(), new WithdrawalCsvWriter());

        lenient().when(withdrawalNoticeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(productRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var investorForPortfolio = new Investor("Placeholder", "Placeholder", LocalDate.now().minusYears(50), "x@example.com");
        portfolio = new Portfolio(investorForPortfolio, "PF-TEST");
    }

    private Investor investorAged(int age) {
        return new Investor("Test", "Investor", LocalDate.now().minusYears(age), "test@example.com");
    }

    private Product productWithBalance(String balance) {
        return new Product(portfolio, "Test Product", new BigDecimal(balance));
    }

    private void givenInvestorAndProduct(Investor investor, Product product) {
        when(investorRepository.findById(INVESTOR_ID)).thenReturn(Optional.of(investor));
        when(productRepository.findByIdAndPortfolio_Investor_Id(PRODUCT_ID, INVESTOR_ID))
                .thenReturn(Optional.of(product));
    }

    @Test
    void standardWithdrawalWithinLimitsIsApproved() {
        givenInvestorAndProduct(investorAged(30), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("5000.00")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(response.rejectionReason()).isNull();
        assertThat(response.balanceAfter()).isEqualByComparingTo("5000.00");
    }

    @Test
    void standardWithdrawalIgnoresAgeRule() {
        givenInvestorAndProduct(investorAged(30), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("1000.00")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.APPROVED);
    }

    @Test
    void retirementWithdrawalRejectedWhenAgeExactly65() {
        givenInvestorAndProduct(investorAged(65), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.RETIREMENT, new BigDecimal("1000.00")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(response.rejectionReason()).contains("age > 65").contains("65");
        assertThat(response.balanceAfter()).isNull();
    }

    @Test
    void retirementWithdrawalApprovedWhenAgeAbove65AndWithinLimits() {
        givenInvestorAndProduct(investorAged(66), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.RETIREMENT, new BigDecimal("5000.00")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.APPROVED);
    }

    @Test
    void withdrawalRejectedWhenAmountExceedsBalance() {
        givenInvestorAndProduct(investorAged(30), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("10000.01")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Withdrawal amount exceeds available balance");
    }

    @Test
    void withdrawalRejectedWhenAmountEqualsBalanceExactly() {
        // 100% of any positive balance also exceeds 90% of it, so this is rejected by the
        // 90% rule, not the balance rule — see WithdrawalService's rule-ordering comment.
        givenInvestorAndProduct(investorAged(30), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("10000.00")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Withdrawal amount exceeds 90% of available balance");
    }

    @Test
    void withdrawalRejectedWhenAmountJustOver90Percent() {
        givenInvestorAndProduct(investorAged(30), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("9000.01")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Withdrawal amount exceeds 90% of available balance");
    }

    @Test
    void withdrawalApprovedWhenAmountExactly90Percent() {
        givenInvestorAndProduct(investorAged(30), productWithBalance("10000.00"));

        WithdrawalResponse response = withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("9000.00")));

        assertThat(response.status()).isEqualTo(WithdrawalStatus.APPROVED);
        assertThat(response.balanceAfter()).isEqualByComparingTo("1000.00");
    }

    @Test
    void approvedWithdrawalDebitsAndPersistsProductBalance() {
        var product = productWithBalance("10000.00");
        givenInvestorAndProduct(investorAged(30), product);

        withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("4000.00")));

        assertThat(product.getBalance()).isEqualByComparingTo("6000.00");
        verify(productRepository).save(product);
    }

    @Test
    void rejectedWithdrawalDoesNotDebitProductBalance() {
        givenInvestorAndProduct(investorAged(65), productWithBalance("10000.00"));

        withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.RETIREMENT, new BigDecimal("1000.00")));

        verify(productRepository, never()).save(any());
    }

    @Test
    void throwsInvestorNotFoundWhenInvestorMissing() {
        when(investorRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("100.00"))))
                .isInstanceOf(InvestorNotFoundException.class);
    }

    @Test
    void throwsProductNotFoundWhenProductMissingOrNotOwnedByInvestor() {
        when(investorRepository.findById(INVESTOR_ID)).thenReturn(Optional.of(investorAged(30)));
        when(productRepository.findByIdAndPortfolio_Investor_Id(PRODUCT_ID, INVESTOR_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> withdrawalService.submitWithdrawal(INVESTOR_ID,
                new WithdrawalRequest(PRODUCT_ID, WithdrawalType.STANDARD, new BigDecimal("100.00"))))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void getHistoryReturnsMappedResponses() {
        when(investorRepository.existsById(INVESTOR_ID)).thenReturn(true);
        var product = productWithBalance("10000.00");
        var notice = WithdrawalNotice.approved(product, INVESTOR_ID, WithdrawalType.STANDARD,
                new BigDecimal("1000.00"), new BigDecimal("9000.00"));
        when(withdrawalNoticeRepository.findByFilters(eq(INVESTOR_ID), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(List.of(notice));

        List<WithdrawalResponse> history = withdrawalService.getHistory(INVESTOR_ID, null, null, null, null);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo(WithdrawalStatus.APPROVED);
    }

    @Test
    void getHistoryConvertsDateRangeToInclusiveDayBounds() {
        when(investorRepository.existsById(INVESTOR_ID)).thenReturn(true);
        when(withdrawalNoticeRepository.findByFilters(any(), any(), any(), any(), any())).thenReturn(List.of());

        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 1, 31);
        withdrawalService.getHistory(INVESTOR_ID, null, null, from, to);

        ArgumentCaptor<LocalDateTime> fromCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> toCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(withdrawalNoticeRepository).findByFilters(eq(INVESTOR_ID), isNull(), isNull(),
                fromCaptor.capture(), toCaptor.capture());

        assertThat(fromCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 1, 1, 0, 0));
        // Exclusive upper bound at the start of the day *after* "to", so the whole "to" day is included.
        assertThat(toCaptor.getValue()).isEqualTo(LocalDateTime.of(2026, 2, 1, 0, 0));
    }

    @Test
    void getHistoryThrowsInvestorNotFoundWhenInvestorMissing() {
        when(investorRepository.existsById(INVESTOR_ID)).thenReturn(false);

        assertThatThrownBy(() -> withdrawalService.getHistory(INVESTOR_ID, null, null, null, null))
                .isInstanceOf(InvestorNotFoundException.class);
    }

    @Test
    void getHistoryThrowsInvalidDateRangeWhenFromAfterTo() {
        when(investorRepository.existsById(INVESTOR_ID)).thenReturn(true);

        assertThatThrownBy(() -> withdrawalService.getHistory(INVESTOR_ID, null, null,
                LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void exportCsvReturnsHeaderOnlyWhenNoWithdrawalsMatch() {
        when(investorRepository.existsById(INVESTOR_ID)).thenReturn(true);
        when(withdrawalNoticeRepository.findByFilters(any(), any(), any(), any(), any())).thenReturn(List.of());

        String csv = withdrawalService.exportCsv(INVESTOR_ID, null, null, null, null);

        assertThat(csv).isEqualTo("id,productName,type,amount,status,rejectionReason,balanceAfter,requestedAt\n");
    }

    @Test
    void exportCsvEscapesRejectionReasonContainingComma() {
        when(investorRepository.existsById(INVESTOR_ID)).thenReturn(true);
        var product = productWithBalance("10000.00");
        var notice = WithdrawalNotice.rejected(product, INVESTOR_ID, WithdrawalType.RETIREMENT,
                new BigDecimal("1000.00"), "age > 65 required, investor is 60");
        when(withdrawalNoticeRepository.findByFilters(any(), any(), any(), any(), any())).thenReturn(List.of(notice));

        String csv = withdrawalService.exportCsv(INVESTOR_ID, null, null, null, null);

        assertThat(csv).contains("\"age > 65 required, investor is 60\"");
    }
}
