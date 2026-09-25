package io.github.lijinhong11.nexusmcpublisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class NexusMCApiClientTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @TempDir
    Path temporaryDirectory;

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void uploadsAFileUsingTheDocumentedMultipartContract() throws Exception {
        AtomicReference<HttpExchange> request = new AtomicReference<>();
        AtomicReference<byte[]> body = new AtomicReference<>();
        startServer("/api/upload", exchange -> {
            request.set(exchange);
            body.set(readAllBytes(exchange.getRequestBody()));
            respond(exchange, 200, "{\"url\":\"/uploads/files/random.jar\",\"filename\":\"plugin.jar\",\"size\":4,\"sha256\":\"abc\",\"sha1\":\"def\"}");
        });
        Path artifact = temporaryDirectory.resolve("plugin.jar");
        Files.write(artifact, "test".getBytes(StandardCharsets.UTF_8));

        NexusMCApiClient.UploadedFile uploaded = client().upload(artifact);

        assertEquals("Bearer secret", request.get().getRequestHeaders().getFirst("Authorization"));
        assertTrue(request.get().getRequestHeaders().getFirst("Content-Type").startsWith("multipart/form-data; boundary="));
        String multipartBody = new String(body.get(), StandardCharsets.ISO_8859_1);
        assertTrue(multipartBody.contains("name=\"file\"; filename=\"plugin.jar\""));
        assertTrue(multipartBody.contains("test"));
        assertEquals("/uploads/files/random.jar", uploaded.getUrl());
        assertEquals("plugin.jar", uploaded.getFilename());
        assertEquals(4, uploaded.getSize());
    }

    @Test
    void publishesAResourceVersionUsingTheUploadedFileMetadata() throws Exception {
        AtomicReference<JsonNode> body = new AtomicReference<>();
        startServer("/api/resources/resource-1/versions", exchange -> {
            body.set(JSON.readTree(exchange.getRequestBody()));
            respond(exchange, 201, "{\"id\":\"version-1\"}");
        });
        NexusMCApiClient.VersionRequest request = new NexusMCApiClient.VersionRequest(
            "1.2.3", VersionTag.RELEASE, "Version 1.2.3", JSON.readTree("{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":[{\"type\":\"text\",\"text\":\"Changes\"}]}]}"), "local",
            Collections.singletonList(new NexusMCApiClient.VersionFile(
                new NexusMCApiClient.UploadedFile("/uploads/files/random.jar", "plugin.jar", 4, "abc", "def"),
                true, Collections.singletonList("1.21.4"), Collections.singletonList("paper")
            )),
                Collections.singletonList("1.21.4")
        );

        client().publishVersion("resource-1", request);

        assertEquals("1.2.3", body.get().get("version").asText());
        assertEquals("releases", body.get().get("versionTag").asText());
        assertEquals("doc", body.get().get("changelog").get("type").asText());
        assertEquals("plugin.jar", body.get().get("files").get(0).get("fileName").asText());
        assertEquals("abc", body.get().get("files").get(0).get("sha256").asText());
        assertTrue(body.get().get("files").get(0).get("isPrimary").asBoolean());
        assertEquals("1.21.4", body.get().get("files").get(0).get("gameVersions").get(0).asText());
        assertEquals("paper", body.get().get("files").get(0).get("subcategoryIds").get(0).asText());
        assertNull(body.get().get("files").get(0).get("loaders"));
        assertEquals("1.21.4", body.get().get("mcVersions").get(0).asText());
    }

    @Test
    void publishesAllUploadedFilesAndTheirMetadata() throws Exception {
        AtomicReference<JsonNode> body = new AtomicReference<>();
        startServer("/api/resources/resource-1/versions", exchange -> {
            body.set(JSON.readTree(exchange.getRequestBody()));
            respond(exchange, 201, "{\"id\":\"version-1\"}");
        });
        NexusMCApiClient.VersionRequest request = new NexusMCApiClient.VersionRequest(
            "2.0.0", VersionTag.BETA, "Version 2.0.0", JSON.readTree("{\"type\":\"doc\",\"content\":[]}"), "local",
            Arrays.asList(
                new NexusMCApiClient.VersionFile(
                    new NexusMCApiClient.UploadedFile("/uploads/paper.jar", "paper.jar", 10, "p256", "p1"),
                    true, Collections.singletonList("1.21.4"), Collections.singletonList("paper")
                ),
                new NexusMCApiClient.VersionFile(
                    new NexusMCApiClient.UploadedFile("/uploads/fabric.jar", "fabric.jar", 20, "f256", "f1"),
                    false, Arrays.asList("1.21.1", "1.21.4"), Collections.singletonList("fabric")
                )
            ),
            Arrays.asList("1.21.1", "1.21.4")
        );

        client().publishVersion("resource-1", request);

        assertEquals(2, body.get().get("files").size());
        assertEquals("paper.jar", body.get().get("files").get(0).get("fileName").asText());
        assertTrue(body.get().get("files").get(0).get("isPrimary").asBoolean());
        assertEquals("fabric.jar", body.get().get("files").get(1).get("fileName").asText());
        assertFalse(body.get().get("files").get(1).get("isPrimary").asBoolean());
        assertEquals("fabric", body.get().get("files").get(1).get("subcategoryIds").get(0).asText());
        assertNull(body.get().get("files").get(1).get("loaders"));
        assertEquals(2, body.get().get("files").get(1).get("gameVersions").size());
    }

    @Test
    void rejectsVersionsWithoutExactlyOnePrimaryFile() {
        NexusMCApiClient.VersionRequest request = new NexusMCApiClient.VersionRequest(
            "2.0.0", VersionTag.RELEASE, null, null, "local",
            Arrays.asList(
                new NexusMCApiClient.VersionFile(
                    new NexusMCApiClient.UploadedFile("/uploads/a.jar", "a.jar", 1, null, null),
                    false, Collections.emptyList(), Collections.emptyList()
                ),
                new NexusMCApiClient.VersionFile(
                    new NexusMCApiClient.UploadedFile("/uploads/b.jar", "b.jar", 1, null, null),
                    false, Collections.emptyList(), Collections.emptyList()
                )
            ),
            Collections.emptyList()
        );

        IllegalArgumentException error = assertThrows(
            IllegalArgumentException.class,
            () -> new NexusMCApiClient("http://localhost:1", "secret", JSON)
                .publishVersion("resource-1", request)
        );

        assertTrue(error.getMessage().contains("exactly one primary"));
    }

    @Test
    void reportsTheResponseBodyWhenTheApiRejectsARequest() throws Exception {
        startServer("/api/upload", exchange -> respond(exchange, 403, "{\"error\":\"missing upload:file\"}"));
        Path artifact = temporaryDirectory.resolve("plugin.jar");
        Files.write(artifact, "test".getBytes(StandardCharsets.UTF_8));

        NexusMCApiException error = assertThrows(NexusMCApiException.class, () -> client().upload(artifact));

        assertEquals(403, error.getStatusCode());
        assertTrue(error.getMessage().contains("missing upload:file"));
    }

    private NexusMCApiClient client() {
        return new NexusMCApiClient("http://localhost:" + server.getAddress().getPort(), "secret", JSON);
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private void startServer(String path, ThrowingHandler handler) throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext(path, exchange -> {
            try {
                handler.handle(exchange);
            } catch (Exception error) {
                exchange.close();
                throw new RuntimeException(error);
            }
        });
        server.start();
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    @FunctionalInterface
    private interface ThrowingHandler {
        void handle(HttpExchange exchange) throws Exception;
    }
}
