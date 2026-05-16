package org.tw.token_billing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bills")
public class BillPO {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerPO customer;

    @Column(name = "prompt_tokens", nullable = false)
    private Integer promptTokens;

    @Column(name = "completion_tokens", nullable = false)
    private Integer completionTokens;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    @Column(name = "included_tokens_used", nullable = false)
    private Integer includedTokensUsed;

    @Column(name = "overage_tokens", nullable = false)
    private Integer overageTokens;

    @Column(name = "total_charge", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalCharge;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    protected BillPO() {
    }

    public BillPO(
            UUID id,
            CustomerPO customer,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            Integer includedTokensUsed,
            Integer overageTokens,
            BigDecimal totalCharge,
            LocalDateTime calculatedAt
    ) {
        this.id = id;
        this.customer = customer;
        this.promptTokens = promptTokens;
        this.completionTokens = completionTokens;
        this.totalTokens = totalTokens;
        this.includedTokensUsed = includedTokensUsed;
        this.overageTokens = overageTokens;
        this.totalCharge = totalCharge;
        this.calculatedAt = calculatedAt;
    }

    public UUID getId() {
        return id;
    }

    public CustomerPO getCustomer() {
        return customer;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public Integer getIncludedTokensUsed() {
        return includedTokensUsed;
    }

    public Integer getOverageTokens() {
        return overageTokens;
    }

    public BigDecimal getTotalCharge() {
        return totalCharge;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }
}
