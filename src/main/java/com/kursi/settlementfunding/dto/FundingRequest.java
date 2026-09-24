package com.kursi.settlementfunding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record FundingRequest(

        @NotNull
        @PositiveOrZero
        BigDecimal availableSettlementBalance,

        @NotNull
        List<@NotNull @Valid CandidateInstruction> candidateInstructions
) {
}
