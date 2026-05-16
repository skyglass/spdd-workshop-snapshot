package org.tw.token_billing.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.tw.token_billing.domain.model.Customer;
import org.tw.token_billing.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import org.tw.token_billing.repository.CustomerRepository;

import java.util.Optional;

@Repository
public class JpaCustomerRepositoryAdapter implements CustomerRepository {
    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerPersistenceMapper customerMapper;

    public JpaCustomerRepositoryAdapter(
            CustomerJpaRepository customerJpaRepository,
            CustomerPersistenceMapper customerMapper
    ) {
        this.customerJpaRepository = customerJpaRepository;
        this.customerMapper = customerMapper;
    }

    @Override
    public Optional<Customer> findById(String id) {
        return customerJpaRepository.findById(id).map(customerMapper::toDomain);
    }

    @Override
    public Optional<Customer> findByIdForUpdate(String id) {
        return customerJpaRepository.findByIdForUpdate(id).map(customerMapper::toDomain);
    }
}
