package org.tw.token_billing.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UsageRequest(
        @NotBlank String customerId,
        @NotNull @Min(value = 0, message = "Token count cannot be negative") Integer promptTokens,
        @NotNull @Min(value = 0, message = "Token count cannot be negative") Integer completionTokens
) {
}
