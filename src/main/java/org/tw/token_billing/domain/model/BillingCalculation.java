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
    private static final int TOKENS_PER_PRICING_UNIT = 1000;
    private static final int CALCULATION_PRECISION_SCALE = 10;
    private static final int CURRENCY_SCALE = 2;

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
        BigDecimal billableTokenUnits = BigDecimal.valueOf(overageTokens)
                .divide(BigDecimal.valueOf(TOKENS_PER_PRICING_UNIT), CALCULATION_PRECISION_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalCharge = billableTokenUnits
                .multiply(overageRatePer1k)
                .setScale(CURRENCY_SCALE, RoundingMode.HALF_UP);

        return new BillingCalculation(totalTokens, tokensFromQuota, overageTokens, totalCharge);
    }
}
