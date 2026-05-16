package org.tw.token_billing.repository;

import org.tw.token_billing.domain.model.CustomerSubscription;

import java.time.LocalDate;
import java.util.List;

public interface CustomerSubscriptionRepository {
    List<CustomerSubscription> findActiveSubscriptions(String customerId, LocalDate date);
}
