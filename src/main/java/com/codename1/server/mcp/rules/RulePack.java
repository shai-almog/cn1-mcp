package com.codename1.server.mcp.rules;

import java.util.List;
import java.util.regex.Pattern;

/** Central repository of linting rules expressed as regular expressions. */
public final class RulePack {
  private RulePack() {}

  // Strict deny-list regexes (line-based import + FQNs)
  public static final List<Pattern> FORBIDDEN = List.of(
      Pattern.compile("^\\s*import\\s+java\\.awt\\..*;", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^\\s*import\\s+javax\\.swing\\..*;", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^\\s*import\\s+javafx\\..*;", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^\\s*import\\s+java\\.nio\\.file\\..*;", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^\\s*import\\s+java\\.lang\\.reflect\\..*;", Pattern.CASE_INSENSITIVE),
      Pattern.compile("^\\s*import\\s+java\\.sql\\..*;", Pattern.CASE_INSENSITIVE));

  public static final List<Pattern> FORBIDDEN_FQNS = List.of(
      Pattern.compile("java\\.lang\\.Process"),
      Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec"),
      Pattern.compile("java\\.net\\.HttpURLConnection"));

  public static final Pattern ABSOLUTE_PATH =
      Pattern.compile("([A-Za-z]:\\\\\\\\|/Users/|/home/|/tmp/)");

  public static final Pattern UI_MUTATION =
      Pattern.compile("\\.(add|setText|revalidate|show|setUIID|getStyle\\(\\)\\.set)[\\s\\r\\n]*\\(");

  public static final Pattern CALLS_SERIAL =
      Pattern.compile(
          "Display\\.getInstance\\(\\)\\." + "(callSerially|callSeriallyAndWait)\\s*\\(");

  public static final Pattern RAW_THREAD = Pattern.compile("new\\s+Thread\\s*\\(");

  public static final Pattern SLEEP = Pattern.compile("Thread\\.sleep\\s*\\(");
}
