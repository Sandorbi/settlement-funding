package com.kursi.settlementfunding.algorithm;

import java.math.BigDecimal;
import java.util.List;

public record FundingSelection(
        List<Integer> selectedIndexes,
        BigDecimal totalSettlementConsumed,
        BigDecimal totalExpectedFee
) {
    public FundingSelection {
        selectedIndexes = List.copyOf(selectedIndexes);
    }
}
