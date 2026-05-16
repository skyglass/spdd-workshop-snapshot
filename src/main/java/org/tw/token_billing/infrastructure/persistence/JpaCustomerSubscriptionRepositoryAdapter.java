package org.tw.token_billing.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.tw.token_billing.domain.model.CustomerSubscription;
import org.tw.token_billing.infrastructure.persistence.mapper.CustomerSubscriptionPersistenceMapper;
import org.tw.token_billing.repository.CustomerSubscriptionRepository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class JpaCustomerSubscriptionRepositoryAdapter implements CustomerSubscriptionRepository {
    private final CustomerSubscriptionJpaRepository customerSubscriptionJpaRepository;
    private final CustomerSubscriptionPersistenceMapper customerSubscriptionMapper;

    public JpaCustomerSubscriptionRepositoryAdapter(
            CustomerSubscriptionJpaRepository customerSubscriptionJpaRepository,
            CustomerSubscriptionPersistenceMapper customerSubscriptionMapper
    ) {
        this.customerSubscriptionJpaRepository = customerSubscriptionJpaRepository;
        this.customerSubscriptionMapper = customerSubscriptionMapper;
    }

    @Override
    public List<CustomerSubscription> findActiveSubscriptions(String customerId, LocalDate date) {
        return customerSubscriptionJpaRepository.findActiveSubscriptions(customerId, date)
                .stream()
                .map(customerSubscriptionMapper::toDomain)
                .toList();
    }
}
