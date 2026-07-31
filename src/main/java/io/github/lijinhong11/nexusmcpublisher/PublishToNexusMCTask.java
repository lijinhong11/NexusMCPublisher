package io.github.lijinhong11.nexusmcpublisher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

public abstract class PublishToNexusMCTask extends DefaultTask {
    private NexusMCPublisherExtension extension;

    @Input public abstract Property<String> getBaseUrl();
    @Internal public abstract Property<String> getToken();
    @Input public abstract Property<String> getResourceId();
    @Input public abstract Property<String> getVersion();
    @Input @Optional public abstract Property<VersionTag> getVersionTag();
    @Input @Optional public abstract Property<String> getVersionTitle();
    @Input @Optional public abstract Property<String> getChangelog();
    @Input public abstract Property<String> getDownloadType();
    @Input public abstract ListProperty<String> getMcVersions();
    @Input public abstract ListProperty<String> getSubcategoryIds();
    @Internal public abstract RegularFileProperty getArtifact();
    @InputFiles public abstract ConfigurableFileCollection getArtifacts();

    @Internal
    public NexusMCPublisherExtension getPublisherExtension() {
        return extension;
    }

    public void setPublisherExtension(NexusMCPublisherExtension extension) {
        this.extension = extension;
    }

    @TaskAction
    public void publish() {
        validateRequired("token", getToken());
        validateRequired("resourceId", getResourceId());
        validateRequired("version", getVersion());

        NexusMCApiClient client = new NexusMCApiClient(
            getBaseUrl().get(), getToken().get(), new ObjectMapper()
        );
        java.util.List<NexusMCFileSpec> configuredFiles = extension.getFiles();
        java.util.List<NexusMCApiClient.VersionFile> uploadedFiles = new java.util.ArrayList<>();
        if (configuredFiles.isEmpty()) {
            java.nio.file.Path artifact = getArtifact().get().getAsFile().toPath();
            getLogger().lifecycle("Uploading {} to NexusMC", artifact.getFileName());
            uploadedFiles.add(new NexusMCApiClient.VersionFile(
                client.upload(artifact), true, getMcVersions().get(), getSubcategoryIds().get()
            ));
        } else {
            validateFiles(configuredFiles);
            for (NexusMCFileSpec file : configuredFiles) {
                java.nio.file.Path artifact = file.getArtifact().get().getAsFile().toPath();
                getLogger().lifecycle("Uploading {} to NexusMC", artifact.getFileName());
                uploadedFiles.add(new NexusMCApiClient.VersionFile(
                    client.upload(artifact), file.getPrimary().get(),
                    file.getGameVersions().get(), subcategoryIds(file)
                ));
            }
        }

        NexusMCApiClient.VersionRequest request = new NexusMCApiClient.VersionRequest(
            getVersion().get(), getVersionTag().getOrNull(), optional(getVersionTitle()),
            optional(getChangelog()), getDownloadType().get(), uploadedFiles, getMcVersions().get()
        );
        JsonNode response = client.publishVersion(getResourceId().get(), request);
        getLogger().lifecycle(submissionMessage(getVersion().get(), response));
    }

    static String submissionMessage(String version, JsonNode response) {
        JsonNode id = response.get("id");
        JsonNode status = response.get("status");
        return "Submitted NexusMC version " + version
            + submissionDetails(id, status)
            + ". It may not be visible until NexusMC approves it.";
    }

    private static String submissionDetails(JsonNode id, JsonNode status) {
        if (id == null && status == null) {
            return "";
        }
        if (id == null) {
            return " (status: " + status.asText() + ")";
        }
        return " (id: " + id.asText()
            + (status == null ? "" : ", status: " + status.asText())
            + ")";
    }

    private static String optional(Property<String> property) {
        return property.isPresent() ? property.get() : null;
    }

    static java.util.List<String> subcategoryIds(NexusMCFileSpec file) {
        java.util.List<String> subcategoryIds = file.getSubcategoryIds().get();
        return subcategoryIds.isEmpty() ? file.getLoaders().get() : subcategoryIds;
    }

    private static void validateRequired(String name, Property<String> property) {
        if (!property.isPresent() || property.get().trim().isEmpty()) {
            throw new GradleException("nexusMCPublisher." + name + " must be configured");
        }
    }

    private static void validateFiles(java.util.List<NexusMCFileSpec> files) {
        int primaryFiles = 0;
        for (NexusMCFileSpec file : files) {
            if (!file.getArtifact().isPresent()) {
                throw new GradleException("Every nexusMCPublisher file must configure artifact");
            }
            if (file.getPrimary().get()) {
                primaryFiles++;
            }
        }
        if (primaryFiles != 1) {
            throw new GradleException("nexusMCPublisher files must contain exactly one primary file");
        }
    }
}