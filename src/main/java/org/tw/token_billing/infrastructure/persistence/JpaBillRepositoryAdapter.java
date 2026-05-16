package org.tw.token_billing.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import org.tw.token_billing.domain.model.Bill;
import org.tw.token_billing.infrastructure.persistence.mapper.BillPersistenceMapper;
import org.tw.token_billing.repository.BillRepository;

import java.time.LocalDateTime;

@Repository
public class JpaBillRepositoryAdapter implements BillRepository {
    private final BillJpaRepository billJpaRepository;
    private final BillPersistenceMapper billMapper;

    public JpaBillRepositoryAdapter(BillJpaRepository billJpaRepository, BillPersistenceMapper billMapper) {
        this.billJpaRepository = billJpaRepository;
        this.billMapper = billMapper;
    }

    @Override
    public Long sumTotalTokensForCustomerBetween(
            String customerId,
            LocalDateTime startInclusive,
            LocalDateTime endExclusive
    ) {
        Long total = billJpaRepository.sumTotalTokensForCustomerBetween(
                customerId,
                startInclusive,
                endExclusive
        );
        return total == null ? 0L : total;
    }

    @Override
    public Bill save(Bill bill) {
        return billMapper.toDomain(billJpaRepository.save(billMapper.toPO(bill)));
    }
}
