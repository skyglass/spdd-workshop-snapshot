package org.tw.token_billing.domain.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record CustomerSubscription(
        UUID id,
        Customer customer,
        PricingPlan pricingPlan,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        LocalDateTime createdAt
) {
    public CustomerSubscription {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(customer, "customer must not be null");
        Objects.requireNonNull(pricingPlan, "pricingPlan must not be null");
        Objects.requireNonNull(effectiveFrom, "effectiveFrom must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public boolean isActiveOn(LocalDate date) {
        Objects.requireNonNull(date, "date must not be null");
        return !effectiveFrom.isAfter(date) && (effectiveTo == null || !effectiveTo.isBefore(date));
    }
}
