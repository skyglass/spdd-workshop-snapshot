package org.tw.token_billing.controller.dto;

import java.time.Instant;

public record ErrorResponse(
        String errorCode,
        String message,
        Instant timestamp,
        String path
) {
}
