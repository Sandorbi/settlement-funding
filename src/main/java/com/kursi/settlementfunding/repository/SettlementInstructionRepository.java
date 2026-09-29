package com.kursi.settlementfunding.repository;

import com.kursi.settlementfunding.entity.SettlementInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementInstructionRepository extends JpaRepository<SettlementInstruction, Long> {

    List<SettlementInstruction> findAllByRun_IdAndSelectedTrue(UUID runId);

    List<SettlementInstruction> findAllByRun_IdInAndSelectedTrueOrderByIdAsc(
            List<UUID> runIds
    );
}
