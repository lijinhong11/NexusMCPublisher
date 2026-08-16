package io.github.lijinhong11.nexusmcpublisher;

import io.github.lijinhong11.tiptapmarkdown.MarkdownManager;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.jvm.tasks.Jar;

public class NexusMCPublisherPlugin implements Plugin<Project> {
    public static final MarkdownManager markdownManager = new MarkdownManager();

    @Override
    public void apply(Project project) {
        NexusMCPublisherExtension extension = project.getExtensions().create(
                "nexusMCPublisher", NexusMCPublisherExtension.class, project
        );

        extension.getToken().convention(
                project.getProviders().environmentVariable("NEXUSMC_API_TOKEN")
                        .orElse(project.getProviders().gradleProperty("nexusMCToken"))
        );
        extension.getVersion().convention(project.provider(() -> String.valueOf(project.getVersion())));
        extension.getVersionTitle().convention(extension.getVersion().map(value -> "Version " + value));

        TaskProvider<PublishToNexusMCTask> publish = project.getTasks().register(
                "publishToNexusMC", PublishToNexusMCTask.class, task -> {
                    task.setGroup("publishing");
                    task.setDescription("Uploads an artifact and publishes a new NexusMC resource version.");
                    task.getBaseUrl().convention(extension.getBaseUrl());
                    task.getToken().convention(extension.getToken());
                    task.getResourceId().convention(extension.getResourceId());
                    task.getVersion().convention(extension.getVersion());
                    task.getVersionTag().convention(extension.getVersionTag());
                    task.getVersionTitle().convention(extension.getVersionTitle());
                    task.getChangelog().convention(extension.getChangelog());
                    task.getDownloadType().convention(extension.getDownloadType());
                    task.getMcVersions().convention(extension.getMcVersions());
                    task.getSubcategoryIds().convention(extension.getSubcategoryIds());
                    task.getArtifact().convention(extension.getArtifact());
                    task.setPublisherExtension(extension);
                    task.getArtifacts().from(project.provider(() -> {
                        if (extension.getFiles().isEmpty()) {
                            return extension.getArtifact().isPresent()
                                    ? java.util.Collections.singletonList(extension.getArtifact().get().getAsFile())
                                    : java.util.Collections.emptyList();
                        }
                        java.util.List<java.io.File> files = new java.util.ArrayList<>();
                        for (NexusMCFileSpec file : extension.getFiles()) {
                            if (file.getArtifact().isPresent()) {
                                files.add(file.getArtifact().get().getAsFile());
                            }
                        }
                        return files;
                    }));
                }
        );

        project.getPluginManager().withPlugin("java", ignored -> {
            TaskProvider<Jar> jar = project.getTasks().named("jar", Jar.class);
            extension.getArtifact().convention(jar.flatMap(Jar::getArchiveFile));
            publish.configure(task -> task.dependsOn(jar));
        });
    }
}