package io.github.jd1378.persianfontfixer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ClientVersionsTest {

    @Test
    void serversBefore1_16_2NeedTheVisualForm() {
        assertFalse(ClientVersions.isAtLeast1_16_2("1.8.8-R0.1-SNAPSHOT"));
        assertFalse(ClientVersions.isAtLeast1_16_2("1.12.2-R0.1-SNAPSHOT"));
        assertFalse(ClientVersions.isAtLeast1_16_2("1.16.1-R0.1-SNAPSHOT"));
        assertFalse(ClientVersions.isAtLeast1_16_2("1.16-R0.1-SNAPSHOT"));
    }

    @Test
    void serversFrom1_16_2RenderArabicThemselves() {
        assertTrue(ClientVersions.isAtLeast1_16_2("1.16.2-R0.1-SNAPSHOT"));
        assertTrue(ClientVersions.isAtLeast1_16_2("1.16.5-R0.1-SNAPSHOT"));
        assertTrue(ClientVersions.isAtLeast1_16_2("1.21.4-R0.1-SNAPSHOT"));
        assertTrue(ClientVersions.isAtLeast1_16_2("1.21-R0.1-SNAPSHOT"));
    }

    @Test
    void yearBasedVersionsAreNewer() {
        assertTrue(ClientVersions.isAtLeast1_16_2("26.2-R0.1-SNAPSHOT"));
        assertTrue(ClientVersions.isAtLeast1_16_2("26.1"));
    }

    @Test
    void unknownFormatsAreTreatedAsNew() {
        assertTrue(ClientVersions.isAtLeast1_16_2("future-R0.1"));
    }
}
