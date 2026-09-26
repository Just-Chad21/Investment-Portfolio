package com.enviro.assessment.junior.chadwynprince.exception;

// Also thrown when the product exists but belongs to a different investor's portfolio —
// returning 404 either way avoids confirming another investor's product ID exists.
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long productId) {
        super("No product found with id " + productId + " for this investor");
    }
}
