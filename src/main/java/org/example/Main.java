package org.example;

import jdk.incubator.concurrent.StructuredTaskScope;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

public class Main {
    public static void main(String[] args) {
        try {
            var address = InetAddress.getByName("0.0.0.0");
            listen(address);
        } catch (IOException | InterruptedException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    private static void listen(InetAddress address) throws IOException, InterruptedException, TimeoutException {
        try (var socket = new ServerSocket(8080, 1024, address)) {
            System.out.println("Listening on: " + address);

            try (var scope = new StructuredTaskScope<Void>()) {
                try {
                    while (true) {
                        var connection = socket.accept();
                        scope.fork(() -> handle(connection));
                    }
                } finally {
                    System.out.println("Shutting down");
                    scope.shutdown();
                    scope.joinUntil(Instant.now().plus(Duration.ofSeconds(10)));
                }
            }
        }
    }

    private static Void handle(Socket connection) throws IOException {
        try (connection) {
            System.out.println("Incoming connection: " + connection.getInetAddress().toString());

            http10(connection);

            return null;
        } finally {
            System.out.println("Connection closed: " + connection.getInetAddress().toString());
        }
    }

    private static void echoAndClose(Socket connection) throws IOException {
        try (var is = connection.getInputStream()) {
            try (var os = connection.getOutputStream()) {
                try (var reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    try (var writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        var l = reader.readLine();
                        writer.write(l);
                    }
                }
            }
        }
    }

    private static void echo(Socket connection) throws IOException {
        try (var is = connection.getInputStream()) {
            try (var os = connection.getOutputStream()) {
                is.transferTo(os);
            }
        }
    }

    private static void http10(Socket connection) throws IOException {
        try (var is = connection.getInputStream()) {
            try (var os = connection.getOutputStream()) {
                try (var r = new Scanner(is, StandardCharsets.US_ASCII)) {
                    try (var writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
                        r.useDelimiter("\r\n");
                        String requestLine = r.next();
                        System.out.println(requestLine);
                        writer.write("HTTP/1.0 204 No Content \r\n");
                    }
                }
            }
        }
    }
}