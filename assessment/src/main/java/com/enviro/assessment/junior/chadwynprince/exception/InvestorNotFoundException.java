package com.enviro.assessment.junior.chadwynprince.exception;

// Mapped to 404 by GlobalExceptionHandler (Epic D).
public class InvestorNotFoundException extends RuntimeException {

    public InvestorNotFoundException(Long investorId) {
        super("No investor found with id " + investorId);
    }

    public InvestorNotFoundException(String email) {
        super("No investor found with email " + email);
    }
}
