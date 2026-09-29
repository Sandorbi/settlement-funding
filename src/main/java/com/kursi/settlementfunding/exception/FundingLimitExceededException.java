package com.kursi.settlementfunding.exception;

public class FundingLimitExceededException extends RuntimeException {

    public FundingLimitExceededException(String message) {
        super(message);
    }
}
