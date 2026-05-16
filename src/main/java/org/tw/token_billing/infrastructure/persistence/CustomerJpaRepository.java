package org.tw.token_billing.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.tw.token_billing.infrastructure.persistence.entity.CustomerPO;

import java.util.Optional;

public interface CustomerJpaRepository extends JpaRepository<CustomerPO, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CustomerPO c where c.id = :id")
    Optional<CustomerPO> findByIdForUpdate(@Param("id") String id);
}
