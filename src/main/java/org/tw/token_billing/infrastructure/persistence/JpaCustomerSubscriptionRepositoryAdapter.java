package org.tw.token_billing.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.tw.token_billing.domain.model.CustomerSubscription;
import org.tw.token_billing.infrastructure.persistence.mapper.CustomerSubscriptionPersistenceMapper;
import org.tw.token_billing.repository.CustomerSubscriptionRepository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public class JpaCustomerSubscriptionRepositoryAdapter implements CustomerSubscriptionRepository {
    private final SpringDataCustomerSubscriptionRepository customerSubscriptionRepository;
    private final CustomerSubscriptionPersistenceMapper customerSubscriptionMapper;

    public JpaCustomerSubscriptionRepositoryAdapter(
            SpringDataCustomerSubscriptionRepository customerSubscriptionRepository,
            CustomerSubscriptionPersistenceMapper customerSubscriptionMapper
    ) {
        this.customerSubscriptionRepository = customerSubscriptionRepository;
        this.customerSubscriptionMapper = customerSubscriptionMapper;
    }

    @Override
    public Optional<CustomerSubscription> findActiveSubscription(String customerId, LocalDate date) {
        return customerSubscriptionRepository.findActiveSubscription(customerId, date)
                .map(customerSubscriptionMapper::toDomain);
    }
}
