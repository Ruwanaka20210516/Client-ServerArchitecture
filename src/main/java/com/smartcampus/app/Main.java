package com.smartcampus.app;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static final String HOST_URI = "http://localhost:8080/";
    public static final String BASE_URI = HOST_URI + "api/v1/";

    public static HttpServer startServer() {
        ResourceConfig rc = ResourceConfig.forApplicationClass(RestApplication.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    public static void main(String[] args) throws IOException {
        final HttpServer server = startServer();
        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        LOGGER.info(String.format(
                "Smart Campus API started at %s - press CTRL+C to stop.", BASE_URI));

        try {
            Thread.currentThread().join();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            LOGGER.log(Level.WARNING, "Server thread interrupted", ex);
        }
    }

    private Main() {
    }
}
