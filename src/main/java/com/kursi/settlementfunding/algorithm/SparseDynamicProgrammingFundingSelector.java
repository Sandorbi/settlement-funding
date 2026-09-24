package com.kursi.settlementfunding.algorithm;

import com.kursi.settlementfunding.dto.CandidateInstruction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SparseDynamicProgrammingFundingSelector implements FundingSelector {

    private record State(
            BigDecimal fee,
            List<Integer> selectedIndexes
    ) {}

    @Override
    public FundingSelection select(
            BigDecimal availableBalance,
            List<CandidateInstruction> candidates
    ) {
        TreeMap<BigDecimal, State> states = new TreeMap<>();
        states.put(BigDecimal.ZERO, new State(BigDecimal.ZERO, List.of()));

        for (int index = 0; index < candidates.size(); index++) {
            CandidateInstruction candidate = candidates.get(index);

            TreeMap<BigDecimal, State> next = new TreeMap<>(states);

            // Reads only previous combinations so each candidate is used once.
            for (Map.Entry<BigDecimal, State> entry : states.entrySet()) {
                BigDecimal amount = entry.getKey()
                        .add(candidate.instructionAmount());

                if (amount.compareTo(availableBalance) > 0) {
                    continue;
                }

                BigDecimal fee = entry.getValue().fee()
                        .add(candidate.expectedFee());
                State existing = next.get(amount);

                // For equal amounts, keep the combination that earns more.
                if (existing == null || fee.compareTo(existing.fee()) > 0) {
                    List<Integer> selectedIndexes = new ArrayList<>(
                            entry.getValue().selectedIndexes()
                    );
                    selectedIndexes.add(index);
                    next.put(amount, new State(fee, selectedIndexes));
                }
            }

            states = keepUsefulCombinations(next);
        }

        // After pruning, fees strictly increase with consumed amount.
        Map.Entry<BigDecimal, State> best = states.lastEntry();
        return new FundingSelection(
                best.getValue().selectedIndexes(),
                best.getKey(),
                best.getValue().fee()
        );
    }

    private TreeMap<BigDecimal, State> keepUsefulCombinations(
            TreeMap<BigDecimal, State> combinations
    ) {
        TreeMap<BigDecimal, State> useful = new TreeMap<>();
        BigDecimal highestFee = null;

        // Entries are ordered from lowest to highest consumed amount.
        for (Map.Entry<BigDecimal, State> entry : combinations.entrySet()) {
            BigDecimal fee = entry.getValue().fee();

            // Keep only combinations earning more than all cheaper ones.
            if (highestFee == null || fee.compareTo(highestFee) > 0) {
                useful.put(entry.getKey(), entry.getValue());
                highestFee = fee;
            }
        }

        return useful;
    }
}
