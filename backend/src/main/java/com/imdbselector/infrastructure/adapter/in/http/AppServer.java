package com.imdbselector.infrastructure.adapter.in.http;

import com.imdbselector.infrastructure.config.AppAssembler;

/**
 * HTTP entry point for the IMDBSelector REST API.
 */
public final class AppServer {
    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        String jdbcUrl = System.getenv().getOrDefault("DB_URL", "jdbc:postgresql://localhost:5432/imdbselector");
        String dbUser = System.getenv().getOrDefault("DB_USER", "imdb");
        String dbPassword = System.getenv().getOrDefault("DB_PASSWORD", "imdb");
        String corsOrigin = System.getenv().getOrDefault("CORS_ORIGIN", "http://localhost:5173");

        AppAssembler assembler = new AppAssembler(jdbcUrl, dbUser, dbPassword, corsOrigin);
        Runtime.getRuntime().addShutdownHook(new Thread(assembler::close));
        assembler.javalin().start(port);
    }
}
