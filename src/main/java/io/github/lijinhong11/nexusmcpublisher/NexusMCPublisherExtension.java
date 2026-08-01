package io.github.lijinhong11.nexusmcpublisher;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NexusMCPublisherExtension {
    private final Project project;
    private final Property<String> baseUrl;
    private final Property<String> token;
    private final Property<String> resourceId;
    private final Property<String> version;
    private final Property<VersionTag> versionTag;
    private final Property<String> versionTitle;
    private final Property<String> changelog;
    private final Property<String> downloadType;
    private final ListProperty<String> mcVersions;
    private final ListProperty<String> subcategoryIds;
    private final RegularFileProperty artifact;
    private final ObjectFactory objects;
    private final List<NexusMCFileSpec> files = new ArrayList<>();

    @Inject
    public NexusMCPublisherExtension(Project project, ObjectFactory objects) {
        this.project = project;
        this.objects = objects;
        this.baseUrl = objects.property(String.class);
        this.token = objects.property(String.class);
        this.resourceId = objects.property(String.class);
        this.version = objects.property(String.class);
        this.versionTag = objects.property(VersionTag.class);
        this.versionTitle = objects.property(String.class);
        this.changelog = objects.property(String.class);
        this.downloadType = objects.property(String.class);
        this.mcVersions = objects.listProperty(String.class);
        this.subcategoryIds = objects.listProperty(String.class);
        this.artifact = objects.fileProperty();

        baseUrl.convention("https://www.nexusmc.cn");
        versionTag.convention(VersionTag.RELEASE);
        downloadType.convention("local");
        mcVersions.convention(Collections.emptyList());
        subcategoryIds.convention(Collections.emptyList());
    }

    public Property<String> getBaseUrl() {
        return baseUrl;
    }

    public Property<String> getToken() {
        return token;
    }

    public Property<String> getResourceId() {
        return resourceId;
    }

    public Property<String> getVersion() {
        return version;
    }

    public Property<VersionTag> getVersionTag() {
        return versionTag;
    }

    public Property<String> getVersionTitle() {
        return versionTitle;
    }

    public Property<String> getChangelog() {
        return changelog;
    }

    public Property<String> getDownloadType() {
        return downloadType;
    }

    public ListProperty<String> getMcVersions() {
        return mcVersions;
    }

    public ListProperty<String> getSubcategoryIds() {
        return subcategoryIds;
    }

    public RegularFileProperty getArtifact() {
        return artifact;
    }

    public List<NexusMCFileSpec> getFiles() {
        return Collections.unmodifiableList(files);
    }

    public void artifact(Object path) {
        artifact.set(project.getLayout().file(project.provider(() -> project.file(path))));
    }

    public void file(Action<? super NexusMCFileSpec> action) {
        NexusMCFileSpec file = objects.newInstance(NexusMCFileSpec.class, project);
        action.execute(file);
        files.add(file);
    }
}