package io.github.lijinhong11.nexusmcpublisher;

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.Collections;

public class NexusMCFileSpec {
    private final Project project;
    private final RegularFileProperty artifact;
    private final Property<Boolean> primary;
    private final ListProperty<String> gameVersions;
    private final ListProperty<String> subcategoryIds;
    private final ListProperty<String> loaders;

    @Inject
    public NexusMCFileSpec(Project project, ObjectFactory objects) {
        this.project = project;
        this.artifact = objects.fileProperty();
        this.primary = objects.property(Boolean.class);
        this.gameVersions = objects.listProperty(String.class);
        this.subcategoryIds = objects.listProperty(String.class);
        this.loaders = objects.listProperty(String.class);
        primary.convention(false);
        gameVersions.convention(Collections.emptyList());
        subcategoryIds.convention(Collections.emptyList());
        loaders.convention(Collections.emptyList());
    }

    public RegularFileProperty getArtifact() {
        return artifact;
    }

    public Property<Boolean> getPrimary() {
        return primary;
    }

    public ListProperty<String> getGameVersions() {
        return gameVersions;
    }

    public ListProperty<String> getSubcategoryIds() {
        return subcategoryIds;
    }

    public ListProperty<String> getLoaders() {
        return loaders;
    }

    public void artifact(Object path) {
        artifact.set(project.getLayout().file(project.provider(() -> project.file(path))));
    }
}