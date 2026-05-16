package org.tw.token_billing.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public record PricingPlan(
        String id,
        String name,
        Integer monthlyQuota,
        BigDecimal overageRatePer1k,
        LocalDateTime createdAt
) {
    public PricingPlan {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(monthlyQuota, "monthlyQuota must not be null");
        Objects.requireNonNull(overageRatePer1k, "overageRatePer1k must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
