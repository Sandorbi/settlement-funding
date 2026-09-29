package com.kursi.settlementfunding.controller;

import com.kursi.settlementfunding.dto.FundingRequest;
import com.kursi.settlementfunding.dto.FundingHistoryResponse;
import com.kursi.settlementfunding.dto.FundingResponse;
import com.kursi.settlementfunding.service.SettlementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public ResponseEntity<FundingHistoryResponse> getFundingHistory(
            @RequestParam(defaultValue = "0")
            @Min(value = 0, message = "page must be greater than or equal to 0")
            int page,

            @RequestParam(defaultValue = "20")
            @Min(value = 1, message = "size must be at least 1")
            @Max(value = 100, message = "size must not exceed 100")
            int size
    ) {
        return ResponseEntity.ok(settlementService.getFundingHistory(page, size));
    }

    @GetMapping("/{requestId}")
    public ResponseEntity<FundingResponse> getFundingRunById(@PathVariable UUID requestId) {
        return ResponseEntity.ok(settlementService.getById(requestId));
    }

    @PostMapping("/fund")
    public ResponseEntity<FundingResponse> fund(
            @Valid @RequestBody FundingRequest request
    ) {
        SettlementService.FundingOutcome outcome = settlementService.fund(request);

        HttpStatus status = outcome.nothingFits()
                ? HttpStatus.OK
                : HttpStatus.CREATED;

        return ResponseEntity.status(status).body(outcome.response());
    }
}
