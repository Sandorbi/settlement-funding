package com.kursi.settlementfunding.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record FundingRequest(

        @NotNull
        @PositiveOrZero
        @Digits(integer = 16, fraction = 4)
        BigDecimal availableSettlementBalance,

        @NotNull
        List<@NotNull @Valid CandidateInstruction> candidateInstructions
) {
}
