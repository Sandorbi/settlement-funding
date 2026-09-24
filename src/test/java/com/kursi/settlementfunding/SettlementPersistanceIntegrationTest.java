package com.kursi.settlementfunding;

import com.kursi.settlementfunding.entity.SettlementInstruction;
import com.kursi.settlementfunding.entity.SettlementRun;
import com.kursi.settlementfunding.repository.SettlementInstructionRepository;
import com.kursi.settlementfunding.repository.SettlementRunRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class SettlementPersistanceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private SettlementRunRepository runRepository;

    @Autowired
    private SettlementInstructionRepository instructionRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistAndReloadRunWithSelectedAndUnselectedInstructions() {

        // Creates and saves run and instruction entities
        SettlementRun run = new SettlementRun(
                new BigDecimal("10000.0000"),
                new BigDecimal("7000.1234"),
                new BigDecimal("150.5678")
        );
        runRepository.save(run);

        SettlementInstruction selected = new SettlementInstruction(
                run,
                "INS-2001",
                new BigDecimal("7000.1234"),
                new BigDecimal("150.5678"),
                true
        );
        SettlementInstruction unselected = new SettlementInstruction(
                run,
                "INS-2002",
                new BigDecimal("9000.0000"),
                new BigDecimal("100.0000"),
                false
        );
        instructionRepository.save(selected);
        instructionRepository.save(unselected);

        entityManager.flush();

        // Checks that columns aren't nulls
        assertThat(run.getId()).isNotNull();
        assertThat(run.getCreatedAt()).isNotNull();
        assertThat(selected.getId()).isNotNull();
        assertThat(unselected.getId()).isNotNull();
        assertThat(selected.getId()).isNotEqualTo(unselected.getId());

        // Get IDs
        UUID runId = run.getId();
        Long selectedId = selected.getId();
        Long unselectedId = unselected.getId();

        entityManager.clear();

        // Load from PostgreSQL instead of the persistence context's cache.
        SettlementRun loadedRun = runRepository.findById(runId).orElseThrow();
        SettlementInstruction loadedSelected = instructionRepository
                .findById(selectedId).orElseThrow();
        SettlementInstruction loadedUnselected = instructionRepository
                .findById(unselectedId).orElseThrow();

        // Check if returned values from db are correct
        assertThat(loadedRun.getAvailableSettlementBalance())
                .isEqualByComparingTo("10000.0000");
        assertThat(loadedRun.getTotalSettlementConsumed())
                .isEqualByComparingTo("7000.1234");
        assertThat(loadedRun.getTotalExpectedFee())
                .isEqualByComparingTo("150.5678");
        assertThat(loadedRun.getCreatedAt()).isNotNull();

        assertThat(loadedSelected.getInstructionReference()).isEqualTo("INS-2001");
        assertThat(loadedSelected.getInstructionAmount())
                .isEqualByComparingTo("7000.1234");
        assertThat(loadedSelected.getExpectedFee())
                .isEqualByComparingTo("150.5678");
        assertThat(loadedSelected.isSelected()).isTrue();
        assertThat(loadedSelected.getRun().getId()).isEqualTo(runId);

        assertThat(loadedUnselected.getInstructionReference()).isEqualTo("INS-2002");
        assertThat(loadedUnselected.getInstructionAmount())
                .isEqualByComparingTo("9000.0000");
        assertThat(loadedUnselected.getExpectedFee())
                .isEqualByComparingTo("100.0000");
        assertThat(loadedUnselected.isSelected()).isFalse();
        assertThat(loadedUnselected.getRun().getId()).isEqualTo(runId);
    }
}
