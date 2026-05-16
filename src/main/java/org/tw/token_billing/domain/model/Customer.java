package org.tw.token_billing.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

public record Customer(
        String id,
        String name,
        LocalDateTime createdAt
) {
    public Customer {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
    }
}
