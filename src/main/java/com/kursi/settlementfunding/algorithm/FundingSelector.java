package com.kursi.settlementfunding.algorithm;

import com.kursi.settlementfunding.dto.CandidateInstruction;

import java.math.BigDecimal;
import java.util.List;

public interface FundingSelector {
    FundingSelection select(
            BigDecimal availableBalance,
            List<CandidateInstruction> candidates
    );
}
