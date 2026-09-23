package com.enviro.assessment.junior.chadwynprince.service;

import com.enviro.assessment.junior.chadwynprince.dto.response.LoginResponse;
import com.enviro.assessment.junior.chadwynprince.exception.InvestorNotFoundException;
import com.enviro.assessment.junior.chadwynprince.repository.InvestorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves an email to an investor identity — deliberately NOT real authentication.
 * There is no password, token, or session; the brief never asked for auth, and this
 * exists solely so the demo app can separate the seeded investors' data by email
 * instead of an anything-goes account switcher. Never treat this as a security boundary.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final InvestorRepository investorRepository;

    @Transactional(readOnly = true)
    public LoginResponse login(String email) {
        var investor = investorRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvestorNotFoundException(email));
        return new LoginResponse(investor.getId(), investor.getFirstName(), investor.getLastName());
    }
}
