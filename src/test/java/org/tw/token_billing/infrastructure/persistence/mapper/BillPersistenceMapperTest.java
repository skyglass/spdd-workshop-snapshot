package org.tw.token_billing.infrastructure.persistence.mapper;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tw.token_billing.domain.model.Bill;
import org.tw.token_billing.domain.model.Customer;
import org.tw.token_billing.infrastructure.persistence.entity.BillPO;
import org.tw.token_billing.infrastructure.persistence.entity.CustomerPO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillPersistenceMapperTest {
    private static final UUID BILL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 0, 0);
    private static final LocalDateTime CALCULATED_AT = LocalDateTime.of(2026, 5, 16, 12, 0);

    @Mock
    private EntityManager entityManager;

    private BillPersistenceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new BillPersistenceMapper(new CustomerPersistenceMapper(), entityManager);
    }

    @Test
    void should_return_domain_bill_when_mapping_given_bill_po() {
        CustomerPO customerPo = new CustomerPO("CUST-001", "Acme Corp", CREATED_AT);
        BillPO po = new BillPO(
                BILL_ID,
                customerPo,
                30000,
                20000,
                50000,
                20000,
                30000,
                new BigDecimal("0.60"),
                CALCULATED_AT
        );

        Bill bill = mapper.toDomain(po);

        assertThat(bill.id()).isEqualTo(BILL_ID);
        assertThat(bill.customer().id()).isEqualTo("CUST-001");
        assertThat(bill.promptTokens()).isEqualTo(30000);
        assertThat(bill.completionTokens()).isEqualTo(20000);
        assertThat(bill.totalTokens()).isEqualTo(50000);
        assertThat(bill.includedTokensUsed()).isEqualTo(20000);
        assertThat(bill.overageTokens()).isEqualTo(30000);
        assertThat(bill.totalCharge()).isEqualByComparingTo("0.60");
        assertThat(bill.calculatedAt()).isEqualTo(CALCULATED_AT);
    }

    @Test
    void should_return_bill_po_when_mapping_given_domain_bill() {
        CustomerPO customerReference = new CustomerPO("CUST-001", "Acme Corp", CREATED_AT);
        when(entityManager.getReference(CustomerPO.class, "CUST-001")).thenReturn(customerReference);
        Customer customer = new Customer("CUST-001", "Acme Corp", CREATED_AT);
        Bill bill = new Bill(
                BILL_ID,
                customer,
                1000,
                500,
                1500,
                1500,
                0,
                new BigDecimal("0.00"),
                CALCULATED_AT
        );

        BillPO po = mapper.toPO(bill);

        assertThat(po.getId()).isEqualTo(BILL_ID);
        assertThat(po.getCustomer()).isSameAs(customerReference);
        assertThat(po.getPromptTokens()).isEqualTo(1000);
        assertThat(po.getCompletionTokens()).isEqualTo(500);
        assertThat(po.getTotalTokens()).isEqualTo(1500);
        assertThat(po.getIncludedTokensUsed()).isEqualTo(1500);
        assertThat(po.getOverageTokens()).isZero();
        assertThat(po.getTotalCharge()).isEqualByComparingTo("0.00");
        assertThat(po.getCalculatedAt()).isEqualTo(CALCULATED_AT);
        verify(entityManager).getReference(CustomerPO.class, "CUST-001");
    }
}
