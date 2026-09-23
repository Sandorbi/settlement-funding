package com.kursi.settlementfunding.repository;

import com.kursi.settlementfunding.entity.SettlementInstruction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementInstructionRepository extends JpaRepository<SettlementInstruction, Long> {
}
