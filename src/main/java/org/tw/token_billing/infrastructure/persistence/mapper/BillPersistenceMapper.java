package org.tw.token_billing.infrastructure.persistence.mapper;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.tw.token_billing.domain.model.Bill;
import org.tw.token_billing.infrastructure.persistence.entity.BillPO;
import org.tw.token_billing.infrastructure.persistence.entity.CustomerPO;

@Component
public class BillPersistenceMapper {
    private final CustomerPersistenceMapper customerMapper;
    private final EntityManager entityManager;

    public BillPersistenceMapper(CustomerPersistenceMapper customerMapper, EntityManager entityManager) {
        this.customerMapper = customerMapper;
        this.entityManager = entityManager;
    }

    public Bill toDomain(BillPO po) {
        return new Bill(
                po.getId(),
                customerMapper.toDomain(po.getCustomer()),
                po.getPromptTokens(),
                po.getCompletionTokens(),
                po.getTotalTokens(),
                po.getIncludedTokensUsed(),
                po.getOverageTokens(),
                po.getTotalCharge(),
                po.getCalculatedAt()
        );
    }

    public BillPO toPO(Bill bill) {
        CustomerPO customerReference = entityManager.getReference(CustomerPO.class, bill.customer().id());
        return new BillPO(
                bill.id(),
                customerReference,
                bill.promptTokens(),
                bill.completionTokens(),
                bill.totalTokens(),
                bill.includedTokensUsed(),
                bill.overageTokens(),
                bill.totalCharge(),
                bill.calculatedAt()
        );
    }
}
