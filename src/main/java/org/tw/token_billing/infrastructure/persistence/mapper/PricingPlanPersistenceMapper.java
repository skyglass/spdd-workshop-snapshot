package org.tw.token_billing.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tw.token_billing.domain.model.PricingPlan;
import org.tw.token_billing.infrastructure.persistence.entity.PricingPlanPO;

@Component
public class PricingPlanPersistenceMapper {
    public PricingPlan toDomain(PricingPlanPO po) {
        return new PricingPlan(
                po.getId(),
                po.getName(),
                po.getMonthlyQuota(),
                po.getOverageRatePer1k(),
                po.getCreatedAt()
        );
    }

    public PricingPlanPO toPO(PricingPlan pricingPlan) {
        return new PricingPlanPO(
                pricingPlan.id(),
                pricingPlan.name(),
                pricingPlan.monthlyQuota(),
                pricingPlan.overageRatePer1k(),
                pricingPlan.createdAt()
        );
    }
}
