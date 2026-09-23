package com.kursi.settlementfunding.repository;

import com.kursi.settlementfunding.entity.SettlementRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SettlementRunRepository extends JpaRepository<SettlementRun, UUID> {

}
