# Generated Stub Samples

The MCP tool emits safe defaults that compile immediately. For the example `MyNative` interface shown in the overview, the Android stub looks like:

```java
package com.mycompany.myapp;

public class MyNativeImpl {
    public String helloWorld(String message) {
        return null;
    }

    public boolean isSupported() {
        return false;
    }
}
```

The Java SE build includes an `implements MyNative` clause. Objective-C receives header (`com_mycompany_myapp_MyNativeImpl.h`) and implementation (`.m`) files:

```objectivec
#import "com_mycompany_myapp_MyNativeImpl.h"

@implementation com_mycompany_myapp_MyNativeImpl

-(NSString*)helloWorld:(NSString*)message{
    // TODO implement
}

-(BOOL)isSupported{
    return NO;
}

@end
```

The JavaScript bridge follows the callback pattern:

```javascript
(function(exports){

var o = {};

    o.helloWorld__java_lang_String = function(param1, callback) {
        callback.error(new Error("Not implemented yet"));
    };

    o.isSupported_ = function(callback) {
        callback.complete(false);
    };

exports.com_mycompany_myapp_MyNative = o;

})(cn1_get_native_interfaces());
```
