package org.tw.token_billing.repository;

import org.tw.token_billing.domain.model.CustomerSubscription;

import java.time.LocalDate;
import java.util.Optional;

public interface CustomerSubscriptionRepository {
    Optional<CustomerSubscription> findActiveSubscription(String customerId, LocalDate date);
}
