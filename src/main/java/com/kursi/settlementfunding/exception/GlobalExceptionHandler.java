package com.kursi.settlementfunding.exception;

import org.springframework.http.HttpStatus;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ValidationError(String field, String message) {}

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleParameterValidation(HandlerMethodValidationException exception) {
        if (exception.isForReturnValue()) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "The server produced an invalid response."
            );
            problem.setTitle("Internal server error");
            return problem;
        }

        List<ValidationError> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            String parameter = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new ValidationError(parameter, error.getDefaultMessage()));
            }
        }

        ProblemDetail problem = badRequest("One or more request parameters are invalid.");
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        if (UUID.class.equals(exception.getRequiredType())) {
            return badRequest("Parameter '" + exception.getName() + "' must be a valid UUID.");
        }
        return badRequest("Parameter '" + exception.getName() + "' must be a valid integer.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        ProblemDetail problem = badRequest("One or more request fields are invalid.");
        List<ValidationError> errors = new ArrayList<>();

        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            errors.add(new ValidationError(error.getField(), error.getDefaultMessage()));
        }

        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody() {
        return badRequest("The request body must contain valid JSON matching the funding request: "
                + "monetary fields must be numbers and candidateInstructions must be an array.");
    }

    @ExceptionHandler(FundingLimitExceededException.class)
    public ProblemDetail handleFundingLimit(FundingLimitExceededException exception) {
        return badRequest(exception.getMessage());
    }

    private ProblemDetail badRequest(String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Invalid funding request");
        return problem;
    }

    @ExceptionHandler(FundingRunNotFoundException.class)
    public ProblemDetail handleFundingRunNotFound(FundingRunNotFoundException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.NOT_FOUND,
                exception.getMessage()
        );
        problem.setTitle("Funding run not found");
        return problem;
    }
}
