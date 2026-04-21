package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discovery(@Context UriInfo uriInfo) {
        String base = uriInfo.getBaseUri().toString();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        Map<String, String> links = new LinkedHashMap<>();
        links.put("self", base);
        links.put("rooms", base + "/rooms");
        links.put("sensors", base + "/sensors");
        links.put("sensorReadings", base + "/sensors/{sensorId}/readings");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("name", "Smart Campus Platform Team");
        contact.put("email", "smart-campus@westminster.ac.uk");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", "Smart Campus Sensor & Room Management API");
        body.put("version", "1.0.0");
        body.put("apiVersion", "v1");
        body.put("description",
                "RESTful interface for managing rooms, sensors and historical readings across campus.");
        body.put("contact", contact);
        body.put("links", links);

        return Response.ok(body).build();
    }
}
