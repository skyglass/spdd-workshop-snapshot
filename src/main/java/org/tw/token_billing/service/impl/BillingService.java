package org.tw.token_billing.service.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.tw.token_billing.controller.dto.BillResponse;
import org.tw.token_billing.controller.dto.UsageRequest;
import org.tw.token_billing.domain.model.Bill;
import org.tw.token_billing.domain.model.BillingCalculation;
import org.tw.token_billing.domain.model.Customer;
import org.tw.token_billing.domain.model.CustomerSubscription;
import org.tw.token_billing.exception.BusinessException;
import org.tw.token_billing.exception.CustomerNotFoundException;
import org.tw.token_billing.exception.NoActiveSubscriptionException;
import org.tw.token_billing.repository.BillRepository;
import org.tw.token_billing.repository.CustomerRepository;
import org.tw.token_billing.repository.CustomerSubscriptionRepository;
import org.tw.token_billing.service.UsageService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class BillingService implements UsageService {
    private static final String TOKEN_TOTAL_TOO_LARGE = "Token total exceeds supported limit";

    private final CustomerRepository customerRepository;
    private final CustomerSubscriptionRepository customerSubscriptionRepository;
    private final BillRepository billRepository;
    private final Clock clock;

    public BillingService(
            CustomerRepository customerRepository,
            CustomerSubscriptionRepository customerSubscriptionRepository,
            BillRepository billRepository,
            Clock clock
    ) {
        this.customerRepository = customerRepository;
        this.customerSubscriptionRepository = customerSubscriptionRepository;
        this.billRepository = billRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public BillResponse submitUsage(UsageRequest request) {
        int totalTokens = calculateTotalTokens(request);
        Instant now = clock.instant();
        LocalDate currentDate = LocalDate.now(clock);
        LocalDate monthStartDate = currentDate.withDayOfMonth(1);
        LocalDateTime monthStart = monthStartDate.atStartOfDay();
        LocalDateTime monthEnd = monthStartDate.plusMonths(1).atStartOfDay();
        LocalDateTime calculatedAt = LocalDateTime.ofInstant(now, ZoneOffset.UTC);

        Customer customer = customerRepository.findByIdForUpdate(request.customerId())
                .orElseThrow(CustomerNotFoundException::new);

        List<CustomerSubscription> activeSubscriptions = customerSubscriptionRepository.findActiveSubscriptions(
                customer.id(),
                currentDate
        );
        CustomerSubscription activeSubscription = activeSubscriptions.stream()
                .findFirst()
                .orElseThrow(() -> new NoActiveSubscriptionException(customer.id()));

        long currentMonthUsage = billRepository.sumTotalTokensForCustomerBetween(
                customer.id(),
                monthStart,
                monthEnd
        );

        BillingCalculation calculation = BillingCalculation.calculate(
                totalTokens,
                currentMonthUsage,
                activeSubscription.pricingPlan().monthlyQuota(),
                activeSubscription.pricingPlan().overageRatePer1k()
        );

        Bill bill = Bill.create(
                customer,
                request.promptTokens(),
                request.completionTokens(),
                calculation,
                calculatedAt
        );
        return BillResponse.from(billRepository.save(bill));
    }

    private int calculateTotalTokens(UsageRequest request) {
        long total = (long) request.promptTokens() + request.completionTokens();
        if (total > Integer.MAX_VALUE) {
            throw new BusinessException(
                    "TOKEN_TOTAL_TOO_LARGE",
                    TOKEN_TOTAL_TOO_LARGE,
                    HttpStatus.BAD_REQUEST
            );
        }
        return (int) total;
    }
}
