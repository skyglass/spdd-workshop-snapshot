package org.tw.token_billing.exception;

import org.springframework.http.HttpStatus;

public class NoActiveSubscriptionException extends BusinessException {
    public NoActiveSubscriptionException(String customerId) {
        super("ACTIVE_SUBSCRIPTION_NOT_FOUND", "Active subscription not found", HttpStatus.CONFLICT);
    }
}
