package com.codename1.server.mcp.service;

import com.codename1.server.mcp.dto.LintRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LintServiceTest {

    private final LintService lint = new LintService();

    @Test
    void forbiddenImportIsError() {
        String code = "import javax.swing.JButton;\nclass X {}";
        var res = lint.lint(new LintRequest(code, "java", List.of()));
        assertFalse(res.ok());
        assertTrue(res.diagnostics().stream().anyMatch(d -> d.ruleId().equals("CN1_FORBIDDEN_IMPORT")));
    }

    @Test
    void edtViolationIsError() {
        String code = "class X { void a(com.codename1.ui.Form f){ f.show(); } }";
        var res = lint.lint(new LintRequest(code, "java", List.of()));
        assertFalse(res.ok());
        assertTrue(res.diagnostics().stream().anyMatch(d -> d.ruleId().equals("CN1_EDT_RULE")));
    }

    @Test
    void edtWrappedIsOk() {
        String code = """
      import com.codename1.ui.Display;
      class X { void a(com.codename1.ui.Form f){
        Display.getInstance().callSerially(() -> f.show());
      }}""";
        var res = lint.lint(new LintRequest(code, "java", List.of()));
        assertTrue(res.ok(), () -> "Diagnostics: " + res.diagnostics());
    }

    @Test
    void rawThreadIsError() {
        String code = "class X { void a(){ new Thread(() -> {}).start(); } }";
        var res = lint.lint(new LintRequest(code, "java", List.of()));
        assertFalse(res.ok());
        assertTrue(res.diagnostics().stream().anyMatch(d -> d.ruleId().equals("CN1_RAW_THREADS")));
    }

    @Test
    void absolutePathsAreErrorInStrict() {
        String code = "class X { void a(){ String p = \"/Users/me/file\"; } }";
        var res = lint.lint(new LintRequest(code, "java", List.of()));
        assertFalse(res.ok());
        assertTrue(res.diagnostics().stream().anyMatch(d -> d.ruleId().equals("CN1_STORAGE_PATHS")));
    }
}