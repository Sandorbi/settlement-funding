package com.kursi.settlementfunding.exception;

import java.util.UUID;

public class FundingRunNotFoundException extends RuntimeException {

    public FundingRunNotFoundException(UUID requestId) {
        super("Funding run not found: " + requestId);
    }
}
