package org.tw.token_billing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.tw.token_billing.controller.dto.BillResponse;
import org.tw.token_billing.controller.dto.UsageRequest;
import org.tw.token_billing.exception.CustomerNotFoundException;
import org.tw.token_billing.service.UsageService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsageController.class)
class UsageControllerTest {
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-16T12:00:00Z");
    private static final UUID BILL_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UsageService usageService;

    @MockBean
    private Clock clock;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(FIXED_INSTANT);
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @Test
    void should_return_created_bill_response_when_submit_usage_given_valid_request() throws Exception {
        when(usageService.submitUsage(any(UsageRequest.class))).thenReturn(new BillResponse(
                BILL_ID,
                "CUST-001",
                1500,
                1500,
                0,
                new BigDecimal("0.00"),
                LocalDateTime.of(2026, 5, 16, 12, 0)
        ));

        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "customerId", "CUST-001",
                                "promptTokens", 1000,
                                "completionTokens", 500
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.billId").value(BILL_ID.toString()))
                .andExpect(jsonPath("$.customerId").value("CUST-001"))
                .andExpect(jsonPath("$.totalTokens").value(1500))
                .andExpect(jsonPath("$.tokensFromQuota").value(1500))
                .andExpect(jsonPath("$.overageTokens").value(0))
                .andExpect(jsonPath("$.totalCharge").value(0.0))
                .andExpect(jsonPath("$.calculationTimestamp").value("2026-05-16T12:00:00"));

        ArgumentCaptor<UsageRequest> requestCaptor = ArgumentCaptor.forClass(UsageRequest.class);
        verify(usageService).submitUsage(requestCaptor.capture());
        assertThat(requestCaptor.getValue().customerId()).isEqualTo("CUST-001");
        assertThat(requestCaptor.getValue().promptTokens()).isEqualTo(1000);
        assertThat(requestCaptor.getValue().completionTokens()).isEqualTo(500);
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
                .andExpect(jsonPath("$.message").value("Token count cannot be negative"))
                .andExpect(jsonPath("$.path").value("/api/usage"));

        verifyNoInteractions(usageService);
    }

    @Test
    void should_return_validation_error_when_submit_usage_given_missing_customer_id() throws Exception {
        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "promptTokens", 1000,
                                "completionTokens", 500
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("Required field is missing"))
                .andExpect(jsonPath("$.path").value("/api/usage"));

        verifyNoInteractions(usageService);
    }

    @Test
    void should_return_invalid_request_body_when_submit_usage_given_malformed_json() throws Exception {
        mockMvc.perform(post("/api/usage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_REQUEST_BODY"))
                .andExpect(jsonPath("$.message").value("Invalid request body"))
                .andExpect(jsonPath("$.path").value("/api/usage"));

        verifyNoInteractions(usageService);
    }

    @Test
    void should_return_customer_not_found_when_submit_usage_given_unknown_customer() throws Exception {
        when(usageService.submitUsage(any(UsageRequest.class))).thenThrow(new CustomerNotFoundException());

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
}
