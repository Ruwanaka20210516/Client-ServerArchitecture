package com.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Catches framework-raised WebApplicationExceptions (e.g. 405 Method Not
 * Allowed, 415 Unsupported Media Type, 406 Not Acceptable) so they come
 * back as a uniform JSON body instead of Jersey's default HTML page.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException ex) {
        Response original = ex.getResponse();
        int status = original != null ? original.getStatus() : 500;
        String reason = original != null && original.getStatusInfo() != null
                ? original.getStatusInfo().getReasonPhrase()
                : "Error";
        String message = ex.getMessage() == null ? reason : ex.getMessage();

        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorPayload.of(status, reason, message))
                .build();
    }
}
