package org.tw.token_billing.exception;

import org.springframework.http.HttpStatus;

public class CustomerNotFoundException extends BusinessException {
    public CustomerNotFoundException() {
        super("CUSTOMER_NOT_FOUND", "Customer not found", HttpStatus.NOT_FOUND);
    }
}
