package com.kursi.settlementfunding.exception;

public class PaginationLimitExceededException extends RuntimeException {

    public PaginationLimitExceededException(String message) {
        super(message);
    }
}
