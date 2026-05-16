package org.tw.token_billing.controller.dto;

import org.tw.token_billing.domain.model.Bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record BillResponse(
        UUID billId,
        String customerId,
        Integer totalTokens,
        Integer tokensFromQuota,
        Integer overageTokens,
        BigDecimal totalCharge,
        LocalDateTime calculationTimestamp
) {
    public static BillResponse from(Bill bill) {
        return new BillResponse(
                bill.id(),
                bill.customer().id(),
                bill.totalTokens(),
                bill.includedTokensUsed(),
                bill.overageTokens(),
                bill.totalCharge(),
                bill.calculatedAt()
        );
    }
}
