package com.smartcampus.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global safety net. Any throwable that bubbles up without a dedicated
 * mapper is logged server-side and turned into a generic 500 response so
 * stack traces never reach the client.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            // Delegate to the framework-specific mapper.
            return ((WebApplicationException) ex).getResponse();
        }

        LOGGER.log(Level.SEVERE, "Unhandled server error", ex);

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorPayload.of(
                        Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                        "InternalServerError",
                        "An unexpected error occurred. Please contact the API administrator."))
                .build();
    }
}
