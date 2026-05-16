package org.tw.token_billing.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.tw.token_billing.controller.dto.BillResponse;
import org.tw.token_billing.controller.dto.UsageRequest;
import org.tw.token_billing.domain.model.Bill;
import org.tw.token_billing.domain.model.Customer;
import org.tw.token_billing.domain.model.CustomerSubscription;
import org.tw.token_billing.domain.model.PricingPlan;
import org.tw.token_billing.exception.BusinessException;
import org.tw.token_billing.exception.CustomerNotFoundException;
import org.tw.token_billing.exception.NoActiveSubscriptionException;
import org.tw.token_billing.repository.BillRepository;
import org.tw.token_billing.repository.CustomerRepository;
import org.tw.token_billing.repository.CustomerSubscriptionRepository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceImplTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-16T12:00:00Z"),
            ZoneOffset.UTC
    );
    private static final LocalDate CURRENT_DATE = LocalDate.of(2026, 5, 16);
    private static final LocalDateTime MONTH_START = LocalDateTime.of(2026, 5, 1, 0, 0);
    private static final LocalDateTime MONTH_END = LocalDateTime.of(2026, 6, 1, 0, 0);
    private static final LocalDateTime CALCULATION_TIMESTAMP = LocalDateTime.of(2026, 5, 16, 12, 0);

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerSubscriptionRepository customerSubscriptionRepository;

    @Mock
    private BillRepository billRepository;

    private BillingServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BillingServiceImpl(
                customerRepository,
                customerSubscriptionRepository,
                billRepository,
                FIXED_CLOCK
        );
    }

    @Test
    void should_return_zero_charge_bill_when_submit_usage_given_usage_within_remaining_quota() {
        givenActiveStarterSubscription();
        when(billRepository.sumTotalTokensForCustomerBetween("CUST-001", MONTH_START, MONTH_END))
                .thenReturn(60000L);
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillResponse response = service.submitUsage(new UsageRequest("CUST-001", 20000, 10000));

        assertThat(response.customerId()).isEqualTo("CUST-001");
        assertThat(response.totalTokens()).isEqualTo(30000);
        assertThat(response.tokensFromQuota()).isEqualTo(30000);
        assertThat(response.overageTokens()).isZero();
        assertThat(response.totalCharge()).isEqualByComparingTo("0.00");
        assertThat(response.calculationTimestamp()).isEqualTo(CALCULATION_TIMESTAMP);

        ArgumentCaptor<Bill> billCaptor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(billCaptor.capture());
        assertThat(billCaptor.getValue().includedTokensUsed()).isEqualTo(30000);
        assertThat(billCaptor.getValue().overageTokens()).isZero();
        verify(billRepository).sumTotalTokensForCustomerBetween("CUST-001", MONTH_START, MONTH_END);
    }

    @Test
    void should_return_overage_charge_when_submit_usage_given_usage_exceeds_remaining_quota() {
        givenActiveStarterSubscription();
        when(billRepository.sumTotalTokensForCustomerBetween("CUST-001", MONTH_START, MONTH_END))
                .thenReturn(80000L);
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillResponse response = service.submitUsage(new UsageRequest("CUST-001", 30000, 20000));

        assertThat(response.totalTokens()).isEqualTo(50000);
        assertThat(response.tokensFromQuota()).isEqualTo(20000);
        assertThat(response.overageTokens()).isEqualTo(30000);
        assertThat(response.totalCharge()).isEqualByComparingTo("0.60");
        assertThat(response.calculationTimestamp()).isEqualTo(CALCULATION_TIMESTAMP);

        ArgumentCaptor<Bill> billCaptor = ArgumentCaptor.forClass(Bill.class);
        verify(billRepository).save(billCaptor.capture());
        assertThat(billCaptor.getValue().totalCharge()).isEqualByComparingTo("0.60");
        assertThat(billCaptor.getValue().calculatedAt()).isEqualTo(CALCULATION_TIMESTAMP);
    }

    @Test
    void should_return_zero_charge_bill_when_submit_usage_given_zero_tokens() {
        givenActiveStarterSubscription();
        when(billRepository.sumTotalTokensForCustomerBetween("CUST-001", MONTH_START, MONTH_END))
                .thenReturn(0L);
        when(billRepository.save(any(Bill.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillResponse response = service.submitUsage(new UsageRequest("CUST-001", 0, 0));

        assertThat(response.totalTokens()).isZero();
        assertThat(response.tokensFromQuota()).isZero();
        assertThat(response.overageTokens()).isZero();
        assertThat(response.totalCharge()).isEqualByComparingTo("0.00");
        verify(billRepository).save(any(Bill.class));
    }

    @Test
    void should_return_customer_not_found_exception_when_submit_usage_given_unknown_customer() {
        when(customerRepository.findByIdForUpdate("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitUsage(new UsageRequest("UNKNOWN", 1000, 500)))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found");

        verify(customerRepository).findByIdForUpdate("UNKNOWN");
        verifyNoInteractions(customerSubscriptionRepository, billRepository);
    }

    @Test
    void should_return_business_exception_when_submit_usage_given_token_total_exceeds_integer_limit() {
        assertThatThrownBy(() -> service.submitUsage(new UsageRequest("CUST-001", Integer.MAX_VALUE, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Token total exceeds supported limit")
                .extracting("errorCode")
                .isEqualTo("TOKEN_TOTAL_TOO_LARGE");

        verifyNoInteractions(customerRepository, customerSubscriptionRepository, billRepository);
    }

    @Test
    void should_return_no_active_subscription_exception_when_submit_usage_given_customer_without_subscription() {
        Customer customer = customer();
        when(customerRepository.findByIdForUpdate("CUST-001")).thenReturn(Optional.of(customer));
        when(customerSubscriptionRepository.findActiveSubscription("CUST-001", CURRENT_DATE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submitUsage(new UsageRequest("CUST-001", 1000, 500)))
                .isInstanceOf(NoActiveSubscriptionException.class)
                .hasMessage("Active subscription not found");

        verifyNoInteractions(billRepository);
        verifyNoMoreInteractions(customerRepository, customerSubscriptionRepository);
    }

    private void givenActiveStarterSubscription() {
        Customer customer = customer();
        when(customerRepository.findByIdForUpdate("CUST-001")).thenReturn(Optional.of(customer));
        when(customerSubscriptionRepository.findActiveSubscription("CUST-001", CURRENT_DATE))
                .thenReturn(Optional.of(subscription(customer)));
    }

    private Customer customer() {
        return new Customer("CUST-001", "Acme Corp", LocalDateTime.of(2026, 1, 1, 0, 0));
    }

    private CustomerSubscription subscription(Customer customer) {
        PricingPlan plan = new PricingPlan(
                "PLAN-STARTER",
                "Starter",
                100000,
                new BigDecimal("0.0200"),
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        return new CustomerSubscription(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                customer,
                plan,
                LocalDate.of(2026, 1, 1),
                null,
                LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }
}
