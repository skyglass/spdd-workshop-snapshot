package org.tw.token_billing.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record BillingCalculation(
        Integer totalTokens,
        Integer tokensFromQuota,
        Integer overageTokens,
        BigDecimal totalCharge
) {
    public BillingCalculation {
        Objects.requireNonNull(totalTokens, "totalTokens must not be null");
        Objects.requireNonNull(tokensFromQuota, "tokensFromQuota must not be null");
        Objects.requireNonNull(overageTokens, "overageTokens must not be null");
        Objects.requireNonNull(totalCharge, "totalCharge must not be null");
    }

    public static BillingCalculation calculate(
            Integer totalTokens,
            long currentMonthUsage,
            Integer monthlyQuota,
            BigDecimal overageRatePer1k
    ) {
        Objects.requireNonNull(totalTokens, "totalTokens must not be null");
        Objects.requireNonNull(monthlyQuota, "monthlyQuota must not be null");
        Objects.requireNonNull(overageRatePer1k, "overageRatePer1k must not be null");

        long remainingQuota = Math.max(monthlyQuota.longValue() - currentMonthUsage, 0L);
        int tokensFromQuota = (int) Math.min(totalTokens.longValue(), remainingQuota);
        int overageTokens = totalTokens - tokensFromQuota;
        BigDecimal totalCharge = BigDecimal.valueOf(overageTokens)
                .multiply(overageRatePer1k)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

        return new BillingCalculation(totalTokens, tokensFromQuota, overageTokens, totalCharge);
    }
}
