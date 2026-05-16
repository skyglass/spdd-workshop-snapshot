package org.tw.token_billing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "customer_subscriptions")
public class CustomerSubscriptionPO {
    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private CustomerPO customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private PricingPlanPO pricingPlan;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected CustomerSubscriptionPO() {
    }

    public CustomerSubscriptionPO(
            UUID id,
            CustomerPO customer,
            PricingPlanPO pricingPlan,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.customer = customer;
        this.pricingPlan = pricingPlan;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public CustomerPO getCustomer() {
        return customer;
    }

    public PricingPlanPO getPricingPlan() {
        return pricingPlan;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
