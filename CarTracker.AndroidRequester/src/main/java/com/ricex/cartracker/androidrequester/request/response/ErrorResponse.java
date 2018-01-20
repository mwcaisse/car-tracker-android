package com.ricex.cartracker.androidrequester.request.response;

import org.springframework.http.HttpStatus;

/**
 * Created by Mitchell on 2018-01-20.
 */

public class ErrorResponse<T> extends RequestResponse<T> {

    private String errorMessage;

    /**
     * Creates a new instance of RequestResponse, representing a successful response
     *
     * @param statusCode The status code of the response
     */
    public ErrorResponse(String errorMessage, HttpStatus statusCode) {
        super(statusCode);

        this.errorMessage = errorMessage;
    }

    @Override
    public T getData() {
        return null;
    }

    @Override
    public String getError() {
        return errorMessage;
    }

    @Override
    public boolean isValid() {
        return false;
    }
}
