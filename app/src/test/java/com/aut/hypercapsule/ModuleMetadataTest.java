package com.aut.hypercapsule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public final class ModuleMetadataTest {
    @Test
    public void declaresApi102SystemUiModule() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = openResource("META-INF/xposed/module.prop")) {
            properties.load(stream);
        }

        assertEquals("102", properties.getProperty("minApiVersion"));
        assertEquals("102", properties.getProperty("targetApiVersion"));
        assertEquals("true", properties.getProperty("staticScope"));
        assertEquals("protective", properties.getProperty("exceptionMode"));
        assertEquals(
                "com.aut.hypercapsule.HyperCapsuleModule",
                readResource("META-INF/xposed/java_init.list").trim());
        assertEquals(
                "com.android.systemui",
                readResource("META-INF/xposed/scope.list").trim());
    }

    private static String readResource(String name) throws IOException {
        try (InputStream stream = openResource(name)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream openResource(String name) {
        InputStream stream = ModuleMetadataTest.class.getClassLoader().getResourceAsStream(name);
        assertNotNull("Missing resource: " + name, stream);
        return stream;
    }
}
