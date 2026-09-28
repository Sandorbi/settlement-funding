package com.kursi.settlementfunding.service;

import com.kursi.settlementfunding.algorithm.FundingSelection;
import com.kursi.settlementfunding.algorithm.FundingSelector;
import com.kursi.settlementfunding.dto.CandidateInstruction;
import com.kursi.settlementfunding.dto.FundingRequest;
import com.kursi.settlementfunding.dto.FundingResponse;
import com.kursi.settlementfunding.entity.SettlementInstruction;
import com.kursi.settlementfunding.entity.SettlementRun;
import com.kursi.settlementfunding.repository.SettlementInstructionRepository;
import com.kursi.settlementfunding.repository.SettlementRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final FundingSelector fundingSelector;
    private final SettlementRunRepository runRepository;
    private final SettlementInstructionRepository instructionRepository;

    @Transactional
    public FundingResponse fund(FundingRequest request) {
        FundingSelection selection = fundingSelector.select(
                request.availableSettlementBalance(),
                request.candidateInstructions()
        );

        SettlementRun run = new SettlementRun(
                request.availableSettlementBalance(),
                selection.totalSettlementConsumed(),
                selection.totalExpectedFee()
        );

        run = runRepository.saveAndFlush(run);

        Set<Integer> selectedIndexes =
                new HashSet<>(selection.selectedIndexes());

        List<SettlementInstruction> instructions = new ArrayList<>();
        List<CandidateInstruction> selectedInstructions = new ArrayList<>();

        List<CandidateInstruction> candidates = request.candidateInstructions();

        for (int index = 0; index < candidates.size(); index++) {
            CandidateInstruction candidate = candidates.get(index);
            boolean selected = selectedIndexes.contains(index);

            instructions.add(new SettlementInstruction(
                    run,
                    candidate.instructionReference(),
                    candidate.instructionAmount(),
                    candidate.expectedFee(),
                    selected
            ));

            if (selected) {
                selectedInstructions.add(candidate);
            }
        }

        instructionRepository.saveAll(instructions);

        return new FundingResponse(
                run.getId(),
                List.copyOf(selectedInstructions),
                run.getTotalSettlementConsumed(),
                run.getTotalExpectedFee(),
                run.getCreatedAt()
        );
    }
}
