package org.tw.token_billing.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record Bill(
        UUID id,
        Customer customer,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        Integer includedTokensUsed,
        Integer overageTokens,
        BigDecimal totalCharge,
        LocalDateTime calculatedAt
) {
    public Bill {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(promptTokens, "promptTokens must not be null");
        Objects.requireNonNull(completionTokens, "completionTokens must not be null");
        Objects.requireNonNull(totalTokens, "totalTokens must not be null");
        Objects.requireNonNull(includedTokensUsed, "includedTokensUsed must not be null");
        Objects.requireNonNull(overageTokens, "overageTokens must not be null");
        Objects.requireNonNull(totalCharge, "totalCharge must not be null");
        Objects.requireNonNull(calculatedAt, "calculatedAt must not be null");
    }

    public static Bill create(
            Customer customer,
            Integer promptTokens,
            Integer completionTokens,
            BillingCalculation calculation,
            LocalDateTime calculatedAt
    ) {
        Objects.requireNonNull(calculation, "calculation must not be null");
        return new Bill(
                UUID.randomUUID(),
                customer,
                promptTokens,
                completionTokens,
                calculation.totalTokens(),
                calculation.tokensFromQuota(),
                calculation.overageTokens(),
                calculation.totalCharge(),
                calculatedAt
        );
    }
}
