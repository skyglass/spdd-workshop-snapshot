#!/bin/bash
# =============================================================================
# API Test Script
# Generated for: Token Usage Billing API
# Source: spdd/prompt/GGQPA-XXX-202605160301-[Feat]-api-token-usage-billing.md
# =============================================================================
#
# Usage: ./scripts/test-api.sh [BASE_URL]
#        Default BASE_URL: http://localhost:8080
#
# Idempotency:
# - This script cleans synthetic API-TEST-* data before the first test, between
#   stateful tests, and at the end of execution.
# - It preserves Flyway baseline rows such as CUST-001/CUST-002/CUST-003,
#   pricing plans, and subscriptions.
# - It is safe to run multiple times against the same local environment.
#
# Requirements:
# - bash and curl for API execution
# - psql or Docker Compose for repeatable SQL setup/reset
# - Each request uses -m 10 timeout to prevent hanging
# - HTTP status captured via: -o /tmp/response.txt -w "%{http_code}"
#
# Optional database environment overrides:
#   DB_HOST=localhost DB_PORT=54323 DB_NAME=token_billing DB_USER=postgres DB_PASSWORD=postgres
#
# =============================================================================
#
# TEST CASE OVERVIEW (Human-Reviewable)
# =============================================================================
#
# VALIDATION ERROR TESTS
# +--------+-----------------------------+------------------+--------+------+-----+-------------------------------+
# | ID     | Description                 | Customer         | Prompt | Comp | HTTP| Expected Message              |
# +--------+-----------------------------+------------------+--------+------+-----+-------------------------------+
# | AC1    | Non-existent customer       | API-TEST-MISSING | 1000   | 500  | 404 | Customer not found            |
# | AC2.1  | Negative prompt tokens      | API-TEST-CUST    | -1     | 500  | 400 | Token count cannot be negative|
# | AC2.2  | Negative completion tokens  | API-TEST-CUST    | 1000   | -1   | 400 | Token count cannot be negative|
# | EDGE1  | Missing customerId          | -                | 1000   | 500  | 400 | Required field is missing     |
# | EDGE2  | Missing promptTokens        | API-TEST-CUST    | -      | 500  | 400 | Required field is missing     |
# | EDGE3  | Missing completionTokens    | API-TEST-CUST    | 1000   | -    | 400 | Required field is missing     |
# | EDGE4  | Invalid JSON body           | malformed        | -      | -    | 400 | Invalid request body          |
# | EDGE5  | Token total too large       | API-TEST-CUST    | maxint | 1    | 400 | Token total exceeds supported |
# | EDGE6  | No active subscription      | API-TEST-NOSUB   | 1000   | 500  | 409 | Active subscription not found |
# +--------+-----------------------------+------------------+--------+------+-----+-------------------------------+
#
# BILLING CALCULATION TESTS
# +------+--------------------------+---------------+-------+--------+-------+-----+----------+---------+--------+
# | ID   | Description              | Customer      | Prior | Prompt | Comp  | HTTP| QuotaUse | Overage | Charge |
# +------+--------------------------+---------------+-------+--------+-------+-----+----------+---------+--------+
# | AC3  | Within included quota    | API-TEST-CUST | 60000 | 20000  | 10000 | 201 | 30000    | 0       | 0.00   |
# | AC4  | Exceeds included quota   | API-TEST-CUST | 80000 | 30000  | 20000 | 201 | 20000    | 30000   | 0.60   |
# | AC5  | Successful bill details  | API-TEST-CUST | 0     | 1000   | 500   | 201 | 1500     | 0       | 0.00   |
# | EDGE7| Zero token submission    | API-TEST-CUST | 0     | 0      | 0     | 201 | 0        | 0       | 0.00   |
# +------+--------------------------+---------------+-------+--------+-------+-----+----------+---------+--------+
#
# =============================================================================

BASE_URL="${1:-http://localhost:8080}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-54323}"
DB_NAME="${DB_NAME:-token_billing}"
DB_USER="${DB_USER:-postgres}"
DB_PASSWORD="${DB_PASSWORD:-postgres}"

if [ -t 1 ]; then
    RED='\033[0;31m'
    GREEN='\033[0;32m'
    YELLOW='\033[1;33m'
    BLUE='\033[0;34m'
    CYAN='\033[0;36m'
    NC='\033[0m'
else
    RED=''
    GREEN=''
    YELLOW=''
    BLUE=''
    CYAN=''
    NC=''
fi

# -----------------------------------------------------------------------------
# SEED DATA REFERENCE
# -----------------------------------------------------------------------------
# Flyway baseline data preserved by this script:
# Customers:
#   - CUST-001: Acme Corp, PLAN-STARTER, quota 100000, rate 0.0200 per 1K
#   - CUST-002: TechStart Inc, PLAN-FREE, quota 10000, rate 0.0300 per 1K
#   - CUST-003: Enterprise Solutions Ltd, PLAN-ENTERPRISE, quota 2000000, rate 0.0100 per 1K
# Plans:
#   - PLAN-FREE: quota 10000, overage rate 0.0300
#   - PLAN-STARTER: quota 100000, overage rate 0.0200
#   - PLAN-PRO: quota 500000, overage rate 0.0150
#   - PLAN-ENTERPRISE: quota 2000000, overage rate 0.0100
#
# Synthetic data created and removed by this script:
#   - API-TEST-CUST with API-TEST-PLAN-STARTER
#   - API-TEST-NOSUB without an active subscription
#   - Bills for customer IDs matching API-TEST-%
# -----------------------------------------------------------------------------

TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

declare -a TEST_IDS
declare -a TEST_DESCRIPTIONS
declare -a EXPECTED_STATUS
declare -a ACTUAL_STATUS
declare -a TEST_RESULTS

print_test_header() {
    echo ""
    echo -e "${BLUE}===============================================================${NC}"
    echo -e "${BLUE}TEST: $1${NC}"
    echo -e "${BLUE}===============================================================${NC}"
}

print_expected() {
    echo -e "${YELLOW}Expected: $1${NC}"
}

print_result() {
    echo -e "${GREEN}Response:${NC}"
}

record_result() {
    TEST_IDS+=("$1")
    TEST_DESCRIPTIONS+=("$2")
    EXPECTED_STATUS+=("$3")
    ACTUAL_STATUS+=("$4")
    TEST_RESULTS+=("$5")
}

check_result() {
    local test_id="$1"
    local test_desc="$2"
    local expected_status="$3"
    local actual_status="$4"
    local body="$5"
    shift 5

    local result="PASS"
    local actual_summary="HTTP ${actual_status}"
    local missing_fragment

    echo "$body"
    echo ""

    if [ "$actual_status" != "$expected_status" ]; then
        result="FAIL"
    fi

    for expected_fragment in "$@"; do
        if [[ "$body" != *"$expected_fragment"* ]]; then
            result="FAIL"
            missing_fragment="$expected_fragment"
            echo -e "${RED}Missing response fragment: ${missing_fragment}${NC}"
        fi
    done

    if [ "$result" = "PASS" ]; then
        echo -e "${GREEN}PASSED${NC} [HTTP Status: $actual_status]"
        TESTS_PASSED=$((TESTS_PASSED + 1))
        record_result "$test_id" "$test_desc" "HTTP $expected_status" "$actual_summary" "PASS"
    else
        echo -e "${RED}FAILED${NC} [HTTP Status: $actual_status, Expected: $expected_status]"
        TESTS_FAILED=$((TESTS_FAILED + 1))
        record_result "$test_id" "$test_desc" "HTTP $expected_status" "$actual_summary" "FAIL"
    fi
    echo ""
}

print_results_table() {
    echo ""
    echo -e "${CYAN}+----------+--------------------------------+----------+----------+----------+${NC}"
    echo -e "${CYAN}| Test ID  | Description                    | Expected | Actual   | Result   |${NC}"
    echo -e "${CYAN}+----------+--------------------------------+----------+----------+----------+${NC}"

    for i in "${!TEST_IDS[@]}"; do
        local result_color="${GREEN}"
        if [ "${TEST_RESULTS[$i]}" = "FAIL" ]; then
            result_color="${RED}"
        fi
        printf "${CYAN}|${NC} %-8s ${CYAN}|${NC} %-30s ${CYAN}|${NC} %-8s ${CYAN}|${NC} %-8s ${CYAN}|${NC} ${result_color}%-8s${NC} ${CYAN}|${NC}\n" \
            "${TEST_IDS[$i]}" \
            "${TEST_DESCRIPTIONS[$i]:0:30}" \
            "${EXPECTED_STATUS[$i]}" \
            "${ACTUAL_STATUS[$i]}" \
            "${TEST_RESULTS[$i]}"
    done

    echo -e "${CYAN}+----------+--------------------------------+----------+----------+----------+${NC}"
}

run_sql() {
    local sql="$1"

    if command -v psql >/dev/null 2>&1; then
        PGPASSWORD="${DB_PASSWORD}" psql \
            -v ON_ERROR_STOP=1 \
            -h "${DB_HOST}" \
            -p "${DB_PORT}" \
            -U "${DB_USER}" \
            -d "${DB_NAME}" \
            -q \
            -c "${sql}" >/dev/null
        return $?
    fi

    if command -v docker >/dev/null 2>&1 && [ -f compose.yaml ]; then
        docker compose exec -T postgres psql \
            -v ON_ERROR_STOP=1 \
            -U "${DB_USER}" \
            -d "${DB_NAME}" \
            -q \
            -c "${sql}" >/dev/null
        return $?
    fi

    echo -e "${RED}No SQL reset mechanism found. Install psql or run with Docker Compose available.${NC}"
    return 1
}

cleanup_test_data() {
    run_sql "
BEGIN;
DELETE FROM bills WHERE customer_id LIKE 'API-TEST-%';
DELETE FROM customer_subscriptions WHERE customer_id LIKE 'API-TEST-%' OR plan_id LIKE 'API-TEST-%';
DELETE FROM customers WHERE id LIKE 'API-TEST-%';
DELETE FROM pricing_plans WHERE id LIKE 'API-TEST-%';
COMMIT;
"
}

setup_test_data() {
    run_sql "
BEGIN;
INSERT INTO pricing_plans (id, name, monthly_quota, overage_rate_per_1k)
VALUES ('API-TEST-PLAN-STARTER', 'API Test Starter', 100000, 0.0200)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    monthly_quota = EXCLUDED.monthly_quota,
    overage_rate_per_1k = EXCLUDED.overage_rate_per_1k;

INSERT INTO customers (id, name)
VALUES
    ('API-TEST-CUST', 'API Test Customer'),
    ('API-TEST-NOSUB', 'API Test Customer Without Subscription')
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name;

INSERT INTO customer_subscriptions (id, customer_id, plan_id, effective_from, effective_to)
VALUES ('11111111-1111-1111-1111-111111111111', 'API-TEST-CUST', 'API-TEST-PLAN-STARTER', DATE '2020-01-01', NULL)
ON CONFLICT (id) DO UPDATE
SET customer_id = EXCLUDED.customer_id,
    plan_id = EXCLUDED.plan_id,
    effective_from = EXCLUDED.effective_from,
    effective_to = EXCLUDED.effective_to;
COMMIT;
"
}

prepare_clean_state() {
    echo -e "${CYAN}Resetting synthetic test data...${NC}"
    if ! cleanup_test_data; then
        echo -e "${RED}Failed to clean synthetic test data.${NC}"
        exit 1
    fi
    if ! setup_test_data; then
        echo -e "${RED}Failed to create synthetic test data.${NC}"
        exit 1
    fi
}

insert_prior_usage() {
    local bill_id="$1"
    local prompt_tokens="$2"
    local completion_tokens="$3"
    local total_tokens="$4"
    local included_tokens="$5"
    local overage_tokens="$6"
    local total_charge="$7"

    run_sql "
INSERT INTO bills (
    id,
    customer_id,
    prompt_tokens,
    completion_tokens,
    total_tokens,
    included_tokens_used,
    overage_tokens,
    total_charge,
    calculated_at
)
VALUES (
    '${bill_id}',
    'API-TEST-CUST',
    ${prompt_tokens},
    ${completion_tokens},
    ${total_tokens},
    ${included_tokens},
    ${overage_tokens},
    ${total_charge},
    NOW() AT TIME ZONE 'UTC'
);
"
}

final_cleanup() {
    cleanup_test_data >/dev/null 2>&1 || true
    rm -f /tmp/response.txt
}

trap final_cleanup EXIT

echo ""
echo -e "${BLUE}===============================================================${NC}"
echo -e "${BLUE}TOKEN USAGE BILLING API TESTS${NC}"
echo -e "${BLUE}===============================================================${NC}"
echo "Base URL: ${BASE_URL}"
echo "Started at: $(date)"
echo ""

prepare_clean_state

# -----------------------------------------------------------------------------
# AC1: Validate Customer ID exists
# -----------------------------------------------------------------------------
TEST_ID="AC1"
TEST_DESC="Non-existent customer"
EXPECTED="404"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Customer not found"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-MISSING","promptTokens":1000,"completionTokens":500}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"CUSTOMER_NOT_FOUND"' \
    '"message":"Customer not found"' \
    '"path":"/api/usage"'

# -----------------------------------------------------------------------------
# AC2.1: Validate prompt tokens are non-negative
# -----------------------------------------------------------------------------
TEST_ID="AC2.1"
TEST_DESC="Negative prompt tokens"
EXPECTED="400"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Token count cannot be negative"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":-1,"completionTokens":500}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"VALIDATION_ERROR"' \
    '"message":"Token count cannot be negative"'

# -----------------------------------------------------------------------------
# AC2.2: Validate completion tokens are non-negative
# -----------------------------------------------------------------------------
TEST_ID="AC2.2"
TEST_DESC="Negative completion tokens"
EXPECTED="400"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Token count cannot be negative"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":1000,"completionTokens":-1}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"VALIDATION_ERROR"' \
    '"message":"Token count cannot be negative"'

# -----------------------------------------------------------------------------
# EDGE1: Missing customerId
# -----------------------------------------------------------------------------
TEST_ID="EDGE1"
TEST_DESC="Missing customerId"
EXPECTED="400"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Required field is missing"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"promptTokens":1000,"completionTokens":500}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"VALIDATION_ERROR"' \
    '"message":"Required field is missing"'

# -----------------------------------------------------------------------------
# EDGE2: Missing promptTokens
# -----------------------------------------------------------------------------
TEST_ID="EDGE2"
TEST_DESC="Missing promptTokens"
EXPECTED="400"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Required field is missing"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","completionTokens":500}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"VALIDATION_ERROR"' \
    '"message":"Required field is missing"'

# -----------------------------------------------------------------------------
# EDGE3: Missing completionTokens
# -----------------------------------------------------------------------------
TEST_ID="EDGE3"
TEST_DESC="Missing completionTokens"
EXPECTED="400"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Required field is missing"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":1000}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"VALIDATION_ERROR"' \
    '"message":"Required field is missing"'

# -----------------------------------------------------------------------------
# EDGE4: Invalid JSON body
# -----------------------------------------------------------------------------
TEST_ID="EDGE4"
TEST_DESC="Invalid JSON body"
EXPECTED="400"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Invalid request body"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"INVALID_REQUEST_BODY"' \
    '"message":"Invalid request body"'

# -----------------------------------------------------------------------------
# EDGE5: Token total exceeds integer schema capacity
# -----------------------------------------------------------------------------
TEST_ID="EDGE5"
TEST_DESC="Token total too large"
EXPECTED="400"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Token total exceeds supported limit"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":2147483647,"completionTokens":1}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"TOKEN_TOTAL_TOO_LARGE"' \
    '"message":"Token total exceeds supported limit"'

# -----------------------------------------------------------------------------
# EDGE6: Customer exists but has no active subscription
# -----------------------------------------------------------------------------
TEST_ID="EDGE6"
TEST_DESC="No active subscription"
EXPECTED="409"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with message Active subscription not found"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-NOSUB","promptTokens":1000,"completionTokens":500}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"errorCode":"ACTIVE_SUBSCRIPTION_NOT_FOUND"' \
    '"message":"Active subscription not found"'

# -----------------------------------------------------------------------------
# AC3: Bill within included quota
# -----------------------------------------------------------------------------
prepare_clean_state
if ! insert_prior_usage "22222222-2222-2222-2222-222222222222" 60000 0 60000 60000 0 0.00; then
    echo -e "${RED}Failed to insert AC3 prior usage fixture.${NC}"
    exit 1
fi

TEST_ID="AC3"
TEST_DESC="Within included quota"
EXPECTED="201"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with totalTokens=30000, tokensFromQuota=30000, overageTokens=0, totalCharge=0.00"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":20000,"completionTokens":10000}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"customerId":"API-TEST-CUST"' \
    '"totalTokens":30000' \
    '"tokensFromQuota":30000' \
    '"overageTokens":0' \
    '"totalCharge":0.00' \
    '"billId":' \
    '"calculationTimestamp":'

# -----------------------------------------------------------------------------
# AC4: Bill exceeding included quota
# -----------------------------------------------------------------------------
prepare_clean_state
if ! insert_prior_usage "33333333-3333-3333-3333-333333333333" 80000 0 80000 80000 0 0.00; then
    echo -e "${RED}Failed to insert AC4 prior usage fixture.${NC}"
    exit 1
fi

TEST_ID="AC4"
TEST_DESC="Exceeds included quota"
EXPECTED="201"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with totalTokens=50000, tokensFromQuota=20000, overageTokens=30000, totalCharge=0.60"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":30000,"completionTokens":20000}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"customerId":"API-TEST-CUST"' \
    '"totalTokens":50000' \
    '"tokensFromQuota":20000' \
    '"overageTokens":30000' \
    '"totalCharge":0.60' \
    '"billId":' \
    '"calculationTimestamp":'

# -----------------------------------------------------------------------------
# AC5: Successful return includes bill details
# -----------------------------------------------------------------------------
prepare_clean_state

TEST_ID="AC5"
TEST_DESC="Successful bill details"
EXPECTED="201"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with all required bill response fields"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":1000,"completionTokens":500}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"billId":' \
    '"customerId":"API-TEST-CUST"' \
    '"totalTokens":1500' \
    '"tokensFromQuota":1500' \
    '"overageTokens":0' \
    '"totalCharge":0.00' \
    '"calculationTimestamp":'

# -----------------------------------------------------------------------------
# EDGE7: Zero-token submissions are valid and create zero-charge bills
# -----------------------------------------------------------------------------
prepare_clean_state

TEST_ID="EDGE7"
TEST_DESC="Zero token submission"
EXPECTED="201"
TESTS_TOTAL=$((TESTS_TOTAL + 1))
print_test_header "$TEST_ID: $TEST_DESC"
print_expected "HTTP $EXPECTED with zero total, quota, overage, and charge"
print_result
HTTP_CODE=$(curl -s -o /tmp/response.txt -w "%{http_code}" -X POST "${BASE_URL}/api/usage" \
    -H "Content-Type: application/json" \
    -m 10 \
    -d '{"customerId":"API-TEST-CUST","promptTokens":0,"completionTokens":0}')
BODY=$(cat /tmp/response.txt)
check_result "$TEST_ID" "$TEST_DESC" "$EXPECTED" "$HTTP_CODE" "$BODY" \
    '"customerId":"API-TEST-CUST"' \
    '"totalTokens":0' \
    '"tokensFromQuota":0' \
    '"overageTokens":0' \
    '"totalCharge":0.00' \
    '"billId":' \
    '"calculationTimestamp":'

# -----------------------------------------------------------------------------
# TEST SUMMARY
# -----------------------------------------------------------------------------
echo ""
echo -e "${BLUE}===============================================================${NC}"
echo -e "${BLUE}TEST EXECUTION COMPLETE${NC}"
echo -e "${BLUE}===============================================================${NC}"
echo ""
echo "Base URL: ${BASE_URL}"
echo "Finished at: $(date)"
echo ""

print_results_table

echo ""
echo -e "Tests Passed: ${GREEN}${TESTS_PASSED}${NC}"
echo -e "Tests Failed: ${RED}${TESTS_FAILED}${NC}"
echo -e "Total Tests:  ${TESTS_TOTAL}"
echo ""

if [ "$TESTS_TOTAL" -gt 0 ]; then
    PASS_RATE=$((TESTS_PASSED * 100 / TESTS_TOTAL))
    if [ "$TESTS_FAILED" -eq 0 ]; then
        echo -e "${GREEN}All tests passed (${PASS_RATE}%).${NC}"
    else
        echo -e "${RED}Some tests failed (${PASS_RATE}% passed).${NC}"
    fi
fi
echo ""

if [ "$TESTS_FAILED" -gt 0 ]; then
    exit 1
fi
