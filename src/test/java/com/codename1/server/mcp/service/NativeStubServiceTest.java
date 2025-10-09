package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.FileEntry;
import com.codename1.server.mcp.dto.NativeStubRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NativeStubServiceTest {

    private final NativeStubService service = new NativeStubService();

    @Test
    void generatesStubsForValidInterface() {
        String src = """
                package com.mycompany.myapp;

                import com.codename1.system.NativeInterface;

                public interface MyNative extends NativeInterface {
                    String helloWorld(String hi);
                    int add(int a, int b);
                }
                """;

        var request = new NativeStubRequest(List.of(new FileEntry("com/mycompany/myapp/MyNative.java", src)),
                "com.mycompany.myapp.MyNative");

        var response = service.generate(request);
        assertEquals(8, response.files().size());
        assertTrue(response.files().stream().anyMatch(f -> f.path().equals("android/com/mycompany/myapp/MyNativeImpl.java")));
        assertTrue(response.files().stream().anyMatch(f -> f.path().equals("ios/com_mycompany_myapp_MyNativeImpl.h")));

        var androidStub = response.files().stream()
                .filter(f -> f.path().equals("android/com/mycompany/myapp/MyNativeImpl.java"))
                .findFirst()
                .orElseThrow();
        assertTrue(androidStub.content().contains("return null;"));
        assertTrue(androidStub.content().contains("return 0;"));
    }

    @Test
    void rejectsInvalidTypes() {
        String src = """
                package com.mycompany.bad;

                import com.codename1.system.NativeInterface;

                public interface BadNative extends NativeInterface {
                    java.util.Date nope();
                }
                """;

        var request = new NativeStubRequest(List.of(new FileEntry("com/mycompany/bad/BadNative.java", src)),
                "com.mycompany.bad.BadNative");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.generate(request));
        assertTrue(ex.getMessage().contains("Unsupported return type"));
    }
}
