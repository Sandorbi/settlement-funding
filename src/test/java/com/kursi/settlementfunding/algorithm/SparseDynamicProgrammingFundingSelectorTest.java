package com.kursi.settlementfunding.algorithm;

import com.kursi.settlementfunding.dto.CandidateInstruction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SparseDynamicProgrammingFundingSelectorTest {

    private final FundingSelector selector = new SparseDynamicProgrammingFundingSelector();

    @Test
    void shouldSelectOptimalCombination() {
        List<CandidateInstruction> candidates = List.of(
                candidate("INS-2001", "7000", "150"),
                candidate("INS-2002", "9000", "210"),
                candidate("INS-2003", "4000", "90"),
                candidate("INS-2004", "6000", "130")
        );

        FundingSelection result =
                selector.select(new BigDecimal("20000"), candidates);

        assertThat(result.selectedIndexes()).containsExactly(0, 1, 2);
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("20000");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("450");
    }

    @Test
    void shouldReturnEmptySelectionWhenCandidatesAreEmpty() {
        List<CandidateInstruction> candidates = List.of();

        FundingSelection result =
                selector.select(new BigDecimal("100"), candidates);

        assertThat(result.selectedIndexes()).isEmpty();
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("0");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("0");
    }

    @Test
    void shouldReturnEmptySelectionWhenNothingFits() {
        List<CandidateInstruction> candidates = List.of(
                candidate("INS-1", "11", "5"),
                candidate("INS-2", "12", "6")
        );

        FundingSelection result =
                selector.select(new BigDecimal("10"), candidates);

        assertThat(result.selectedIndexes()).isEmpty();
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("0");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("0");
    }

    @Test
    void shouldReturnEmptySelectionWhenBalanceIsZero() {
        List<CandidateInstruction> candidates = List.of(
                candidate("INS-1", "1", "5")
        );

        FundingSelection result =
                selector.select(new BigDecimal("0"), candidates);

        assertThat(result.selectedIndexes()).isEmpty();
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("0");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("0");
    }

    @Test
    void shouldHandleDecimalsAndFitBalanceExactly() {
        List<CandidateInstruction> candidates = List.of(
                candidate("INS-1", "0.1001", "0.0101"),
                candidate("INS-2", "0.2002", "0.0202"),
                candidate("INS-3", "0.3003", "0.0300")
        );

        FundingSelection result =
                selector.select(new BigDecimal("0.3003"), candidates);

        assertThat(result.selectedIndexes()).containsExactly(0, 1);
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("0.3003");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("0.0303");
    }

    @Test
    void shouldPreferLowerConsumptionWhenFeesAreEqual() {
        List<CandidateInstruction> candidates = List.of(
                candidate("INS-1", "8", "10"),
                candidate("INS-2", "6", "10")
        );

        FundingSelection result =
                selector.select(new BigDecimal("10"), candidates);

        assertThat(result.selectedIndexes()).containsExactly(1);
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("6");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("10");
    }

    @Test
    void shouldNotSelectAnInstructionMoreThanOnce() {
        List<CandidateInstruction> candidates = List.of(
                candidate("INS-1", "3", "5")
        );

        FundingSelection result =
                selector.select(new BigDecimal("9"), candidates);

        assertThat(result.selectedIndexes()).containsExactly(0);
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("3");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("5");
    }

    @Test
    void shouldTreatDuplicateReferencesAsSeparateCandidates() {
        List<CandidateInstruction> candidates = List.of(
                candidate("SAME-REFERENCE", "4", "7"),
                candidate("SAME-REFERENCE", "6", "8")
        );

        FundingSelection result =
                selector.select(new BigDecimal("10"), candidates);

        assertThat(result.selectedIndexes()).containsExactly(0, 1);
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("10");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("15");
    }

    @Test
    void shouldKeepHigherFeeForNumericallyEqualAmounts() {
        List<CandidateInstruction> candidates = List.of(
                candidate("INS-1", "5.0", "7"),
                candidate("INS-2", "5.0000", "9")
        );

        FundingSelection result =
                selector.select(new BigDecimal("5"), candidates);

        assertThat(result.selectedIndexes()).containsExactly(1);
        assertThat(result.totalSettlementConsumed())
                .isEqualByComparingTo("5");
        assertThat(result.totalExpectedFee())
                .isEqualByComparingTo("9");
    }

    private CandidateInstruction candidate(
            String reference,
            String amount,
            String fee
    ) {
        return new CandidateInstruction(
                reference,
                new BigDecimal(amount),
                new BigDecimal(fee)
        );
    }
}

