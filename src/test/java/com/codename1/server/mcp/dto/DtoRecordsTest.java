package com.codename1.server.mcp.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DtoRecordsTest {

    @Test
    void compilerErrorRetainsValues() {
        CompilerError error = new CompilerError("Main.java", 12, 5, "Unexpected token");

        assertEquals("Main.java", error.file());
        assertEquals(12, error.line());
        assertEquals(5, error.col());
        assertEquals("Unexpected token", error.message());
    }

    @Test
    void autoFixRequestDefensivelyCopiesDiagnostics() {
        List<LintDiag> diagnostics = new ArrayList<>();
        diagnostics.add(new LintDiag(
                "CN1_RULE",
                "warning",
                "Describe the issue",
                new Range(new Range.Pos(1, 1), new Range.Pos(1, 5))));

        AutoFixRequest request = new AutoFixRequest("class Test {}", diagnostics);
        diagnostics.add(new LintDiag(
                "OTHER",
                "error",
                "Second entry",
                new Range(new Range.Pos(2, 1), new Range.Pos(2, 3))));

        assertEquals("class Test {}", request.code());
        assertEquals(1, request.diagnostics().size());
        assertNotSame(diagnostics, request.diagnostics());
        assertThrows(UnsupportedOperationException.class, () -> request.diagnostics().add(null));

        AutoFixRequest nullRequest = new AutoFixRequest("class Test {}", null);
        assertNull(nullRequest.diagnostics());
    }

    @Test
    void autoFixResponseProvidesImmutablePatchList() {
        List<Patch> patches = new ArrayList<>();
        patches.add(new Patch("Add method", "@@ -1 +1 @@"));

        AutoFixResponse response = new AutoFixResponse("patched", patches);
        patches.clear();

        assertEquals("patched", response.patchedCode());
        assertEquals(1, response.patches().size());
        assertNotSame(patches, response.patches());
        assertThrows(UnsupportedOperationException.class,
                () -> response.patches().add(new Patch("New", "diff")));

        AutoFixResponse nullResponse = new AutoFixResponse("patched", null);
        assertNull(nullResponse.patches());
    }

    @Test
    void snippetsResponseReturnsDefensiveCopy() {
        List<Snippet> snippets = new ArrayList<>();
        snippets.add(new Snippet("Title", "Description", "System.out.println();"));

        SnippetsResponse response = new SnippetsResponse(snippets);
        snippets.clear();

        assertEquals(1, response.snippets().size());
        assertNotSame(snippets, response.snippets());
        assertThrows(UnsupportedOperationException.class, () -> response.snippets().add(null));

        SnippetsResponse nullResponse = new SnippetsResponse(null);
        assertNull(nullResponse.snippets());
    }

    @Test
    void explainRequestAndResponseExposeValues() {
        ExplainRequest request = new ExplainRequest("RULE_ID");
        ExplainResponse response = new ExplainResponse("Summary", "Bad", "Good");

        assertEquals("RULE_ID", request.ruleId());
        assertEquals("Summary", response.summary());
        assertEquals("Bad", response.bad());
        assertEquals("Good", response.good());
    }

    @Test
    void scaffoldRequestDefensivelyCopiesFeatures() {
        List<String> features = new ArrayList<>(List.of("firebase", "push"));

        ScaffoldRequest request = new ScaffoldRequest("MyApp", "com.example", features);
        features.add("ads");

        assertEquals("MyApp", request.name());
        assertEquals("com.example", request.pkg());
        assertEquals(List.of("firebase", "push"), request.features());
        assertNotSame(features, request.features());
        assertThrows(UnsupportedOperationException.class, () -> request.features().add("more"));

        ScaffoldRequest nullFeatures = new ScaffoldRequest("MyApp", "com.example", null);
        assertNull(nullFeatures.features());
    }

    @Test
    void scaffoldResponseDefensivelyCopiesFiles() {
        List<FileEntry> files = new ArrayList<>();
        files.add(new FileEntry("Main.java", "class Main {}"));

        ScaffoldResponse response = new ScaffoldResponse(files);
        files.clear();

        assertEquals(1, response.files().size());
        assertEquals("Main.java", response.files().getFirst().path());
        assertNotSame(files, response.files());
        assertThrows(UnsupportedOperationException.class,
                () -> response.files().add(new FileEntry("Other.java", "")));

        ScaffoldResponse nullResponse = new ScaffoldResponse(null);
        assertNull(nullResponse.files());
    }

    @Test
    void snippetsRequestStoresTopic() {
        SnippetsRequest request = new SnippetsRequest("Codename One");

        assertEquals("Codename One", request.topic());
    }
}
