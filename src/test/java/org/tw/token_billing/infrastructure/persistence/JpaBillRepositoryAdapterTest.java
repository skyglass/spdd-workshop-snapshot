package org.tw.token_billing.infrastructure.persistence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tw.token_billing.domain.model.Bill;
import org.tw.token_billing.domain.model.Customer;
import org.tw.token_billing.infrastructure.persistence.entity.BillPO;
import org.tw.token_billing.infrastructure.persistence.entity.CustomerPO;
import org.tw.token_billing.infrastructure.persistence.mapper.BillPersistenceMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaBillRepositoryAdapterTest {
    @Mock
    private BillJpaRepository billJpaRepository;

    @Mock
    private BillPersistenceMapper billMapper;

    private JpaBillRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaBillRepositoryAdapter(billJpaRepository, billMapper);
    }

    @Test
    void should_return_zero_when_sum_total_tokens_given_repository_returns_null() {
        LocalDateTime start = LocalDateTime.of(2026, 5, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2026, 6, 1, 0, 0);
        when(billJpaRepository.sumTotalTokensForCustomerBetween("CUST-001", start, end)).thenReturn(null);

        Long total = adapter.sumTotalTokensForCustomerBetween("CUST-001", start, end);

        assertThat(total).isZero();
    }

    @Test
    void should_return_saved_domain_bill_when_save_given_domain_bill() {
        Bill inputBill = bill("11111111-1111-1111-1111-111111111111");
        Bill savedBill = bill("22222222-2222-2222-2222-222222222222");
        BillPO inputPo = billPo("11111111-1111-1111-1111-111111111111");
        BillPO savedPo = billPo("22222222-2222-2222-2222-222222222222");
        when(billMapper.toPO(inputBill)).thenReturn(inputPo);
        when(billJpaRepository.save(inputPo)).thenReturn(savedPo);
        when(billMapper.toDomain(savedPo)).thenReturn(savedBill);

        Bill result = adapter.save(inputBill);

        assertThat(result).isSameAs(savedBill);
        InOrder inOrder = inOrder(billMapper, billJpaRepository);
        inOrder.verify(billMapper).toPO(inputBill);
        inOrder.verify(billJpaRepository).save(inputPo);
        inOrder.verify(billMapper).toDomain(savedPo);
    }

    private Bill bill(String id) {
        Customer customer = new Customer("CUST-001", "Acme Corp", LocalDateTime.of(2026, 1, 1, 0, 0));
        return new Bill(
                UUID.fromString(id),
                customer,
                1000,
                500,
                1500,
                1500,
                0,
                new BigDecimal("0.00"),
                LocalDateTime.of(2026, 5, 16, 12, 0)
        );
    }

    private BillPO billPo(String id) {
        CustomerPO customer = new CustomerPO("CUST-001", "Acme Corp", LocalDateTime.of(2026, 1, 1, 0, 0));
        return new BillPO(
                UUID.fromString(id),
                customer,
                1000,
                500,
                1500,
                1500,
                0,
                new BigDecimal("0.00"),
                LocalDateTime.of(2026, 5, 16, 12, 0)
        );
    }
}
