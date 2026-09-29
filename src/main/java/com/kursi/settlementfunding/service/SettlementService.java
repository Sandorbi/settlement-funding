package com.kursi.settlementfunding.service;

import com.kursi.settlementfunding.algorithm.FundingSelection;
import com.kursi.settlementfunding.algorithm.FundingSelector;
import com.kursi.settlementfunding.dto.CandidateInstruction;
import com.kursi.settlementfunding.dto.FundingHistoryResponse;
import com.kursi.settlementfunding.dto.FundingRequest;
import com.kursi.settlementfunding.dto.FundingResponse;
import com.kursi.settlementfunding.entity.SettlementInstruction;
import com.kursi.settlementfunding.entity.SettlementRun;
import com.kursi.settlementfunding.exception.FundingLimitExceededException;
import com.kursi.settlementfunding.exception.FundingRunNotFoundException;
import com.kursi.settlementfunding.exception.PaginationLimitExceededException;
import com.kursi.settlementfunding.repository.SettlementInstructionRepository;
import com.kursi.settlementfunding.repository.SettlementRunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private static final BigDecimal MAX_TOTAL_FEE =
            new BigDecimal("9999999999999999.9999");

    public record FundingOutcome(
            FundingResponse response,
            boolean nothingFits
    ) {}

    private final FundingSelector fundingSelector;
    private final SettlementRunRepository runRepository;
    private final SettlementInstructionRepository instructionRepository;

    @Transactional(readOnly = true)
    public FundingResponse getById(UUID requestId) {
        SettlementRun run = runRepository.findById(requestId)
                .orElseThrow(() -> new FundingRunNotFoundException(requestId));

        List<SettlementInstruction> instructions =
                instructionRepository.findAllByRun_IdAndSelectedTrue(requestId);

        List<CandidateInstruction> selectedInstructions = new ArrayList<>();

        for (SettlementInstruction instruction : instructions) {
            selectedInstructions.add(new CandidateInstruction(
                    instruction.getInstructionReference(),
                    instruction.getInstructionAmount(),
                    instruction.getExpectedFee()
            ));
        }

        return new FundingResponse(
                run.getId(),
                List.copyOf(selectedInstructions),
                run.getTotalSettlementConsumed(),
                run.getTotalExpectedFee(),
                run.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public FundingHistoryResponse getFundingHistory(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        if (pageRequest.getOffset() > Integer.MAX_VALUE) {
            throw new PaginationLimitExceededException(
                    "Page is too large for the requested size. With size " + size
                            + ", page must not exceed " + Integer.MAX_VALUE / size + "."
            );
        }

        Page<SettlementRun> runPage = runRepository.findAll(pageRequest);

        List<UUID> runIds = new ArrayList<>();
        for (SettlementRun run : runPage.getContent()) {
            runIds.add(run.getId());
        }

        Map<UUID, List<CandidateInstruction>> instructionsByRun = new HashMap<>();

        if (!runIds.isEmpty()) {
            List<SettlementInstruction> instructions = instructionRepository
                    .findAllByRun_IdInAndSelectedTrueOrderByIdAsc(runIds);

            for (SettlementInstruction instruction : instructions) {
                UUID runId = instruction.getRun().getId();
                List<CandidateInstruction> selected = instructionsByRun.computeIfAbsent(runId, k -> new ArrayList<>());

                selected.add(new CandidateInstruction(
                        instruction.getInstructionReference(),
                        instruction.getInstructionAmount(),
                        instruction.getExpectedFee()
                ));
            }
        }

        List<FundingResponse> results = new ArrayList<>();

        for (SettlementRun run : runPage.getContent()) {
            List<CandidateInstruction> selected =
                    instructionsByRun.getOrDefault(run.getId(), List.of());

            results.add(new FundingResponse(
                    run.getId(),
                    List.copyOf(selected),
                    run.getTotalSettlementConsumed(),
                    run.getTotalExpectedFee(),
                    run.getCreatedAt()
            ));
        }

        return new FundingHistoryResponse(
                results,
                runPage.getNumber(),
                runPage.getSize(),
                runPage.getTotalElements(),
                runPage.getTotalPages()
        );
    }

    @Transactional
    public FundingOutcome fund(FundingRequest request) {
        FundingSelection selection = fundingSelector.select(
                request.availableSettlementBalance(),
                request.candidateInstructions()
        );

        if (selection.totalExpectedFee().compareTo(MAX_TOTAL_FEE) > 0) {
            throw new FundingLimitExceededException(
                    "The selected instructions' total expected fee exceeds the supported maximum of "
                            + MAX_TOTAL_FEE.toPlainString()
            );
        }

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
        boolean nothingFits = true;

        for (int index = 0; index < candidates.size(); index++) {
            CandidateInstruction candidate = candidates.get(index);
            if (candidate.instructionAmount()
                    .compareTo(request.availableSettlementBalance()) <= 0) {
                nothingFits = false;
            }
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

        FundingResponse response = new FundingResponse(
                run.getId(),
                List.copyOf(selectedInstructions),
                run.getTotalSettlementConsumed(),
                run.getTotalExpectedFee(),
                run.getCreatedAt()
        );
        return new FundingOutcome(response, nothingFits);
    }
}
