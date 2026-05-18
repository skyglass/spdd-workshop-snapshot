# Story Decomposition: Premium Plan Model-Specific Usage Billing

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

### Abstract Task: "Premium Plan Model-Specific Usage Billing"

**Analysis Dimensions**:
- **Core Responsibility**: Allow Premium plan customers to submit usage for a specific AI model and receive billing with separate prompt-token and completion-token charges, without applying a monthly quota limit.
- **Primary Operations**: Submit usage, validate model selection, identify Premium plan billing behavior, calculate prompt-token charge, calculate completion-token charge, return calculated bill.
- **Key Constraints**: `modelId` is required, the model must be supported by billing, Premium customers do not receive quota deductions, and prompt and completion token rates differ by model.
- **Technical Complexity**: Medium - adds a second plan-specific billing behavior to the existing usage submission workflow.
- **Business Complexity**: Medium - billing results depend on customer plan, selected model, prompt token count, completion token count, and configured model-specific rates.

### INVEST Evaluation
- **Independent**: Pass - can be delivered as Premium plan billing behavior using existing usage submission and customer data.
- **Negotiable**: Pass - exact Premium model rates and bill presentation details can be agreed during implementation planning.
- **Valuable**: Pass - enables monetization of Premium plan usage without quota limits while reflecting different costs for prompt and completion tokens.
- **Estimable**: Pass - scope is limited to Premium plan calculation and related validations.
- **Small**: Pass - expected to fit in 2-4 days of implementation and verification.
- **Testable**: Pass - acceptance criteria define concrete plans, model IDs, token counts, rates, and expected charges.

**Conclusion**: Split needed. The full enhancement combines Standard plan and Premium plan billing behavior. This story covers the Premium plan part only.

### Split Strategy
- **Split dimension**: By user subscription plan.
- **Story boundary**: This story covers Premium plan billing without quotas and with separate prompt and completion token rates. Standard plan quota-based model overage billing is covered by `[STORY-001-001]`.
- **Reasoning**: Premium billing has a different business model from Standard billing and can be tested independently through Premium customer scenarios.

---

## [STORY-001-002] Premium Plan Model-Specific Usage Billing API Development

### Background
Premium plan customers use the LLM API without a monthly quota limit. Instead of consuming included tokens and paying only for overage, they are charged directly for submitted usage. The business also needs prompt tokens and completion tokens to be charged separately because the cost profile can differ by token type and AI model.

This capability allows Premium customers to use models such as `"fast-model"` and `"reasoning-model"` while receiving bills that reflect the selected model and the separate prompt and completion token rates.

### Business Value
- Provide no-quota usage billing for Premium plan customers.
- Support separate prompt-token and completion-token pricing for each AI model.
- Enable Premium plan revenue capture that aligns with model-specific cost differences.

### Dependencies and Assumptions
- **Prerequisites**: The existing `POST /api/usage` capability from `requirements/token-usage-billing-story.md` is available for submitting token usage and returning calculated bills.
- **Data assumptions**: Premium customers are identifiable by subscription tier. Billing has configured Premium rates, including `"fast-model"` at `$0.005 per 1,000 prompt tokens` and `$0.015 per 1,000 completion tokens`, and `"reasoning-model"` at `$0.03 per 1,000 prompt tokens` and `$0.09 per 1,000 completion tokens`.
- **Integration points**: Existing usage submission API and existing customer billing data.
- **Business constraints**: Premium bills must not apply monthly quota deductions or overage calculations. Charges must be based on the selected model and the submitted prompt and completion token counts.

### Scope In
- Calculate Premium plan usage without applying a quota limit.
- Charge prompt tokens and completion tokens separately using the selected model's Premium rates.
- Require and validate `modelId` for Premium plan usage submissions.
- Return a bill that makes the selected plan, model, prompt charge, completion charge, and total charge clear to the API consumer.

### Scope Out
- Standard plan quota-based model overage billing.
- Customer creation, customer plan management, model price administration, historical bill queries, and monthly quota reset logic.
- Internal architecture or design pattern choices; those belong to downstream SPDD analysis and implementation planning.

### Acceptance Criteria

#### AC1: Premium Fast Model Usage Charges Prompt and Completion Separately
**Given** Premium customer `"cust-premium-200"` has no monthly quota limit, and `"fast-model"` has Premium rates of `$0.005 per 1,000 prompt tokens` and `$0.015 per 1,000 completion tokens`  
**When** the customer submits usage with `modelId` `"fast-model"`, `40,000` prompt tokens, and `10,000` completion tokens  
**Then** the bill is accepted with HTTP `201`, records `40,000` prompt tokens charged at `$0.20`, records `10,000` completion tokens charged at `$0.15`, records no quota deduction, and charges `$0.35` total

#### AC2: Premium Reasoning Model Usage Uses Reasoning Rates
**Given** Premium customer `"cust-premium-200"` has no monthly quota limit, and `"reasoning-model"` has Premium rates of `$0.03 per 1,000 prompt tokens` and `$0.09 per 1,000 completion tokens`  
**When** the customer submits usage with `modelId` `"reasoning-model"`, `10,000` prompt tokens, and `20,000` completion tokens  
**Then** the bill is accepted with HTTP `201`, records `10,000` prompt tokens charged at `$0.30`, records `20,000` completion tokens charged at `$1.80`, records no quota deduction, and charges `$2.10` total

#### AC3: Premium Billing Applies Even When Monthly Usage Is High
**Given** Premium customer `"cust-premium-200"` has already submitted `900,000` tokens this month, has no monthly quota limit, and `"fast-model"` is a supported billable model  
**When** the customer submits usage with `modelId` `"fast-model"`, `100,000` prompt tokens, and `100,000` completion tokens  
**Then** the bill is accepted with HTTP `201`, does not reject the request for exceeding a quota, records no overage tokens, and charges the full Premium model-specific amount of `$2.00`

#### AC4: Zero Prompt or Completion Tokens Are Charged Correctly
**Given** Premium customer `"cust-premium-200"` has no monthly quota limit, and `"reasoning-model"` has a Premium completion-token rate of `$0.09 per 1,000 tokens`  
**When** the customer submits usage with `modelId` `"reasoning-model"`, `0` prompt tokens, and `10,000` completion tokens  
**Then** the bill is accepted with HTTP `201`, records `$0.00` prompt-token charge, records `$0.90` completion-token charge, and charges `$0.90` total

#### AC5: Model ID Is Required For Premium Usage
**Given** Premium customer `"cust-premium-200"` exists  
**When** the customer submits usage with `5,000` prompt tokens and `5,000` completion tokens but does not provide `modelId`  
**Then** the system rejects the request with HTTP `400` and the user-facing message `"Model ID is required"`

#### AC6: Unsupported Model Is Rejected For Premium Usage
**Given** Premium customer `"cust-premium-200"` exists and `"legacy-model"` is not supported for billing  
**When** the customer submits usage with `modelId` `"legacy-model"`, `5,000` prompt tokens, and `5,000` completion tokens  
**Then** the system rejects the request with HTTP `400` and the user-facing message `"Model ID is not supported"`

### Quality Check Result
- **Structure and completeness**: Passed - all required sections are present.
- **Business clarity**: Passed - the story is focused on Premium plan customers and model-specific prompt and completion billing.
- **Sizing and independence**: Passed - the story contains three core functional points: Premium no-quota billing, separate prompt and completion charges, and model validation.
- **Final INVEST re-validation**: Passed - the story is independent, complete, valuable, estimable, right-sized, and testable.
