package com.kursi.settlementfunding.controller;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SettlementApiIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SettlementRunRepository runRepository;

    @Autowired
    private SettlementInstructionRepository instructionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    @Test
    void shouldReturnSavedFundingRun() throws Exception {
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

        MvcResult postResult = mockMvc.perform(post("/api/v1/settlement/fund")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        String postResponse = postResult.getResponse().getContentAsString();
        String requestId = JsonPath.read(postResponse, "$.requestId");
        String createdAt = JsonPath.read(postResponse, "$.createdAt");

        mockMvc.perform(get("/api/v1/settlement/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.createdAt").value(createdAt))
                .andExpect(jsonPath("$.selectedInstructions.length()").value(1))
                .andExpect(jsonPath("$.selectedInstructions[0].instructionReference").value("INS-1"))
                .andExpect(jsonPath("$.selectedInstructions[0].instructionAmount").value(7))
                .andExpect(jsonPath("$.selectedInstructions[0].expectedFee").value(4))
                .andExpect(jsonPath("$.totalSettlementConsumed").value(7))
                .andExpect(jsonPath("$.totalExpectedFee").value(4));
    }

    @Test
    void shouldReturnNotFoundForUnknownRun() throws Exception {
        String requestId = "00000000-0000-0000-0000-000000000001";

        mockMvc.perform(get("/api/v1/settlement/{requestId}", requestId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Funding run not found"))
                .andExpect(jsonPath("$.detail", containsString(requestId)));
    }

    @Test
    void shouldReturnEmptyHistoryWithDefaultPagination() throws Exception {
        mockMvc.perform(get("/api/v1/settlement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void shouldReturnHistoryNewestFirstAcrossPages() throws Exception {
        SettlementRun olderRun = runRepository.saveAndFlush(
                new SettlementRun(
                        new BigDecimal("10"),
                        new BigDecimal("7"),
                        new BigDecimal("4")
                )
        );

        SettlementRun newerRun = runRepository.saveAndFlush(
                new SettlementRun(
                        new BigDecimal("20"),
                        new BigDecimal("6"),
                        new BigDecimal("3")
                )
        );

        // Fixed timestamps make the expected order independent of execution speed.
        jdbcTemplate.update(
                "UPDATE settlement_runs SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-09-07T09:00:00Z")),
                olderRun.getId()
        );
        jdbcTemplate.update(
                "UPDATE settlement_runs SET created_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-09-08T09:00:00Z")),
                newerRun.getId()
        );

        instructionRepository.saveAll(List.of(
                new SettlementInstruction(
                        olderRun, "OLDER-SELECTED",
                        new BigDecimal("7"), new BigDecimal("4"), true
                ),
                new SettlementInstruction(
                        newerRun, "NEWER-SELECTED",
                        new BigDecimal("6"), new BigDecimal("3"), true
                ),
                new SettlementInstruction(
                        newerRun, "NEWER-UNSELECTED",
                        new BigDecimal("21"), new BigDecimal("5"), false
                )
        ));

        // First page: only the newer run and its selected instruction.
        mockMvc.perform(get("/api/v1/settlement")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].requestId").value(newerRun.getId().toString()))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-09-08T09:00:00Z"))
                .andExpect(jsonPath("$.content[0].selectedInstructions.length()").value(1))
                .andExpect(jsonPath("$.content[0].selectedInstructions[0].instructionReference")
                        .value("NEWER-SELECTED"))
                .andExpect(jsonPath("$.content[0].totalSettlementConsumed").value(6))
                .andExpect(jsonPath("$.content[0].totalExpectedFee").value(3));

        // Second page: only the older run and its selected instruction.
        mockMvc.perform(get("/api/v1/settlement")
                        .param("page", "1")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].requestId").value(olderRun.getId().toString()))
                .andExpect(jsonPath("$.content[0].createdAt").value("2026-09-07T09:00:00Z"))
                .andExpect(jsonPath("$.content[0].selectedInstructions.length()").value(1))
                .andExpect(jsonPath("$.content[0].selectedInstructions[0].instructionReference")
                        .value("OLDER-SELECTED"))
                .andExpect(jsonPath("$.content[0].totalSettlementConsumed").value(7))
                .andExpect(jsonPath("$.content[0].totalExpectedFee").value(4));
    }
}
