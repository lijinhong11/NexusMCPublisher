package io.github.lijinhong11.nexusmcpublisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class NexusMCPublisherPluginTest {
    @Test
    void extensionProvidesSubcategoryIdsForSingleFilePublishing() {
        Project project = ProjectBuilder.builder().build();
        NexusMCPublisherExtension extension = project.getObjects().newInstance(
            NexusMCPublisherExtension.class,
            project
        );

        extension.getSubcategoryIds().set(Collections.singletonList("paper"));

        assertEquals(Collections.singletonList("paper"), extension.getSubcategoryIds().get());
    }

    @Test
    void subcategoryIdsTakePriorityOverTheLegacyLoadersAlias() {
        Project project = ProjectBuilder.builder().build();
        NexusMCFileSpec file = project.getObjects().newInstance(NexusMCFileSpec.class, project);
        file.getSubcategoryIds().set(Collections.singletonList("paper"));
        file.getLoaders().set(Collections.singletonList("legacy-loader"));

        assertEquals(Collections.singletonList("paper"), PublishToNexusMCTask.subcategoryIds(file));

        file.getSubcategoryIds().set(Collections.emptyList());
        assertEquals(Collections.singletonList("legacy-loader"), PublishToNexusMCTask.subcategoryIds(file));
    }

    @Test
    void submissionMessageDoesNotClaimTheVersionIsAlreadyPublished() throws Exception {
        String message = PublishToNexusMCTask.submissionMessage(
            "1.2.2",
            new ObjectMapper().readTree("{\"id\":\"version-id\",\"status\":\"pending\"}")
        );

        assertEquals(
            "Submitted NexusMC version 1.2.2 (id: version-id, status: pending). It may not be visible until NexusMC approves it.",
            message
        );
        assertFalse(message.startsWith("Published"));
    }

    @Test
    void registersTheExtensionAndPublishingTask() {
        Project project = ProjectBuilder.builder().build();

        project.getPluginManager().apply("io.github.lijinhong11.nexusmc-publisher");

        assertNotNull(project.getExtensions().findByType(NexusMCPublisherExtension.class));
        Task task = project.getTasks().getByName("publishToNexusMC");
        assertInstanceOf(PublishToNexusMCTask.class, task);
    }

    @Test
    void extensionCollectsMultipleFileSpecifications() {
        Project project = ProjectBuilder.builder().build();
        project.getPluginManager().apply("io.github.lijinhong11.nexusmc-publisher");
        NexusMCPublisherExtension extension = project.getExtensions().getByType(NexusMCPublisherExtension.class);

        extension.file(file -> {
            file.artifact("build/paper.jar");
            file.getPrimary().set(true);
            file.getGameVersions().set(Collections.singletonList("1.21.4"));
            file.getSubcategoryIds().set(Collections.singletonList("paper"));
        });
        extension.file(file -> {
            file.artifact("build/fabric.jar");
            file.getLoaders().set(Collections.singletonList("fabric"));
        });

        assertEquals(2, extension.getFiles().size());
        assertTrue(extension.getFiles().get(0).getPrimary().get());
        assertEquals("paper", extension.getFiles().get(0).getSubcategoryIds().get().get(0));
        assertEquals("fabric", extension.getFiles().get(1).getLoaders().get().get(0));
    }
}