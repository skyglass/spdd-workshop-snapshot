package org.tw.token_billing.repository;

import org.tw.token_billing.domain.model.Bill;

import java.time.LocalDateTime;

public interface BillRepository {
    Long sumTotalTokensForCustomerBetween(
            String customerId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    );

    Bill save(Bill bill);
}
