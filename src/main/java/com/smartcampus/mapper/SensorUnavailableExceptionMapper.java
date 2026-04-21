package com.smartcampus.mapper;

import com.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("sensorId", ex.getSensorId());
        details.put("status", ex.getStatus());

        Map<String, Object> body = ErrorPayload.of(
                Response.Status.FORBIDDEN.getStatusCode(),
                "SensorUnavailable",
                ex.getMessage(),
                details);

        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
