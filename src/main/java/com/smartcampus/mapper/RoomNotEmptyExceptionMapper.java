package com.smartcampus.mapper;

import com.smartcampus.exception.RoomNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException ex) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roomId", ex.getRoomId());
        details.put("activeSensorCount", ex.getSensorCount());

        Map<String, Object> body = ErrorPayload.of(
                Response.Status.CONFLICT.getStatusCode(),
                "RoomNotEmpty",
                ex.getMessage(),
                details);

        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
