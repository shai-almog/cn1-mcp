# Native Interface Type Mapping

Codename One limits native interface signatures so parameters and return values can be translated across platform toolchains. The table below summarizes how each supported Java type maps to the Android, Java SE, iOS, and Windows implementations.

| Java Type       | Android             | Java SE              | iOS / Objective-C        | Windows (C#)     |
|-----------------|---------------------|----------------------|--------------------------|------------------|
| `byte`          | `byte`              | `byte`               | `char`                   | `sbyte`          |
| `boolean`       | `boolean`           | `boolean`            | `BOOL`                   | `bool`           |
| `char`          | `char`              | `char`               | `int`                    | `char`           |
| `short`         | `short`             | `short`              | `short`                  | `short`          |
| `int`           | `int`               | `int`                | `int`                    | `int`            |
| `long`          | `long`              | `long`               | `long long`              | `long`           |
| `float`         | `float`             | `float`              | `float`                  | `float`          |
| `double`        | `double`            | `double`             | `double`                 | `double`         |
| `String`        | `String`            | `String`             | `NSString*`              | `string`         |
| `byte[]`        | `byte[]`            | `byte[]`             | `NSData*`                | `sbyte[]`        |
| `boolean[]`     | `boolean[]`         | `boolean[]`          | `NSData*`                | `bool[]`         |
| `char[]`        | `char[]`            | `char[]`             | `NSData*`                | `char[]`         |
| `short[]`       | `short[]`           | `short[]`            | `NSData*`                | `short[]`        |
| `int[]`         | `int[]`             | `int[]`              | `NSData*`                | `int[]`          |
| `long[]`        | `long[]`            | `long[]`             | `NSData*`                | `long[]`         |
| `float[]`       | `float[]`           | `float[]`            | `NSData*`                | `float[]`        |
| `double[]`      | `double[]`          | `double[]`           | `NSData*`                | `double[]`       |
| `PeerComponent` | `android.view.View` | `PeerComponent`      | `void*` (expect `UIView*`) | `FrameworkElement` |

JavaScript bridges are dynamically typed. The generated bridge uses XMLVM-style naming and delivers results via callbacks (`callback.complete(value)` / `callback.error(error)`).
