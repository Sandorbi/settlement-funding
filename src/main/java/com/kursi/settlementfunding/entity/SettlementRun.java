package com.kursi.settlementfunding.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Table(name = "settlement_runs")
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class SettlementRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "available_settlement_balance", precision = 20, scale = 4, nullable = false)
    private BigDecimal availableSettlementBalance;

    @Column(name = "total_settlement_consumed", precision = 20, scale = 4, nullable = false)
    private BigDecimal totalSettlementConsumed;

    @Column(name = "total_expected_fee",  precision = 20, scale = 4, nullable = false)
    private BigDecimal totalExpectedFee;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public SettlementRun(BigDecimal availableSettlementBalance,
                         BigDecimal totalSettlementConsumed,
                         BigDecimal totalExpectedFee)
    {
        this.availableSettlementBalance = availableSettlementBalance;
        this.totalSettlementConsumed = totalSettlementConsumed;
        this.totalExpectedFee = totalExpectedFee;
    }
}
