package io.github.lijinhong11.nexusmcpublisher;

import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NexusMCPublisherPluginTest {
    @Test
    void registersTheExtensionAndPublishingTask() {
        Project project = ProjectBuilder.builder().build();

        project.getPluginManager().apply("io.github.lijinhong11.nexusmc-publisher");

        assertNotNull(project.getExtensions().findByType(NexusMCPublisherExtension.class));
        Task task = project.getTasks().getByName("publishToNexusMC");
        assertTrue(task instanceof PublishToNexusMCTask);
    }

    @Test
    void extensionCollectsMultipleFileSpecifications() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("io.github.lijinhong11.nexusmc-publisher");
        NexusMCPublisherExtension extension = project.getExtensions().getByType(NexusMCPublisherExtension.class);

        extension.file(file -> {
            file.artifact("build/paper.jar");
            file.getPrimary().set(true);
            file.getGameVersions().set(Arrays.asList("1.21.4"));
            file.getLoaders().set(Arrays.asList("paper"));
        });
        extension.file(file -> {
            file.artifact("build/fabric.jar");
            file.getLoaders().set(Arrays.asList("fabric"));
        });

        assertEquals(2, extension.getFiles().size());
        assertTrue(extension.getFiles().get(0).getPrimary().get());
        assertEquals("paper", extension.getFiles().get(0).getLoaders().get().get(0));
        assertEquals("fabric", extension.getFiles().get(1).getLoaders().get().get(0));
    }
}