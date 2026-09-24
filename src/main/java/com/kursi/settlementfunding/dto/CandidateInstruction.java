package com.kursi.settlementfunding.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CandidateInstruction(
        @NotBlank
        @Size(max = 50)
        String instructionReference,

        @NotNull
        @Positive
        BigDecimal instructionAmount,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 16, fraction = 4)
        BigDecimal expectedFee
) {
}
