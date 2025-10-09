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
