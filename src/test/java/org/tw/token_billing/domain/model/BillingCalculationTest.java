package org.tw.token_billing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class BillingCalculationTest {
    @Test
    void should_return_quota_only_calculation_when_calculate_given_usage_within_remaining_quota() {
        BillingCalculation calculation = BillingCalculation.calculate(
                30000,
                60000L,
                100000,
                new BigDecimal("0.0200")
        );

        assertThat(calculation.totalTokens()).isEqualTo(30000);
        assertThat(calculation.tokensFromQuota()).isEqualTo(30000);
        assertThat(calculation.overageTokens()).isZero();
        assertThat(calculation.totalCharge()).isEqualByComparingTo("0.00");
    }

    @Test
    void should_return_overage_calculation_when_calculate_given_usage_exceeds_remaining_quota() {
        BillingCalculation calculation = BillingCalculation.calculate(
                50000,
                80000L,
                100000,
                new BigDecimal("0.0200")
        );

        assertThat(calculation.totalTokens()).isEqualTo(50000);
        assertThat(calculation.tokensFromQuota()).isEqualTo(20000);
        assertThat(calculation.overageTokens()).isEqualTo(30000);
        assertThat(calculation.totalCharge()).isEqualByComparingTo("0.60");
    }

    @Test
    void should_return_zero_charge_when_calculate_given_zero_tokens() {
        BillingCalculation calculation = BillingCalculation.calculate(
                0,
                0L,
                100000,
                new BigDecimal("0.0200")
        );

        assertThat(calculation.totalTokens()).isZero();
        assertThat(calculation.tokensFromQuota()).isZero();
        assertThat(calculation.overageTokens()).isZero();
        assertThat(calculation.totalCharge()).isEqualByComparingTo("0.00");
    }
}
