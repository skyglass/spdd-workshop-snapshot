package org.tw.token_billing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tw.token_billing.infrastructure.persistence.entity.BillPO;

import java.time.LocalDateTime;
import java.util.UUID;

public interface BillJpaRepository extends JpaRepository<BillPO, UUID> {
    @Query("""
            select coalesce(sum(b.totalTokens), 0)
            from BillPO b
            where b.customer.id = :customerId
              and b.calculatedAt >= :startInclusive
              and b.calculatedAt < :endExclusive
            """)
    Long sumTotalTokensForCustomerBetween(
            @Param("customerId") String customerId,
            @Param("startInclusive") LocalDateTime startInclusive,
            @Param("endExclusive") LocalDateTime endExclusive
    );
}
