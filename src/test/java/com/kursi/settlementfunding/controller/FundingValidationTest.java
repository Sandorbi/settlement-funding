package com.kursi.settlementfunding.controller;

import com.kursi.settlementfunding.algorithm.SparseDynamicProgrammingFundingSelector;
import com.kursi.settlementfunding.exception.GlobalExceptionHandler;
import com.kursi.settlementfunding.repository.SettlementInstructionRepository;
import com.kursi.settlementfunding.repository.SettlementRunRepository;
import com.kursi.settlementfunding.service.SettlementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FundingValidationTest {

    private SettlementRunRepository runRepository;
    private SettlementInstructionRepository instructionRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        runRepository = mock(SettlementRunRepository.class);
        instructionRepository = mock(SettlementInstructionRepository.class);
        SettlementService service = new SettlementService(
                new SparseDynamicProgrammingFundingSelector(),
                runRepository,
                instructionRepository
        );
        mockMvc = MockMvcBuilders.standaloneSetup(new SettlementController(service)).setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void shouldRejectNegativeHistoryPage() throws Exception {
        mockMvc.perform(get("/api/v1/settlement").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("page")));
        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectZeroHistorySize() throws Exception {
        mockMvc.perform(get("/api/v1/settlement").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("size")));
        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectHistorySizeOverMaximum() throws Exception {
        mockMvc.perform(get("/api/v1/settlement").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("size")));
        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectNonNumericHistoryPage() throws Exception {
        mockMvc.perform(get("/api/v1/settlement").param("page", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("valid integer")));
        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectInvalidRunIdFormat() throws Exception {
        mockMvc.perform(get("/api/v1/settlement/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("valid UUID")));
        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectBalanceWithTooManyDecimalPlaces() throws Exception {
        String body = """
                {
                  "availableSettlementBalance": 1.00001,
                  "candidateInstructions": [
                    {
                      "instructionReference": "INS-1",
                      "instructionAmount": 1,
                      "expectedFee": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/settlement/fund")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("availableSettlementBalance")))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());

        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectBalanceWithTooManyIntegerDigits() throws Exception {
        String body = """
                {
                  "availableSettlementBalance": 10000000000000000,
                  "candidateInstructions": [
                    {
                      "instructionReference": "INS-1",
                      "instructionAmount": 1,
                      "expectedFee": 1
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/v1/settlement/fund")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("availableSettlementBalance")))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty());

        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectMalformedJson() throws Exception {
        String body = "{";

        mockMvc.perform(post("/api/v1/settlement/fund")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("valid JSON")));

        verifyNoInteractions(runRepository, instructionRepository);
    }

    @Test
    void shouldRejectEmptyRequestBody() throws Exception {
        String body = "";

        mockMvc.perform(post("/api/v1/settlement/fund")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("valid JSON")));

        verifyNoInteractions(runRepository, instructionRepository);
    }

}
