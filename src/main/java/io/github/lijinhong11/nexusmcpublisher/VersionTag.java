package io.github.lijinhong11.nexusmcpublisher;

public enum VersionTag {
    RELEASE("releases"),
    BETA("beta"),
    ALPHA("alpha");

    private final String apiKey;

    VersionTag(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }
}