package com.codename1.server.mcp.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

/**
 * Port of the Codename One native stub generator that operates purely in
 * memory. The implementation mirrors the behaviour of the Ant-based tooling in
 * the Codename One build server so that agents can generate the boilerplate for
 * all supported native platforms directly from the MCP.
 */
class NativeStubGenerator {
    private static final String NATIVE_INTERFACE_FQN = "com.codename1.system.NativeInterface";

    private final Class<?> nativeInterface;
    private final List<Method> declaredMethods;

    NativeStubGenerator(Class<?> nativeInterface) {
        this.nativeInterface = nativeInterface;
        this.declaredMethods = Arrays.stream(nativeInterface.getMethods())
                .filter(m -> !m.getDeclaringClass().equals(Object.class))
                .toList();
    }

    String verify() {
        if (!nativeInterface.isInterface()) {
            return "Not an interface! Native interfaces must be interfaces.";
        }

        if (!isSubinterfaceOfNativeInterface(nativeInterface)) {
            return "The interface MUST implement NativeInterface!";
        }

        if ((nativeInterface.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
            return "The interface must be a public interface and not an inner class";
        }

        if (nativeInterface.getEnclosingClass() != null) {
            return "The interface must be a public interface and not an inner class";
        }

        if (nativeInterface.getPackage() == null) {
            return "The interface must declare a package";
        }

        Set<String> methodNames = new HashSet<>();
        for (Method m : declaredMethods) {
            String lowerCaseName = m.getName().toLowerCase(Locale.ROOT);
            if (!methodNames.add(lowerCaseName)) {
                return "A method with the same name exists for the method " + m.getName()
                        + ", notice that duplicate names (even with different case) aren't supported!";
            }

            if (m.getExceptionTypes().length > 0) {
                return "Exceptions aren't supported when communicating with native interfaces, in the method " + m.getName();
            }

            if (m.getName().equalsIgnoreCase("init")) {
                return "init() is a reserved method in iOS (a constructor of sort) naming a method init will not work properly.";
            }

            if (!isValidType(m.getReturnType())) {
                return "Unsupported return type  " + m.getReturnType().getSimpleName() + " in the method " + m.getName();
            }

            for (Class<?> arg : m.getParameterTypes()) {
                if (!isValidType(arg)) {
                    return "Unsupported argument type  " + arg.getSimpleName() + " in the method " + m.getName();
                }
            }
        }

        return null;
    }

    Map<String, String> generate() {
        Map<String, String> files = new LinkedHashMap<>();
        try {
            addJavaFile(files, "android", "android.view.View", false);
            addJavaFile(files, "javase", "com.codename1.ui.PeerComponent", true);
            addJavaFile(files, "rim", "net.rim.device.api.ui.Field", false);
            addJavaFile(files, "j2me", "Object", false);
            addCSFile(files, "win", "FrameworkElement");
            addIOSFiles(files);
            addJavaScriptFile(files);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
        return files;
    }

    private void addJavaFile(Map<String, String> files, String platformDir, String peerComponentType, boolean impl) throws IOException {
        String pkg = nativeInterface.getPackage().getName();
        String className = nativeInterface.getSimpleName() + "Impl";
        StringBuilder builder = new StringBuilder("package " + pkg + ";\n\n");
        builder.append("public class ").append(className);
        builder.append(impl ? " implements " + nativeInterface.getName() + "{\n" : " {\n");

        for (Method m : declaredMethods) {
            builder.append("    public ");
            Class<?> returnType = m.getReturnType();
            builder.append(returnType.getName().equals("com.codename1.ui.PeerComponent") ? peerComponentType : getJavaTypeName(returnType));
            builder.append(' ').append(m.getName()).append('(');

            Class<?>[] params = m.getParameterTypes();
            builder.append(IntStream.range(0, params.length)
                    .mapToObj(i -> {
                        Class<?> arg = params[i];
                        String typeName = arg.getName().equals("com.codename1.ui.PeerComponent") ? peerComponentType : getJavaTypeName(arg);
                        return typeName + " param" + (i == 0 ? "" : Integer.toString(i));
                    })
                    .collect(java.util.stream.Collectors.joining(", ")));
            builder.append(") {\n");

            builder.append("        ").append(defaultReturnStatement(returnType));

            builder.append("    }\n\n");
        }
        builder.append("}\n");

        String path = platformDir + "/" + pkg.replace('.', '/') + "/" + className + ".java";
        files.put(path, builder.toString());
    }

    private void addCSFile(Map<String, String> files, String platformDir, String peerComponentType) throws IOException {
        String pkg = nativeInterface.getPackage().getName();
        StringBuilder builder = new StringBuilder();
        builder.append("namespace ").append(pkg).append("{\r\n\r\n");
        builder.append("public class ").append(nativeInterface.getSimpleName()).append("Impl : I")
                .append(nativeInterface.getSimpleName()).append("Impl {\r\n");

        for (Method m : declaredMethods) {
            builder.append("    public ");
            builder.append(javaTypeToCSharpType(m.getReturnType()));
            builder.append(' ').append(m.getName()).append('(');
            Class<?>[] params = m.getParameterTypes();
            builder.append(IntStream.range(0, params.length)
                    .mapToObj(i -> {
                        Class<?> arg = params[i];
                        String typeName = switch (arg.getName()) {
                            case "com.codename1.ui.PeerComponent" -> "object";
                            default -> switch (arg) {
                                case Class<?> t when t == boolean.class || t == Boolean.class || t == Boolean.TYPE -> "bool";
                                default -> getJavaTypeName(arg);
                            };
                        };
                        return typeName + " param" + (i == 0 ? "" : Integer.toString(i));
                    })
                    .collect(java.util.stream.Collectors.joining(", ")));
            builder.append(") {\n");
            builder.append("        ").append(defaultReturnStatement(m.getReturnType()));
            builder.append("    }\n\n");
        }
        builder.append("}\r\n}\r\n");

        String path = platformDir + "/" + pkg.replace('.', '/') + "/" + nativeInterface.getSimpleName() + "Impl.cs";
        files.put(path, builder.toString());
    }

    private void addIOSFiles(Map<String, String> files) throws IOException {
        String prefix = nativeInterface.getName().replace('.', '_') + "Impl";
        StringBuilder header = new StringBuilder();
        header.append("#import <Foundation/Foundation.h>\n\n");
        header.append("@interface ").append(prefix).append(" : NSObject {\n}\n\n");
        for (Method m : declaredMethods) {
            header.append("-(").append(javaTypeToObjectiveCType(m.getReturnType())).append(')').append(m.getName());
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 0) {
                header.append(";\n");
            } else {
                header.append(":(").append(javaTypeToObjectiveCType(params[0])).append(")param");
                if (params.length == 1) {
                    header.append(";\n");
                } else {
                    for (int i = 1; i < params.length; i++) {
                        header.append(" param").append(i).append(":(").append(javaTypeToObjectiveCType(params[i])).append(")param").append(i);
                    }
                    header.append(";\n");
                }
            }
        }
        header.append("@end\n");

        StringBuilder impl = new StringBuilder();
        impl.append("#import \"").append(prefix).append(".h\"\n\n");
        impl.append("@implementation ").append(prefix).append("\n\n");

        for (Method m : declaredMethods) {
            impl.append("-(").append(javaTypeToObjectiveCType(m.getReturnType())).append(')').append(m.getName());
            Class<?>[] params = m.getParameterTypes();
            if (params.length == 0) {
                impl.append("{\n");
            } else {
                impl.append(":(").append(javaTypeToObjectiveCType(params[0])).append(")param");
                if (params.length == 1) {
                    impl.append("{\n");
                } else {
                    for (int i = 1; i < params.length; i++) {
                        impl.append(" param").append(i).append(":(").append(javaTypeToObjectiveCType(params[i])).append(")param").append(i);
                    }
                    impl.append("{\n");
                }
            }
            impl.append("    ").append(defaultObjectiveCReturnStatement(m.getReturnType()));
            impl.append("}\n\n");
        }
        impl.append("@end\n");

        files.put("ios/" + prefix + ".h", header.toString());
        files.put("ios/" + prefix + ".m", impl.toString());
    }

    private void addJavaScriptFile(Map<String, String> files) throws IOException {
        StringBuilder builder = new StringBuilder();
        builder.append("(function(exports){\n\n");
        builder.append("var o = {};\n\n");

        for (Method m : declaredMethods) {
            builder.append("    o.").append(m.getName());
            builder.append('_');
            for (Class<?> param : m.getParameterTypes()) {
                builder.append('_');
                if (param.getName().equals("com.codename1.ui.PeerComponent")) {
                    builder.append("com_codename1_ui_PeerComponent");
                } else {
                    builder.append(typeToXMLVMJavaName(param));
                }
            }
            builder.append(" = function(");
            Class<?>[] params = m.getParameterTypes();
            if (params.length > 0) {
                builder.append("param1");
                for (int i = 1; i < params.length; i++) {
                    builder.append(", param").append(i + 1);
                }
                builder.append(", callback) {\n");
            } else {
                builder.append("callback) {\n");
            }

            builder.append("        ");
            builder.append(m.getName().equals("isSupported") ? "callback.complete(false);" : "callback.error(new Error(\"Not implemented yet\"));");
            builder.append("\n");
            builder.append("    };\n\n");
        }
        builder.append("exports.").append(nativeInterface.getName().replace('.', '_')).append("= o;\n\n");
        builder.append("})(cn1_get_native_interfaces());\n");

        files.put("javascript/" + nativeInterface.getName().replace('.', '_') + ".js", builder.toString());
    }

    private static boolean isSubinterfaceOfNativeInterface(Class<?> iface) {
        for (Class<?> current : iface.getInterfaces()) {
            if (current.getName().equals(NATIVE_INTERFACE_FQN)) {
                return true;
            }
            if (isSubinterfaceOfNativeInterface(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidType(Class<?> cls) {
        return switch (cls) {
            case Class<?> type when type.isPrimitive() -> true;
            case Class<?> type when type.isArray() -> type.getComponentType().isPrimitive();
            case Class<?> type when type == String.class -> true;
            case Class<?> type when type.getName().equals("com.codename1.ui.PeerComponent") -> true;
            default -> false;
        };
    }

    private static String defaultReturnStatement(Class<?> returnType) {
        return switch (returnType) {
            case Class<?> type when type == Void.TYPE || type == Void.class -> "// TODO implement\n";
            case Class<?> type when type == String.class
                    || type.getName().equals("com.codename1.ui.PeerComponent")
                    || type.isArray() -> "return null;\n";
            case Class<?> type when type == Boolean.class || type == Boolean.TYPE -> "return false;\n";
            case Class<?> type when type == Character.class || type == Character.TYPE -> "return (char)0;\n";
            case Class<?> type when type == Byte.class || type == Byte.TYPE -> "return (byte)0;\n";
            case Class<?> type when type == Short.class || type == Short.TYPE -> "return (short)0;\n";
            default -> "return 0;\n";
        };
    }

    private static String defaultObjectiveCReturnStatement(Class<?> returnType) {
        return switch (returnType) {
            case Class<?> type when type == Void.TYPE || type == Void.class -> "// TODO implement\n";
            case Class<?> type when type == String.class || type.isArray() -> "return nil;\n";
            case Class<?> type when type.getName().equals("com.codename1.ui.PeerComponent") -> "return NULL;\n";
            case Class<?> type when type == Boolean.class || type == Boolean.TYPE -> "return NO;\n";
            default -> "return 0;\n";
        };
    }

    private static String getJavaTypeName(Class<?> type) {
        if (type.isArray()) {
            return getJavaTypeName(type.getComponentType()) + "[]";
        }
        return type.getSimpleName();
    }

    private static String javaTypeToObjectiveCType(Class<?> type) {
        return switch (type) {
            case Class<?> t when t == String.class -> "NSString*";
            case Class<?> t when t.isArray() -> "NSData*";
            case Class<?> t when t == Integer.class || t == Integer.TYPE -> "int";
            case Class<?> t when t == Long.class || t == Long.TYPE -> "long long";
            case Class<?> t when t == Byte.class || t == Byte.TYPE -> "char";
            case Class<?> t when t == Short.class || t == Short.TYPE -> "short";
            case Class<?> t when t == Character.class || t == Character.TYPE -> "int";
            case Class<?> t when t == Boolean.class || t == Boolean.TYPE -> "BOOL";
            case Class<?> t when t == Float.class || t == Float.TYPE -> "float";
            case Class<?> t when t == Double.class || t == Double.TYPE -> "double";
            case Class<?> t when t == Void.class || t == Void.TYPE -> "void";
            default -> "void*";
        };
    }

    private static String javaTypeToCSharpType(Class<?> type) {
        return switch (type) {
            case Class<?> t when t.getName().equals("com.codename1.ui.PeerComponent") -> "object";
            case Class<?> t when t == String.class -> "string";
            case Class<?> t when t.isArray() -> javaTypeToCSharpType(t.getComponentType()) + "[]";
            case Class<?> t when t == Boolean.class || t == Boolean.TYPE -> "bool";
            case Class<?> t when t == Character.class || t == Character.TYPE -> "char";
            case Class<?> t when t == Byte.class || t == Byte.TYPE -> "byte";
            case Class<?> t when t == Short.class || t == Short.TYPE -> "short";
            case Class<?> t when t == Integer.class || t == Integer.TYPE -> "int";
            case Class<?> t when t == Long.class || t == Long.TYPE -> "long";
            case Class<?> t when t == Float.class || t == Float.TYPE -> "float";
            case Class<?> t when t == Double.class || t == Double.TYPE -> "double";
            case Class<?> t when t == Void.class || t == Void.TYPE -> "void";
            default -> type.getSimpleName();
        };
    }

    private static String typeToXMLVMJavaName(Class<?> type) {
        return type.isArray()
                ? getSimpleNameWithJavaLang(type.getComponentType()).replace('.', '_') + "_1ARRAY"
                : getSimpleNameWithJavaLang(type).replace('.', '_');
    }

    private static String getSimpleNameWithJavaLang(Class<?> c) {
        return switch (c) {
            case Class<?> type when type.isPrimitive() -> type.getSimpleName();
            case Class<?> type when type.isArray() -> getSimpleNameWithJavaLang(type.getComponentType()) + "[]";
            case Class<?> type when type.getName().startsWith("java.lang.") -> type.getName();
            default -> c.getSimpleName();
        };
    }
}
