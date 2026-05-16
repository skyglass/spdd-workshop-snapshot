package org.tw.token_billing.service;

import org.tw.token_billing.controller.dto.BillResponse;
import org.tw.token_billing.controller.dto.UsageRequest;

public interface UsageService {
    BillResponse submitUsage(UsageRequest request);
}
