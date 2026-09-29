package com.kursi.settlementfunding.controller;

import com.kursi.settlementfunding.dto.FundingRequest;
import com.kursi.settlementfunding.dto.FundingResponse;
import com.kursi.settlementfunding.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

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
