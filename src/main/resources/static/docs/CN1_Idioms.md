# Codename One Idioms — Do This, Not That

## EDT Wrapping
**Bad**
```java
form.show(); // Anywhere
```

**Good**
```java
CN.callSerially(() -> form.show());
```

## Async Networking
**Bad**
```java
HttpURLConnection c = (HttpURLConnection)new URL(url).openConnection();
int code = c.getResponseCode(); // blocks
```

**Good**
```java
Rest.get(url).acceptJson().onComplete(res -> {
    Display.getInstance().callSerially(() -> {
        // update UI with res.getResponseData()
    });
}).send();
```

## Storage
**Bad**
```java
Files.write(Paths.get("/Users/me/file.txt"), data); // desktop path, Files API isn't supported
```

**Good**
```java
Storage.getInstance().writeObject("fileKey", data);
byte[] out = (byte[]) Storage.getInstance().readObject("fileKey");
```

## Theming with UIIDs & CSS
**Bad**
```java
Button b = new Button("Go");
b.getAllStyles().setBgColor(0x3A86FF);
b.getAllStyles().setFgColor(0xFFFFFF);
```

**Good**
```css
// theme.css
.PrimaryButton { 
    bgColor: #3A86FF; 
    color: white; 
}
```

```Java
Button b = new Button("Go");
b.setUIID("PrimaryButton");
```