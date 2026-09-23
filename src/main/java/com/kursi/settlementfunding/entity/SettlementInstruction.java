package com.kursi.settlementfunding.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "settlement_instructions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementInstruction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private SettlementRun run;

    @Column(name = "instruction_reference",length = 50, nullable = false)
    private String instructionReference;

    @Column(name = "instruction_amount", precision = 20, scale = 4, nullable = false)
    private BigDecimal instructionAmount;

    @Column(name = "expected_fee", precision = 20, scale = 4, nullable = false)
    private BigDecimal expectedFee;

    @Column(name = "selected", nullable = false)
    private boolean selected;

    public SettlementInstruction(
            SettlementRun run,
            String instructionReference,
            BigDecimal instructionAmount,
            BigDecimal expectedFee,
            boolean selected)
    {
        this.run = run;
        this.instructionReference = instructionReference;
        this.instructionAmount = instructionAmount;
        this.expectedFee = expectedFee;
        this.selected = selected;
    }



}
