# SPDD Analysis: Token Usage Billing

## Original Business Requirement
## Background
The LLM API platform charges customers based on token consumption. Customers have monthly included quotas; usage exceeding the quota is billed at an overage rate.

## Business Value
1. **Accurate Billing**: Calculate charges based on actual token consumption.
2. **Quota Management**: Track usage against included quotas.
3. **Revenue Capture**: Bill overage when customers exceed quotas.

## Scope In
* Implement POST /api/usage endpoint for submitting token usage and receiving calculated bills.
* Request fields:
  * Customer ID (required, must exist)
  * Prompt tokens (required, ≥ 0)
  * Completion tokens (required, ≥ 0)
* Calculate bill using customer's monthly quota, current month usage, and overage rate.

## Scope Out
* Customer CRUD operations.
* Historical bill queries.
* Monthly quota reset logic.

## Acceptance Criteria (ACs)
1. Validate Customer ID exists
   **Given** customer ID does not exist
   **When** backend receives request
   **Then** return HTTP 404, message "Customer not found".

2. Validate token counts are non-negative
   **Given** prompt tokens or completion tokens is negative
   **When** backend validates request
   **Then** return HTTP 400, message "Token count cannot be negative".

3. Bill within included quota
   **Given** customer has 100,000 monthly quota and 60,000 tokens used this month
   **When** submitting 30,000 tokens
   **Then** bill shows: 30,000 from quota, 0 overage, $0.00 charge.

4. Bill exceeding included quota
   **Given** customer has 100,000 monthly quota, 80,000 tokens used this month, overage rate $0.02 per 1K tokens
   **When** submitting 50,000 tokens
   **Then** bill shows: 20,000 from quota, 30,000 overage, $0.60 charge.

5. Successful return
   **Given** valid request
   **When** bill is calculated
   **Then** return HTTP 201 with bill details including: bill ID, customer ID, total tokens, tokens from quota, overage tokens, total charge, and calculation timestamp.

## Domain Concept Identification

#### Existing Concepts (from codebase)
- Customer: Represents the account that submits token usage and receives bills. The existing schema stores customers as basic account records and links them to subscriptions and bills.
- Pricing Plan: Represents reusable commercial terms for billing. The existing schema stores the monthly included quota and overage rate on pricing plans, making the plan the owner of quota and price policy rather than the customer record.
- Customer Subscription: Represents the relationship between a customer and a pricing plan over time. The existing schema uses subscriptions as the bridge that determines which pricing plan applies to a customer for billing.
- Bill: Represents both submitted usage and the calculated billing result. The existing schema records token quantities, quota allocation, overage tokens, charge, customer ownership, and calculation time.

#### New Concepts Required
- Usage Submission: Represents the incoming business event where a customer reports prompt and completion token consumption. It relates to Customer for ownership and to Bill as the resulting persisted billing record.
- Billing Calculation: Represents the decision process that allocates submitted tokens between remaining quota and overage, then determines the charge. It depends on Customer Subscription, Pricing Plan, and existing Bill records for current-month usage.
- Current Month Usage: Represents the customer's already-consumed tokens in the active billing month. It is needed to calculate remaining quota and is conceptually derived from existing Bill history rather than introduced as a separate persisted business object in the current scope.

#### Key Business Rules
- Customer existence is mandatory: Usage can only be billed for a known Customer.
- Token counts must be non-negative: Prompt and completion token quantities cannot be below zero.
- Total usage is additive: A submission's total token count is the combined prompt and completion token consumption.
- Included quota is consumed before overage: Submitted tokens first use any remaining monthly quota, and only tokens beyond that remaining quota become overage.
- Current-month history affects every calculation: Prior Bill records in the same billing month reduce the available quota for the new submission.
- Overage charge follows the customer's applicable pricing plan: Overage tokens are billed using the overage rate associated with the customer's active plan.
- Successful submissions create a bill: A valid calculation must produce a Bill with customer ownership, token allocation, charge, and calculation timestamp.
- Monthly reset job behavior is out of scope: The calculation still needs a clear current-month boundary, but no separate reset process should be introduced for this story.

## Strategic Approach

#### Solution Direction
- Build the feature as a backend API capability in the existing Spring Boot service. The explored project is a Java 21 Spring Boot 3.5 backend using Spring Web, Spring Validation, Spring Data JPA, Flyway-managed PostgreSQL schema, and a minimal existing application/test scaffold.
- Use the existing relational domain as the source of truth. Customers already exist; pricing terms live on pricing plans; customer subscriptions connect customers to those pricing terms; bills store usage and calculated billing results.
- Keep the data flow aligned with the requirement: REST usage submission -> validation and customer/plan lookup -> service-layer billing calculation -> bill persistence -> created response with bill details.
- Treat this as mostly greenfield application logic over an existing schema. No relevant controller, service, repository, entity, or error-handling conventions currently exist beyond the framework stack, so the next phase should establish those conventions consistently.

#### Key Design Decisions
- Source of quota and overage policy: The schema places monthly quota and overage rate on Pricing Plan, reached through Customer Subscription. Using those tables respects the existing data model; placing billing terms directly on Customer would duplicate or bypass the schema. Recommendation: derive billing terms from the customer's applicable subscription and plan.
- Source of current-month usage: The bills table already stores usage and calculation timestamps, so it can serve as the current-month usage ledger. A separate usage aggregate could improve performance later but adds synchronization risk and is not required by the scope. Recommendation: use existing bills as the authoritative usage history for this story.
- Billing result lifecycle: The acceptance criteria require immediate bill details after submission, and the existing bill table stores calculated results. Asynchronous or deferred billing would complicate the user-visible contract. Recommendation: one valid usage submission should synchronously create one Bill.
- Handling validation and business failures: The ACs require specific statuses and messages for missing customers and negative tokens. The project includes Spring Validation but has no established error format. Recommendation: establish a small, consistent API error convention that preserves the exact required messages.
- Monetary calculation policy: The requirement expresses overage pricing per 1K tokens and examples use exact multiples of 1,000. Non-multiple token counts need a deliberate rounding/proration policy. Recommendation: carry this as an explicit design constraint into the REASONS Canvas rather than leaving it implicit.

#### Alternatives Considered
- Add customer CRUD or plan management: Rejected because both are explicitly outside scope and seed data already provides customers and subscriptions for the billing workflow.
- Introduce a monthly quota reset process: Rejected because monthly reset logic is explicitly outside scope; the story only needs current-month usage interpretation.
- Separate usage ingestion from bill creation: Rejected because the API is expected to return calculated bill details immediately with HTTP 201.
- Store current-month usage in a new aggregate table: Rejected for this story because the existing bills table is sufficient as a usage ledger, while an aggregate would introduce additional consistency and concurrency responsibilities.

## Risk & Gap Analysis

#### Requirement Ambiguities
- Current month boundary: The requirement does not specify whether month boundaries follow server time, UTC, customer locale, or billing-account timezone.
- Active subscription selection: The schema supports subscription effective dates, but the requirement does not define what happens with no subscription, future subscription, expired subscription, or overlapping subscriptions.
- Pricing granularity: Overage rate is per 1K tokens, but the requirement does not define whether non-multiple token counts are prorated exactly or rounded up to the next 1K.
- Monetary currency and rounding: The examples use dollar amounts, but currency, rounding mode, and precision rules are not explicitly specified.
- Duplicate submissions: The requirement does not address retries, idempotency, or whether duplicate valid requests should create duplicate bills.
- Zero-token submissions: Non-negative validation allows zero prompt and zero completion tokens, but the expected bill behavior for total zero usage is not stated.
- Existing usage source: The requirement says "current month usage" but does not state whether this is derived from persisted bills, a separate usage counter, or an external metering source.
- Error response shape: Required messages are specified, but the surrounding API error structure is not.

#### Edge Cases
- Submission exactly consumes the remaining quota: Important to ensure no overage is charged at the boundary.
- Customer already exceeded quota before the submission: All newly submitted tokens should conceptually be overage, but the ACs do not state this case directly.
- Partial quota remaining: One submission can be split between quota and overage, as shown by AC4, and should stay consistent for smaller partial amounts.
- Multiple subscriptions for one customer: Effective date handling can change which quota and overage rate apply.
- Missing or inactive pricing plan: Customer existence alone may not be enough to calculate a bill.
- Very large token counts: Token totals and current-month accumulated usage can exceed small numeric assumptions if customers submit high-volume usage.
- Month transition during calculation: A request near the month boundary can be misclassified if the current-month reference time is not consistent.
- Concurrent submissions for the same customer: Two valid requests can both observe the same remaining quota and over-allocate included tokens without a concurrency strategy.

#### Technical Risks
- Greenfield application layer: The project has schema and seed data but no existing domain classes, controllers, repositories, or API error pattern to reuse, so the first implementation will establish conventions for future work.
- Schema validation constraints: Hibernate is configured to validate against Flyway-managed PostgreSQL schema, so application mapping and schema expectations must stay aligned.
- Database compatibility in tests: The build includes H2 and PostgreSQL drivers, while application configuration targets PostgreSQL. Tests that use H2 may miss PostgreSQL-specific behavior unless the test strategy is chosen deliberately.
- Concurrency and data integrity: Current-month usage derived from existing bills can be incorrect under simultaneous submissions unless the design includes a consistency boundary.
- Query growth over time: Using bills as the usage ledger is appropriate for this story, but repeated current-month usage calculation may become expensive as bill history grows.

#### Acceptance Criteria Coverage
| AC# | Description | Addressable? | Gaps/Notes |
|-----|-------------|--------------|------------|
| 1 | Validate Customer ID exists and return HTTP 404 with "Customer not found". | Yes | Customer table exists; API error format beyond the message remains unspecified. |
| 2 | Validate prompt or completion tokens are non-negative and return HTTP 400 with "Token count cannot be negative". | Yes | Requirement does not clarify missing fields versus negative fields, but non-negative validation is directly addressable. |
| 3 | Calculate a zero-charge bill when submitted usage remains within included quota. | Yes | Current-month usage boundary must be defined to determine the 60,000 prior usage consistently. |
| 4 | Split submitted tokens between remaining quota and overage and calculate overage charge. | Yes | Example uses an exact 1K multiple; non-multiple rounding/proration remains open. |
| 5 | Return HTTP 201 with bill details after successful calculation. | Yes | Bill persistence aligns with the existing schema; exact response shape and timestamp timezone remain to be specified. |
