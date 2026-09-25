package io.github.lijinhong11.nexusmcpublisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class NexusMCApiClient {
    private final String baseUrl;
    private final String token;
    private final ObjectMapper json;

    public NexusMCApiClient(String baseUrl, String token, ObjectMapper json) {
        this.baseUrl = stripTrailingSlash(requireText(baseUrl, "baseUrl"));
        this.token = requireText(token, "token");
        this.json = json;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private static void writeAscii(OutputStream output, String value) throws IOException {
        output.write(value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static JsonNode requiredField(JsonNode node, String name) {
        JsonNode field = node.get(name);
        if (field == null || field.isNull()) {
            throw new NexusMCApiException("NexusMC response is missing field '" + name + "'", null);
        }
        return field;
    }

    private static String optionalText(JsonNode node, String name) {
        JsonNode field = node.get(name);
        return field == null || field.isNull() ? null : field.asText();
    }

    private static void putIfPresent(ObjectNode node, String name, String value) {
        if (value != null && !value.trim().isEmpty()) {
            node.put(name, value);
        }
    }

    private static void putIfPresent(ObjectNode node, String name, JsonNode value) {
        if (value != null && !value.isNull()) {
            node.set(name, value);
        }
    }

    private static void putStringArray(ObjectNode node, String name, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        ArrayNode array = node.putArray(name);
        for (String value : values) {
            array.add(value);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static String defaultIfBlank(String value) {
        return value == null || value.trim().isEmpty() ? "local" : value;
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private static String escapeQuoted(String value) {
        return value.replace("\\", "_").replace("\"", "_").replace("\r", "_").replace("\n", "_");
    }

    public UploadedFile upload(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Artifact does not exist or is not a file: " + file);
        }
        String boundary = "NexusMCPublisher-" + UUID.randomUUID();
        HttpURLConnection connection = open("/api/upload");
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        connection.setDoOutput(true);
        try {
            OutputStream output = connection.getOutputStream();
            String fileName = file.getFileName().toString();
            writeAscii(output, "--" + boundary + "\r\n");
            writeAscii(output, "Content-Disposition: form-data; name=\"file\"; filename=\"" + escapeQuoted(fileName) + "\"\r\n");
            writeAscii(output, "Content-Type: application/octet-stream\r\n\r\n");
            Files.copy(file, output);
            writeAscii(output, "\r\n--" + boundary + "--\r\n");
            output.close();

            JsonNode response = readJsonResponse(connection);
            return new UploadedFile(
                    requiredField(response, "url").asText(),
                    requiredField(response, "filename").asText(),
                    requiredField(response, "size").asLong(),
                    optionalText(response, "sha256"),
                    optionalText(response, "sha1")
            );
        } catch (IOException error) {
            throw new NexusMCApiException("Could not upload " + file, error);
        } finally {
            connection.disconnect();
        }
    }

    public JsonNode publishVersion(String resourceId, VersionRequest request) {
        requireText(resourceId, "resourceId");
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        ObjectNode body = json.createObjectNode();
        body.put("version", requireText(request.version, "version"));
        if (request.versionTag != null) {
            body.put("versionTag", request.versionTag.getApiKey());
        }
        putIfPresent(body, "title", request.title);
        putIfPresent(body, "changelog", request.changelog);
        body.put("downloadType", defaultIfBlank(request.downloadType));

        if (request.files.isEmpty()) {
            throw new IllegalArgumentException("A version must contain at least one file");
        }
        int primaryFiles = 0;
        for (VersionFile file : request.files) {
            if (file.primary) {
                primaryFiles++;
            }
        }
        if (primaryFiles != 1) {
            throw new IllegalArgumentException("A version must contain exactly one primary file");
        }

        ArrayNode files = body.putArray("files");
        for (VersionFile file : request.files) {
            ObjectNode uploaded = files.addObject();
            uploaded.put("url", file.uploadedFile.url);
            uploaded.put("fileName", file.uploadedFile.filename);
            uploaded.put("fileSize", file.uploadedFile.size);
            uploaded.put("isPrimary", file.primary);
            putIfPresent(uploaded, "sha256", file.uploadedFile.sha256);
            putIfPresent(uploaded, "sha1", file.uploadedFile.sha1);
            putStringArray(uploaded, "gameVersions", file.gameVersions);
            putStringArray(uploaded, "subcategoryIds", file.subcategoryIds);
        }

        ArrayNode versions = body.putArray("mcVersions");
        for (String gameVersion : request.mcVersions) {
            versions.add(gameVersion);
        }
        return sendJson("/api/resources/" + resourceId + "/versions", body);
    }

    private JsonNode sendJson(String path, JsonNode body) {
        HttpURLConnection connection = open(path);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        try {
            OutputStream output = connection.getOutputStream();
            json.writeValue(output, body);
            output.close();
            return readJsonResponse(connection);
        } catch (IOException error) {
            throw new NexusMCApiException("Could not call NexusMC API " + path, error);
        } finally {
            connection.disconnect();
        }
    }

    private HttpURLConnection open(String path) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(baseUrl + path).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(30000);
            connection.setReadTimeout(120000);
            return connection;
        } catch (IOException error) {
            throw new NexusMCApiException("Could not connect to NexusMC", error);
        }
    }

    private JsonNode readJsonResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream();
        String responseBody = stream == null ? "" : new String(readAll(stream), StandardCharsets.UTF_8);
        if (status < 200 || status >= 300) {
            throw new NexusMCApiException(status, responseBody);
        }
        return responseBody.trim().isEmpty() ? json.createObjectNode() : json.readTree(responseBody);
    }

    public static final class UploadedFile {
        private final String url;
        private final String filename;
        private final long size;
        private final String sha256;
        private final String sha1;

        public UploadedFile(String url, String filename, long size, String sha256, String sha1) {
            this.url = url;
            this.filename = filename;
            this.size = size;
            this.sha256 = sha256;
            this.sha1 = sha1;
        }

        public String getUrl() {
            return url;
        }

        public String getFilename() {
            return filename;
        }

        public long getSize() {
            return size;
        }

        public String getSha256() {
            return sha256;
        }

        public String getSha1() {
            return sha1;
        }
    }

    public static final class VersionRequest {
        private final String version;
        private final VersionTag versionTag;
        private final String title;
        private final JsonNode changelog;
        private final String downloadType;
        private final List<VersionFile> files;
        private final List<String> mcVersions;

        public VersionRequest(String version, VersionTag versionTag, String title, JsonNode changelog,
                              String downloadType, List<VersionFile> files, List<String> mcVersions) {
            this.version = version;
            this.versionTag = versionTag;
            this.title = title;
            this.changelog = changelog;
            this.downloadType = downloadType;
            this.files = files == null ? Collections.emptyList() : files;
            this.mcVersions = mcVersions == null ? Collections.emptyList() : mcVersions;
        }
    }

    public static final class VersionFile {
        private final UploadedFile uploadedFile;
        private final boolean primary;
        private final List<String> gameVersions;
        private final List<String> subcategoryIds;

        public VersionFile(UploadedFile uploadedFile, boolean primary,
                           List<String> gameVersions, List<String> subcategoryIds) {
            if (uploadedFile == null) {
                throw new IllegalArgumentException("uploadedFile must not be null");
            }
            this.uploadedFile = uploadedFile;
            this.primary = primary;
            this.gameVersions = gameVersions == null ? Collections.emptyList() : gameVersions;
            this.subcategoryIds = subcategoryIds == null ? Collections.emptyList() : subcategoryIds;
        }
    }
}
