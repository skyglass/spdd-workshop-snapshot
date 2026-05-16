package org.tw.token_billing.controller.dto;

import org.junit.jupiter.api.Test;
import org.tw.token_billing.domain.model.Bill;
import org.tw.token_billing.domain.model.Customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BillResponseTest {
    @Test
    void should_return_response_fields_when_mapping_given_domain_bill() {
        UUID billId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        LocalDateTime calculatedAt = LocalDateTime.of(2026, 5, 16, 12, 0);
        Customer customer = new Customer("CUST-001", "Acme Corp", LocalDateTime.of(2026, 1, 1, 0, 0));
        Bill bill = new Bill(
                billId,
                customer,
                30000,
                20000,
                50000,
                20000,
                30000,
                new BigDecimal("0.60"),
                calculatedAt
        );

        BillResponse response = BillResponse.from(bill);

        assertThat(response.billId()).isEqualTo(billId);
        assertThat(response.customerId()).isEqualTo("CUST-001");
        assertThat(response.totalTokens()).isEqualTo(50000);
        assertThat(response.tokensFromQuota()).isEqualTo(20000);
        assertThat(response.overageTokens()).isEqualTo(30000);
        assertThat(response.totalCharge()).isEqualByComparingTo("0.60");
        assertThat(response.calculationTimestamp()).isEqualTo(calculatedAt);
    }
}
