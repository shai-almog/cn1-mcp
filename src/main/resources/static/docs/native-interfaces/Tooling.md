# Using the MCP Tool

Invoke the MCP tool with a payload similar to:

```json
{
  "tool": "cn1_generate_native_stubs",
  "arguments": {
    "qualifiedInterfaceName": "com.mycompany.myapp.MyNative",
    "sources": [
      {
        "path": "src/com/mycompany/myapp/MyNative.java",
        "content": "..."
      }
    ]
  }
}
```

The response contains a `files` array whose entries provide the relative path and text content for each generated stub. Save them into your project, implement the platform logic, and rebuild your Codename One app.

## Fetch Build Hint Examples

Ask the MCP server for build hint suggestions with the snippets endpoint:

```json
{
  "tool": "cn1_search_snippets",
  "arguments": {
    "topic": "build-hints"
  }
}
```

The reply lists ready-made key/value pairs you can paste into Codename One Settings
or `codenameone_settings.properties` to configure permissions, privacy strings,
and other manifest tweaks. The topic aggregates every markdown file in
`static/docs/snippets`, so Android, iOS, desktop, JavaScript, and legacy
categories all arrive in a single response for quick reference.
