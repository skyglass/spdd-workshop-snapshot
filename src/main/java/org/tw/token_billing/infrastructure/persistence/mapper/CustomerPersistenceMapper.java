package org.tw.token_billing.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;
import org.tw.token_billing.domain.model.Customer;
import org.tw.token_billing.infrastructure.persistence.entity.CustomerPO;

@Component
public class CustomerPersistenceMapper {
    public Customer toDomain(CustomerPO po) {
        return new Customer(po.getId(), po.getName(), po.getCreatedAt());
    }

    public CustomerPO toPO(Customer customer) {
        return new CustomerPO(customer.id(), customer.name(), customer.createdAt());
    }
}
