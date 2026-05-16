package org.tw.token_billing.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tw.token_billing.domain.model.CustomerSubscription;
import org.tw.token_billing.infrastructure.persistence.entity.CustomerSubscriptionPO;

@Component
public class CustomerSubscriptionPersistenceMapper {
    private final CustomerPersistenceMapper customerMapper;
    private final PricingPlanPersistenceMapper pricingPlanMapper;

    public CustomerSubscriptionPersistenceMapper(
            CustomerPersistenceMapper customerMapper,
            PricingPlanPersistenceMapper pricingPlanMapper
    ) {
        this.customerMapper = customerMapper;
        this.pricingPlanMapper = pricingPlanMapper;
    }

    public CustomerSubscription toDomain(CustomerSubscriptionPO po) {
        return new CustomerSubscription(
                po.getId(),
                customerMapper.toDomain(po.getCustomer()),
                pricingPlanMapper.toDomain(po.getPricingPlan()),
                po.getEffectiveFrom(),
                po.getEffectiveTo(),
                po.getCreatedAt()
        );
    }
}
