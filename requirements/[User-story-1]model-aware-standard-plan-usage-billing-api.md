# Story Decomposition: Model-Aware Standard Plan Usage Billing

## Source Requirement

Referenced file: `requirements/GGQPA-001-idea-of-the-enhancement.md`

```markdown
# The enhancement content

We need to enhance the billing engine to support diverse subscription strategies
and variable, model-specific pricing. This requires several changes:

- API enhancement: update the existing POST /api/usage endpoint to accept a new,
  required modelId parameter (e.g. "fast-model", "reasoning-model").
- Model-aware pricing: shift from a single global rate to dynamic pricing, where
  costs vary depending on the specific AI model invoked.
- Multi-plan billing logic: introduce distinct billing behaviors based on the
  customer's subscription tier:
    - Standard plan: retains the global monthly quota, but overage usage is now
      calculated using model-specific rates.
    - Premium plan: operates without a quota limit. Prompt tokens and completion
      tokens are charged separately at different rates depending on the model used.
- Architectural scalability: implement an extensible design pattern, such as
  Strategy or Factory, to isolate calculation formulas for different plans.
```

## INVEST Analysis

### Abstract Task: "Model-Aware Standard Plan Usage Billing"

**Analysis Dimensions**:
- **Core Responsibility**: Allow Standard plan customers to submit token usage for a specific AI model and receive quota-aware billing that applies the correct model-specific overage rate.
- **Primary Operations**: Submit usage, validate model selection, calculate quota consumption, calculate model-specific overage charge, return calculated bill.
- **Key Constraints**: `modelId` is required, the model must be supported by billing, Standard customers keep a monthly included quota, and only tokens above the quota are charged.
- **Technical Complexity**: Medium - extends an existing usage submission workflow with model-specific billing behavior.
- **Business Complexity**: Medium - billing results depend on current monthly usage, customer plan, selected model, and configured model rates.

### INVEST Evaluation
- **Independent**: Pass - can be delivered as an enhancement to Standard plan billing without requiring Premium plan behavior.
- **Negotiable**: Pass - exact supported model names, rate values, and response field naming can be agreed during implementation planning.
- **Valuable**: Pass - enables accurate billing for Standard customers when different AI models have different costs.
- **Estimable**: Pass - scope is limited to model-aware Standard plan usage submission and related validations.
- **Small**: Pass - expected to fit in 2-3 days of implementation and verification.
- **Testable**: Pass - acceptance criteria define concrete customer plans, model IDs, token counts, rates, and expected charges.

**Conclusion**: Split needed. The full enhancement combines Standard plan and Premium plan billing behavior. This story covers the Standard plan part only.

### Split Strategy
- **Split dimension**: By user subscription plan.
- **Story boundary**: This story covers Standard plan billing with model-specific overage rates. Premium no-quota billing with separate prompt and completion rates is covered by `[STORY-001-002]`.
- **Reasoning**: Standard and Premium plans have different business rules and can be tested independently through different customer scenarios.

---

## [STORY-001-001] Model-Aware Standard Plan Usage Billing API Development

### Background
The LLM API platform already supports usage-based billing for customers with monthly included quotas. The business now needs billing to reflect that different AI models have different operating costs. Standard plan customers should continue to receive their monthly included quota, but any usage above that quota must be charged at the overage rate for the selected model.

This capability lets API consumers submit usage for models such as `"fast-model"` and `"reasoning-model"` while keeping the familiar Standard plan quota behavior. It is needed now because a single global overage rate no longer represents the cost of serving usage across multiple AI models.

### Business Value
- Provide model-specific overage billing for Standard plan customers.
- Support accurate revenue capture when higher-cost models exceed the included monthly quota.
- Enable the existing usage API to distinguish billing outcomes by AI model without changing the customer-facing billing workflow.

### Dependencies and Assumptions
- **Prerequisites**: The existing `POST /api/usage` capability from `requirements/token-usage-billing-story.md` is available for submitting token usage and returning calculated bills.
- **Data assumptions**: Standard customers have a monthly quota. Billing has configured model IDs and Standard plan overage rates, including `"fast-model"` at `$0.01 per 1,000 overage tokens` and `"reasoning-model"` at `$0.06 per 1,000 overage tokens`.
- **Integration points**: Existing usage submission API and existing customer billing data.
- **Business constraints**: Bills for Standard customers must show the selected model and must charge only usage above the customer's monthly quota.

### Scope In
- Require API consumers to provide `modelId` when submitting usage.
- Validate that the requested model is supported for billing.
- Calculate Standard plan usage using the customer's monthly quota and the selected model's overage rate.
- Preserve existing customer and token-count validation behavior for Standard plan submissions.

### Scope Out
- Premium plan no-quota billing with separate prompt and completion token rates.
- Customer creation, customer plan management, model price administration, historical bill queries, and monthly quota reset logic.
- Internal architecture or design pattern choices; those belong to downstream SPDD analysis and implementation planning.

### Acceptance Criteria

#### AC1: Standard Customer Usage Within Quota Has No Charge
**Given** Standard customer `"cust-standard-100"` has a monthly quota of `100,000` tokens and has used `40,000` tokens this month, and `"fast-model"` is a supported billable model  
**When** the customer submits usage with `modelId` `"fast-model"`, `20,000` prompt tokens, and `30,000` completion tokens  
**Then** the bill is accepted with HTTP `201`, records `50,000` total tokens for `"fast-model"`, applies `50,000` tokens from quota, records `0` overage tokens, and charges `$0.00`

#### AC2: Standard Customer Overage Uses Fast Model Rate
**Given** Standard customer `"cust-standard-100"` has a monthly quota of `100,000` tokens and has used `80,000` tokens this month, and `"fast-model"` has a Standard plan overage rate of `$0.01 per 1,000 tokens`  
**When** the customer submits usage with `modelId` `"fast-model"`, `10,000` prompt tokens, and `30,000` completion tokens  
**Then** the bill is accepted with HTTP `201`, applies `20,000` tokens from quota, records `20,000` overage tokens, and charges `$0.20`

#### AC3: Standard Customer Overage Uses Reasoning Model Rate
**Given** Standard customer `"cust-standard-100"` has a monthly quota of `100,000` tokens and has used `80,000` tokens this month, and `"reasoning-model"` has a Standard plan overage rate of `$0.06 per 1,000 tokens`  
**When** the customer submits usage with `modelId` `"reasoning-model"`, `10,000` prompt tokens, and `30,000` completion tokens  
**Then** the bill is accepted with HTTP `201`, applies `20,000` tokens from quota, records `20,000` overage tokens, and charges `$1.20`

#### AC4: Model ID Is Required
**Given** Standard customer `"cust-standard-100"` exists and has an active monthly quota  
**When** the customer submits usage with `5,000` prompt tokens and `5,000` completion tokens but does not provide `modelId`  
**Then** the system rejects the request with HTTP `400` and the user-facing message `"Model ID is required"`

#### AC5: Unsupported Model Is Rejected
**Given** Standard customer `"cust-standard-100"` exists and `"legacy-model"` is not supported for billing  
**When** the customer submits usage with `modelId` `"legacy-model"`, `5,000` prompt tokens, and `5,000` completion tokens  
**Then** the system rejects the request with HTTP `400` and the user-facing message `"Model ID is not supported"`

#### AC6: Existing Usage Validation Still Applies
**Given** Standard customer `"cust-standard-100"` exists and `"fast-model"` is a supported billable model  
**When** the customer submits usage with `modelId` `"fast-model"`, `-1` prompt tokens, and `1,000` completion tokens  
**Then** the system rejects the request with HTTP `400` and the user-facing message `"Token count cannot be negative"`

### Quality Check Result
- **Structure and completeness**: Passed - all required sections are present.
- **Business clarity**: Passed - the story is focused on Standard plan customers and model-specific overage billing.
- **Sizing and independence**: Passed - the story contains three core functional points: required model selection, model validation, and Standard plan overage calculation.
- **Final INVEST re-validation**: Passed - the story is independent, complete, valuable, estimable, right-sized, and testable.
