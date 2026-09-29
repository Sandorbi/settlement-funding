package com.kursi.settlementfunding.dto;

import java.util.List;

public record FundingHistoryResponse(
        List<FundingResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public FundingHistoryResponse {
        content = List.copyOf(content);
    }
}
