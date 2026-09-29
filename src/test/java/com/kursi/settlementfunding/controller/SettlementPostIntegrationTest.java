package com.kursi.settlementfunding.controller;

import com.kursi.settlementfunding.entity.SettlementInstruction;
import com.kursi.settlementfunding.entity.SettlementRun;
import com.kursi.settlementfunding.repository.SettlementInstructionRepository;
import com.kursi.settlementfunding.repository.SettlementRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SettlementPostIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SettlementRunRepository runRepository;

    @Autowired
    private SettlementInstructionRepository instructionRepository;

    @BeforeEach
    void cleanDatabase() {
        instructionRepository.deleteAll();
        runRepository.deleteAll();
    }

    @Test
    void shouldFundAndPersistRunWithAllCandidates() throws Exception {
        String body = """
                {
                  "availableSettlementBalance": 10,
                  "candidateInstructions": [
                    {
                      "instructionReference": "INS-1",
                      "instructionAmount": 7,
                      "expectedFee": 4
                    },
                    {
                      "instructionReference": "INS-2",
                      "instructionAmount": 6,
                      "expectedFee": 3
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/settlement/fund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.selectedInstructions.length()").value(1))
                .andExpect(jsonPath("$.selectedInstructions[0].instructionReference").value("INS-1"))
                .andExpect(jsonPath("$.totalSettlementConsumed").value(7))
                .andExpect(jsonPath("$.totalExpectedFee").value(4))
                .andExpect(jsonPath("$.nothingFits").doesNotExist());

        List<SettlementRun> runs = runRepository.findAll();
        assertThat(runs).hasSize(1);

        SettlementRun run = runs.getFirst();
        assertThat(run.getId()).isNotNull();
        assertThat(run.getCreatedAt()).isNotNull();
        assertThat(run.getAvailableSettlementBalance()).isEqualByComparingTo("10");
        assertThat(run.getTotalSettlementConsumed()).isEqualByComparingTo("7");
        assertThat(run.getTotalExpectedFee()).isEqualByComparingTo("4");

        List<SettlementInstruction> instructions = instructionRepository.findAll();
        assertThat(instructions).hasSize(2);

        SettlementInstruction first = null;
        SettlementInstruction second = null;

        for (SettlementInstruction instruction : instructions) {
            if ("INS-1".equals(instruction.getInstructionReference())) {
                first = instruction;
            } else if ("INS-2".equals(instruction.getInstructionReference())) {
                second = instruction;
            }

            assertThat(instruction.getRun().getId()).isEqualTo(run.getId());
        }

        assertThat(first).isNotNull();
        assertThat(first.isSelected()).isTrue();
        assertThat(second).isNotNull();
        assertThat(second.isSelected()).isFalse();
    }

    @Test
    void shouldPersistEmptyResultWhenNothingFits() throws Exception {
        String body = """
                {
                  "availableSettlementBalance": 5,
                  "candidateInstructions": [
                    {
                      "instructionReference": "INS-1",
                      "instructionAmount": 6,
                      "expectedFee": 3
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/settlement/fund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.selectedInstructions").isEmpty())
                .andExpect(jsonPath("$.totalSettlementConsumed").value(0))
                .andExpect(jsonPath("$.totalExpectedFee").value(0));

        List<SettlementRun> runs = runRepository.findAll();
        assertThat(runs).hasSize(1);

        SettlementRun run = runs.getFirst();
        assertThat(run.getTotalSettlementConsumed()).isEqualByComparingTo("0");
        assertThat(run.getTotalExpectedFee()).isEqualByComparingTo("0");

        List<SettlementInstruction> instructions = instructionRepository.findAll();
        assertThat(instructions).hasSize(1);
        assertThat(instructions.getFirst().isSelected()).isFalse();
        assertThat(instructions.getFirst().getRun().getId()).isEqualTo(run.getId());
    }
}
