package org.tw.token_billing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_plans")
public class PricingPlanPO {
    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "monthly_quota", nullable = false)
    private Integer monthlyQuota;

    @Column(name = "overage_rate_per_1k", nullable = false, precision = 10, scale = 4)
    private BigDecimal overageRatePer1k;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected PricingPlanPO() {
    }

    public PricingPlanPO(
            String id,
            String name,
            Integer monthlyQuota,
            BigDecimal overageRatePer1k,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.name = name;
        this.monthlyQuota = monthlyQuota;
        this.overageRatePer1k = overageRatePer1k;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getMonthlyQuota() {
        return monthlyQuota;
    }

    public BigDecimal getOverageRatePer1k() {
        return overageRatePer1k;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
