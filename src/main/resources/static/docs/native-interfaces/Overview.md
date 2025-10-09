# Codename One Native Interfaces Overview

Codename One native interfaces expose platform APIs, third-party SDKs, and bespoke native UI widgets to Java code. The MCP server bundles a `cn1_generate_native_stubs` tool that validates a native interface and emits boilerplate implementations for every supported Codename One target.

## Quick Start

1. **Declare the interface** in your Codename One project:

    ```java
    package com.mycompany.myapp;

    import com.codename1.system.NativeInterface;

    public interface MyNative extends NativeInterface {
        String helloWorld(String message);
        boolean isSupported();
    }
    ```

2. **Invoke the MCP tool** with the fully-qualified interface name and the source files that contain it.
3. **Copy the generated stubs** into your project's `native/<platform>` folders and fill in the platform logic.
4. **Call the interface** using `NativeLookup.create(MyNative.class)` from your Java code.

## Interface Requirements

Native interfaces must obey a strict signature subset so the code can be mapped across all platforms:

* The type must be a `public`, top-level Java interface.
* It must extend `com.codename1.system.NativeInterface`.
* Method parameters and return types are limited to:
  * Java primitives (`byte`, `boolean`, `char`, `short`, `int`, `long`, `float`, `double`).
  * `String`.
  * One-dimensional arrays of primitives.
  * `com.codename1.ui.PeerComponent` for native peers.
* Methods cannot throw checked or unchecked exceptions.
* Avoid method overloading and the name `init` (reserved on iOS).
* `hashCode`, `equals`, and `toString` will never be bridged to native code.
