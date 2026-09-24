package com.kursi.settlementfunding.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FundingResponse(
        UUID requestId,
        List<CandidateInstruction> selectedInstructions,
        BigDecimal totalSettlementConsumed,
        BigDecimal totalExpectedFee,
        Instant createdAt



) {
}
