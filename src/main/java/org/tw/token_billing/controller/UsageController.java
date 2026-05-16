package org.tw.token_billing.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.tw.token_billing.controller.dto.BillResponse;
import org.tw.token_billing.controller.dto.UsageRequest;
import org.tw.token_billing.service.UsageService;

@RestController
@RequestMapping("/api/usage")
public class UsageController {
    private final UsageService usageService;

    public UsageController(UsageService usageService) {
        this.usageService = usageService;
    }

    @PostMapping
    public ResponseEntity<BillResponse> submitUsage(@Valid @RequestBody UsageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usageService.submitUsage(request));
    }
}
