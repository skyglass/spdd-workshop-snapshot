# Token Usage Billing API - Test Prompt

Generated from: `spdd/prompt/GGQPA-XXX-202605160301-[Feat]-api-token-usage-billing.md`

Use the standard template at: `spdd/template/TEST-SCENARIOS-TEMPLATE.md`

## Feature Summary
- Endpoint: `POST /api/usage`
- Request JSON fields: `customerId`, `promptTokens`, `completionTokens`
- Success response: HTTP 201 with `billId`, `customerId`, `totalTokens`, `tokensFromQuota`, `overageTokens`, `totalCharge`, and `calculationTimestamp`
- Error response: `ErrorResponse(errorCode, message, timestamp, path)`
- Billing rules:
  - Total tokens are prompt plus completion tokens.
  - Current month usage is summed from `bills.calculated_at` within the current UTC month.
  - Included quota is consumed before overage.
  - Overage charge is `overageTokens / 1000 * overageRatePer1k`, rounded to two decimals with `HALF_UP`.
  - Zero-token submissions are valid and create zero-charge bills.

## Existing Code Inspected
- Controller: `UsageController`
- DTOs: `UsageRequest`, `BillResponse`, `ErrorResponse`
- Service: `UsageService`, `BillingServiceImpl`
- Domain: `Customer`, `PricingPlan`, `CustomerSubscription`, `BillingCalculation`, `Bill`
- Repositories/adapters: `CustomerRepository`, `CustomerSubscriptionRepository`, `BillRepository`, JPA repositories and adapters
- Mappers: `CustomerPersistenceMapper`, `PricingPlanPersistenceMapper`, `CustomerSubscriptionPersistenceMapper`, `BillPersistenceMapper`
- Existing tests: only default `TokenBillingApplicationTests.contextLoads`

## Known Prompt/Implementation Gaps
- The feature prompt describes `UsageRequest.totalTokens()`, but the current `UsageRequest` record does not implement that method.
- The feature prompt describes `CustomerSubscription.isActiveOn(LocalDate)`, but the current `CustomerSubscription` record does not implement that method.
- Do not add production behavior from tests. Report these as SPDD prompt/code mismatches unless production code is explicitly updated separately.

## 1. `UsageController` Test Scenarios
### Create `UsageControllerTest`
Use `@WebMvcTest(UsageController.class)`, `MockMvc`, and a mocked `UsageService`. Provide a deterministic mocked `Clock` for `GlobalExceptionHandler`.

#### `should_return_created_bill_response_when_submit_usage_given_valid_request`
- Description: verifies route, status code, service delegation, and response mapping.
- Input: customer `CUST-001`, prompt `1000`, completion `500`.
- Expected Output: HTTP 201 with bill fields.
- Verification Points:
  - `UsageService.submitUsage` is called once with parsed request values.
  - Response contains `billId`, `customerId`, `totalTokens`, quota, overage, charge, and timestamp.

#### `should_return_validation_error_when_submit_usage_given_negative_prompt_tokens`
- Description: verifies validation preserves the exact negative-token message.
- Input: prompt `-1`, completion `500`.
- Expected Output: HTTP 400 with `message = Token count cannot be negative`.
- Verification Points:
  - Service is not called.
  - Error path is `/api/usage`.

#### `should_return_validation_error_when_submit_usage_given_missing_customer_id`
- Description: verifies missing required fields return the generic required-field message.
- Input: request without `customerId`.
- Expected Output: HTTP 400 with `message = Required field is missing`.
- Verification Points:
  - Service is not called.
  - Error code is `VALIDATION_ERROR`.

#### `should_return_invalid_request_body_when_submit_usage_given_malformed_json`
- Description: verifies invalid JSON maps to structured error response.
- Input: malformed JSON body.
- Expected Output: HTTP 400 with `message = Invalid request body`.
- Verification Points:
  - Service is not called.
  - Error code is `INVALID_REQUEST_BODY`.

#### `should_return_customer_not_found_when_submit_usage_given_unknown_customer`
- Description: verifies business exceptions are converted by the global handler.
- Input: valid request where service throws `CustomerNotFoundException`.
- Expected Output: HTTP 404 with `message = Customer not found`.
- Verification Points:
  - Error code is `CUSTOMER_NOT_FOUND`.
  - Error path is `/api/usage`.

## 2. `BillingServiceImpl` Test Scenarios
### Create `BillingServiceImplTest`
Use JUnit 5 and Mockito. Mock `CustomerRepository`, `CustomerSubscriptionRepository`, and `BillRepository`. Use `Clock.fixed(Instant.parse("2026-05-16T12:00:00Z"), ZoneOffset.UTC)`.

#### `should_return_zero_charge_bill_when_submit_usage_given_usage_within_remaining_quota`
- Description: verifies AC3 business calculation.
- Input: CUST-001 plan quota 100000, prior usage 60000, submitted 20000 prompt and 10000 completion.
- Expected Output: total 30000, quota tokens 30000, overage 0, charge 0.00.
- Verification Points:
  - Customer is loaded with pessimistic-lock repository method.
  - Current month window is `[2026-05-01T00:00, 2026-06-01T00:00)`.
  - Saved bill contains calculated values.

#### `should_return_overage_charge_when_submit_usage_given_usage_exceeds_remaining_quota`
- Description: verifies AC4 business calculation.
- Input: CUST-001 plan quota 100000, prior usage 80000, submitted 30000 prompt and 20000 completion.
- Expected Output: total 50000, quota tokens 20000, overage 30000, charge 0.60.
- Verification Points:
  - Overage is calculated only for tokens beyond remaining quota.
  - Saved bill timestamp is the fixed UTC timestamp.

#### `should_return_zero_charge_bill_when_submit_usage_given_zero_tokens`
- Description: verifies accepted zero-token edge case.
- Input: prompt `0`, completion `0`.
- Expected Output: total 0, quota tokens 0, overage 0, charge 0.00.
- Verification Points:
  - A bill is still saved.
  - No overage is charged.

#### `should_return_customer_not_found_exception_when_submit_usage_given_unknown_customer`
- Description: verifies AC1 service error path.
- Input: missing customer ID.
- Expected Output: `CustomerNotFoundException`.
- Verification Points:
  - Subscription and bill repositories are not called.

#### `should_return_business_exception_when_submit_usage_given_token_total_exceeds_integer_limit`
- Description: verifies integer schema capacity guard.
- Input: prompt `Integer.MAX_VALUE`, completion `1`.
- Expected Output: `BusinessException` with `TOKEN_TOTAL_TOO_LARGE`.
- Verification Points:
  - Repositories are not called.

#### `should_return_no_active_subscription_exception_when_submit_usage_given_customer_without_subscription`
- Description: verifies business error for billable customer with no active plan.
- Input: existing customer but no active subscription.
- Expected Output: `NoActiveSubscriptionException`.
- Verification Points:
  - Bill repository is not called.

## 3. Repository and Adapter Test Scenarios
### Create `JpaBillRepositoryAdapterTest`
Use Mockito for `BillJpaRepository` and `BillPersistenceMapper`.

#### `should_return_zero_when_sum_total_tokens_given_repository_returns_null`
- Description: verifies adapter normalizes null sums to zero.
- Input: repository returns null for a sum query.
- Expected Output: `0L`.
- Verification Points:
  - Adapter delegates exact customer ID and month bounds.

#### `should_return_saved_domain_bill_when_save_given_domain_bill`
- Description: verifies save mapping path.
- Input: domain `Bill` with mapper converting to and from `BillPO`.
- Expected Output: saved domain bill.
- Verification Points:
  - Mapper and JPA repository are called in order.

## 4. DAO Test Scenarios
No DAO classes exist in the current implementation. Do not create DAO tests.

## 5. Model and Mapper Test Scenarios
### Create `BillingCalculationTest`

#### `should_return_quota_only_calculation_when_calculate_given_usage_within_remaining_quota`
- Description: verifies pure quota allocation.
- Input: total 30000, current usage 60000, quota 100000, rate 0.0200.
- Expected Output: quota 30000, overage 0, charge 0.00.

#### `should_return_overage_calculation_when_calculate_given_usage_exceeds_remaining_quota`
- Description: verifies split quota and overage allocation.
- Input: total 50000, current usage 80000, quota 100000, rate 0.0200.
- Expected Output: quota 20000, overage 30000, charge 0.60.

#### `should_return_zero_charge_when_calculate_given_zero_tokens`
- Description: verifies zero-token calculations.
- Input: total 0, current usage 0, quota 100000, rate 0.0200.
- Expected Output: quota 0, overage 0, charge 0.00.

### Create `BillResponseTest`

#### `should_return_response_fields_when_mapping_given_domain_bill`
- Description: verifies response DTO mapping from `Bill`.
- Input: domain bill with known ID, customer, token values, charge, timestamp.
- Expected Output: matching response fields.

### Create `BillPersistenceMapperTest`

#### `should_return_domain_bill_when_mapping_given_bill_po`
- Description: verifies persistence-to-domain mapping.
- Input: `BillPO` with nested `CustomerPO`.
- Expected Output: domain `Bill` with matching values.

#### `should_return_bill_po_when_mapping_given_domain_bill`
- Description: verifies domain-to-persistence mapping uses an entity reference for customer.
- Input: domain `Bill`.
- Expected Output: `BillPO` with matching values and `CustomerPO` reference.

## 6. Integration Test Scenarios
### Create `UsageControllerIntegrationTest`
Use `@SpringBootTest`, `@AutoConfigureMockMvc`, an H2 test datasource in PostgreSQL mode, Flyway migrations, `MockMvc`, `JdbcTemplate`, and a mocked fixed UTC `Clock`.

#### `should_return_customer_not_found_when_submit_usage_given_unknown_customer`
- Description: verifies AC1 end to end.
- Input: unknown customer ID.
- Expected Output: HTTP 404 with `Customer not found`.

#### `should_return_validation_error_when_submit_usage_given_negative_prompt_tokens`
- Description: verifies AC2 negative prompt path.
- Input: negative prompt tokens.
- Expected Output: HTTP 400 with `Token count cannot be negative`.

#### `should_return_validation_error_when_submit_usage_given_negative_completion_tokens`
- Description: verifies AC2 negative completion path.
- Input: negative completion tokens.
- Expected Output: HTTP 400 with `Token count cannot be negative`.

#### `should_return_zero_charge_bill_when_submit_usage_given_usage_within_remaining_quota`
- Description: verifies AC3 through controller, service, repository, and database.
- Input: insert prior current-month bill of 60000 tokens for `CUST-001`; submit 30000 tokens.
- Expected Output: HTTP 201 with quota 30000, overage 0, charge 0.00.
- Verification Points:
  - Response has all bill fields.
  - Persisted bill row has expected total, quota, overage, and charge.

#### `should_return_overage_charge_when_submit_usage_given_usage_exceeds_remaining_quota`
- Description: verifies AC4 through full application wiring.
- Input: insert prior current-month bill of 80000 tokens for `CUST-001`; submit 50000 tokens.
- Expected Output: HTTP 201 with quota 20000, overage 30000, charge 0.60.
- Verification Points:
  - Persisted bill row matches response values.

#### `should_return_bill_details_when_submit_usage_given_valid_request`
- Description: verifies AC5 successful return and persistence.
- Input: no prior bills; submit 1000 prompt and 500 completion for `CUST-001`.
- Expected Output: HTTP 201 with bill ID, customer ID, total tokens, quota tokens, overage tokens, total charge, and timestamp.
- Verification Points:
  - One new bill row is persisted.
  - Response total is 1500 and charge is 0.00.

## 7. Test Support Configuration
- Add `src/test/resources/application.yml` with an H2 in-memory datasource using PostgreSQL compatibility mode.
- Keep Flyway enabled for tests so the production migration creates and seeds the test schema.
- Use deterministic fixed clock values in unit and integration tests.
- Test method names must follow `should_return_[expected_output]_when_[action]_given_[input]`.
