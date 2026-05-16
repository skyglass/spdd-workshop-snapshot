package org.tw.token_billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsageControllerIntegrationTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-16T12:00:00Z");
    private static final LocalDateTime PRIOR_USAGE_TIMESTAMP = LocalDateTime.of(2026, 5, 10, 9, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
        jdbcTemplate.update("delete from bills");
    }

    @Test
    void should_return_customer_not_found_when_submit_usage_given_unknown_customer() throws Exception {
        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "UNKNOWN",
                                "promptTokens", 1000,
                                "completionTokens", 500
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CUSTOMER_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Customer not found"))
                .andExpect(jsonPath("$.path").value("/api/usage"));
    }

    @Test
    void should_return_validation_error_when_submit_usage_given_negative_prompt_tokens() throws Exception {
        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "CUST-001",
                                "promptTokens", -1,
                                "completionTokens", 500
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Token count cannot be negative"));
    }

    @Test
    void should_return_validation_error_when_submit_usage_given_negative_completion_tokens() throws Exception {
        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "CUST-001",
                                "promptTokens", 1000,
                                "completionTokens", -1
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Token count cannot be negative"));
    }

    @Test
    void should_return_zero_charge_bill_when_submit_usage_given_usage_within_remaining_quota() throws Exception {
        insertPriorBill("11111111-1111-1111-1111-111111111111", 60000, 0, 60000, 60000, 0, "0.00");

        MvcResult result = mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "CUST-001",
                                "promptTokens", 20000,
                                "completionTokens", 10000
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billId").exists())
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.totalTokens").value(30000))
                .andExpect(jsonPath("$.tokensFromQuota").value(30000))
                .andExpect(jsonPath("$.overageTokens").value(0))
                .andExpect(jsonPath("$.totalCharge").value(0.0))
                .andExpect(jsonPath("$.calculationTimestamp").value("2026-05-16T12:00:00"))
                .andReturn();

        assertPersistedBill(result, 30000, 30000, 0, "0.00");
    }

    @Test
    void should_return_overage_charge_when_submit_usage_given_usage_exceeds_remaining_quota() throws Exception {
        insertPriorBill("22222222-2222-2222-2222-222222222222", 80000, 0, 80000, 80000, 0, "0.00");

        MvcResult result = mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "CUST-001",
                                "promptTokens", 30000,
                                "completionTokens", 20000
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billId").exists())
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.totalTokens").value(50000))
                .andExpect(jsonPath("$.tokensFromQuota").value(20000))
                .andExpect(jsonPath("$.overageTokens").value(30000))
                .andExpect(jsonPath("$.totalCharge").value(0.6))
                .andExpect(jsonPath("$.calculationTimestamp").value("2026-05-16T12:00:00"))
                .andReturn();

        assertPersistedBill(result, 50000, 20000, 30000, "0.60");
    }

    @Test
    void should_return_bill_details_when_submit_usage_given_valid_request() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "CUST-001",
                                "promptTokens", 1000,
                                "completionTokens", 500
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billId").exists())
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.totalTokens").value(1500))
                .andExpect(jsonPath("$.tokensFromQuota").value(1500))
                .andExpect(jsonPath("$.overageTokens").value(0))
                .andExpect(jsonPath("$.totalCharge").value(0.0))
                .andExpect(jsonPath("$.calculationTimestamp").value("2026-05-16T12:00:00"))
                .andReturn();

        assertPersistedBill(result, 1500, 1500, 0, "0.00");
        Integer billCount = jdbcTemplate.queryForObject("select count(*) from bills", Integer.class);
        assertThat(billCount).isEqualTo(1);
    }

    private void insertPriorBill(
            String id,
            int promptTokens,
            int completionTokens,
            int totalTokens,
            int includedTokensUsed,
            int overageTokens,
            String totalCharge
    ) {
        jdbcTemplate.update("""
                        insert into bills (
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
                        values (?, 'CUST-001', ?, ?, ?, ?, ?, ?, ?)
                        """,
                UUID.fromString(id),
                promptTokens,
                completionTokens,
                totalTokens,
                includedTokensUsed,
                overageTokens,
                new BigDecimal(totalCharge),
                Timestamp.valueOf(PRIOR_USAGE_TIMESTAMP)
        );
    }

    private void assertPersistedBill(
            MvcResult result,
            int totalTokens,
            int includedTokensUsed,
            int overageTokens,
            String totalCharge
    ) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID billId = UUID.fromString(response.get("billId").asText());
        Map<String, Object> row = jdbcTemplate.queryForMap(
                """
                        select total_tokens, included_tokens_used, overage_tokens, total_charge
                        from bills
                        where id = ?
                        """,
                billId
        );
        assertThat(row.get("total_tokens")).isEqualTo(totalTokens);
        assertThat(row.get("included_tokens_used")).isEqualTo(includedTokensUsed);
        assertThat(row.get("overage_tokens")).isEqualTo(overageTokens);
        assertThat((BigDecimal) row.get("total_charge")).isEqualByComparingTo(totalCharge);
    }
}
