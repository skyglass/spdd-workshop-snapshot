package org.tw.token_billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tw.token_billing.infrastructure.persistence.entity.CustomerSubscriptionPO;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CustomerSubscriptionJpaRepository extends JpaRepository<CustomerSubscriptionPO, UUID> {
    @Query("""
            select cs
            from CustomerSubscriptionPO cs
            join fetch cs.customer
            join fetch cs.pricingPlan
            where cs.customer.id = :customerId
              and cs.effectiveFrom <= :date
              and (cs.effectiveTo is null or cs.effectiveTo >= :date)
            order by cs.effectiveFrom desc
            """)
    List<CustomerSubscriptionPO> findActiveSubscriptions(
            @Param("customerId") String customerId,
            @Param("date") LocalDate date
    );
}
