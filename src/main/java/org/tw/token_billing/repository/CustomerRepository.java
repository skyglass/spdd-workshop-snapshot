package org.tw.token_billing.repository;

import org.tw.token_billing.domain.model.Customer;

import java.util.Optional;

public interface CustomerRepository {
    Optional<Customer> findById(String id);

    Optional<Customer> findByIdForUpdate(String id);
}
