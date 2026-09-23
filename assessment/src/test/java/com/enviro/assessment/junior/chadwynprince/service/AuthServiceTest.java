package com.enviro.assessment.junior.chadwynprince.service;

import com.enviro.assessment.junior.chadwynprince.dto.response.LoginResponse;
import com.enviro.assessment.junior.chadwynprince.entity.Investor;
import com.enviro.assessment.junior.chadwynprince.exception.InvestorNotFoundException;
import com.enviro.assessment.junior.chadwynprince.repository.InvestorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private InvestorRepository investorRepository;

    @Test
    void loginReturnsIdentityForKnownEmail() {
        var authService = new AuthService(investorRepository);
        var investor = new Investor("Thabo", "Nkosi", LocalDate.now().minusYears(45), "thabo.nkosi@example.com");
        when(investorRepository.findByEmailIgnoreCase("thabo.nkosi@example.com")).thenReturn(Optional.of(investor));

        LoginResponse response = authService.login("thabo.nkosi@example.com");

        assertThat(response.firstName()).isEqualTo("Thabo");
        assertThat(response.lastName()).isEqualTo("Nkosi");
    }

    @Test
    void loginIsCaseInsensitive() {
        var authService = new AuthService(investorRepository);
        var investor = new Investor("Thabo", "Nkosi", LocalDate.now().minusYears(45), "thabo.nkosi@example.com");
        when(investorRepository.findByEmailIgnoreCase("Thabo.Nkosi@EXAMPLE.com")).thenReturn(Optional.of(investor));

        LoginResponse response = authService.login("Thabo.Nkosi@EXAMPLE.com");

        assertThat(response.firstName()).isEqualTo("Thabo");
    }

    @Test
    void loginThrowsInvestorNotFoundForUnknownEmail() {
        var authService = new AuthService(investorRepository);
        when(investorRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("nobody@example.com"))
                .isInstanceOf(InvestorNotFoundException.class)
                .hasMessageContaining("nobody@example.com");
    }
}
