package io.github.lijinhong11.nexusmcpublisher;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionTagTest {
    @Test
    void mapsEnumValuesToNexusMCApiKeys() {
        assertEquals("releases", VersionTag.RELEASE.getApiKey());
        assertEquals("beta", VersionTag.BETA.getApiKey());
        assertEquals("alpha", VersionTag.ALPHA.getApiKey());
    }
}