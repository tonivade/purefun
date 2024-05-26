/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.processor;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Nullable;

@SupportedAnnotationTypes("com.github.tonivade.purefun.HigherKind")
public class HigherKindProcessor extends AbstractProcessor {

  private static final String GENERATED = "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")";

  private static final String JAVAX_ANNOTATION_GENERATED = "javax.annotation.Generated";
  private static final String JAVAX_ANNOTATION_PROCESSING_GENERATED = "javax.annotation.processing.Generated";

  private static final String KIND = "com.github.tonivade.purefun.Kind";
  private static final String END = "}";

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            format("@%s found at %s", annotation.getSimpleName(), element.getSimpleName()));
        try {
          generate((TypeElement) element);
        } catch (IOException | RuntimeException e) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "error generating code", element);
        }
      }
    }
    return false;
  }

  private void generate(TypeElement element) throws IOException {
    String qualifiedName = element.getQualifiedName().toString();
    int lastIndexOf = qualifiedName.lastIndexOf('.');
    String packageName;
    String className;

    HigherKind annotation = element.getAnnotation(HigherKind.class);

    if (lastIndexOf > 0) {
      packageName =  qualifiedName.substring(0, lastIndexOf);
      className = qualifiedName.substring(lastIndexOf + 1);
    } else {
      packageName = null;
      className = qualifiedName;
    }
    String typeOfName = annotation.value().isEmpty() ? className + "Of" : annotation.value();
    writeTypeOf(packageName, className, typeOfName, element.getTypeParameters());
  }

  private void writeTypeOf(@Nullable String packageName, String className, String typeOfName,
    List<? extends TypeParameterElement> types) throws IOException {
    JavaFileObject typeOfFile = createFile(packageName, typeOfName);
    try (var writer = new PrintWriter(typeOfFile.openWriter())) {
      switch (types.size()) {
        case 1 -> generate1(writer, packageName, className, typeOfName, types);
        case 2 -> generate2(writer, packageName, className, typeOfName, types);
        case 3 -> generate3(writer, packageName, className, typeOfName, types);
        default -> throw new UnsupportedOperationException("too many params: " + packageName + "." + className);
      }
    }
  }

  private void generate1(PrintWriter writer, @Nullable String packageName, String className,
      String typeOfName, List<? extends TypeParameterElement> list) {
    String higher1 = "Kind<" + className + "<?>, A>";
    String higher1Wildcard = "Kind<" + className + "<?>, ? extends A>";
    String aType = type("A", list.get(0));
    String typeParams = "<" + aType + ">";
    String typeOfNameWithParams = typeOfName + typeParams;
    if (packageName != null) {
      writer.println(packageName(packageName));
      writer.println();
    }
    writer.println(importClass(KIND));
    writer.println(importClass(generated()));
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(className, typeOfNameWithParams, higher1));
    writer.println();
    narrowK1(writer, className, aType, higher1Wildcard);
    writer.println(END);
  }

  private void generate2(PrintWriter writer, @Nullable String packageName, String className,
      String typeOfName, List<? extends TypeParameterElement> list) {
    String higher2 = "Kind<" + className + "<A, ?>, B>";
    String higher1Wildcard = "Kind<" + className + "<A, ?>, ? extends B>";
    String aType = type("A", list.get(0));
    String bType = type("B", list.get(1));
    String typeParams = "<" + aType + ", " + bType + ">";
    String typeOfNameWithParams = typeOfName + typeParams;
    if (packageName != null) {
      writer.println(packageName(packageName));
      writer.println();
    }
    writer.println();
    writer.println(importClass(KIND));
    writer.println(importClass(generated()));
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(className, typeOfNameWithParams, higher2));
    writer.println();
    narrowK2(writer, className, aType, bType, higher1Wildcard);
    writer.println(END);
  }

  private void generate3(PrintWriter writer, @Nullable String packageName, String className,
      String typeOfName, List<? extends TypeParameterElement> list) {
    String higher3 = "Kind<" + className + "<A, B, ?>, C>";
    String higher1Wildcard = "Kind<" + className + "<A, B, ?>, ? extends C>";
    String aType = type("A", list.get(0));
    String bType = type("B", list.get(1));
    String cType = type("C", list.get(2));
    String typeParams = "<" + aType + ", " + bType + ", " + cType + ">";
    String typeOfNameWithParams = typeOfName + typeParams;
    if (packageName != null) {
      writer.println(packageName(packageName));
      writer.println();
    }
    writer.println();
    writer.println(importClass(KIND));
    writer.println(importClass(generated()));
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(className, typeOfNameWithParams, higher3));
    writer.println();
    narrowK3(writer, className, aType, bType, cType, higher1Wildcard);
    writer.println(END);
  }

  private JavaFileObject createFile(@Nullable String packageName, String className) throws IOException {
    String qualifiedName = packageName != null ? packageName + "." + className : className;
    return processingEnv.getFiler().createSourceFile(qualifiedName);
  }

  private String generated() {
    if (processingEnv.getSourceVersion() == SourceVersion.RELEASE_8) {
      return JAVAX_ANNOTATION_GENERATED;
    }
    return JAVAX_ANNOTATION_PROCESSING_GENERATED;
  }

  private static void narrowK1(PrintWriter writer, String className, String aType, String hkt) {
    narrowK(writer, className, "<" + aType + ">", className + "<A>", hkt);
  }

  private static void narrowK2(PrintWriter writer, String className, String aType, String bType, String hkt) {
    narrowK(writer, className, "<" + aType + ", " + bType + ">", className + "<A, B>", hkt);
  }

  private static void narrowK3(PrintWriter writer, String className, String aType, String bType, String cType, String hkt) {
    narrowK(writer, className, "<" + aType + ", " + bType + ", " + cType + ">", className + "<A, B, C>", hkt);
  }

  private static void narrowK(PrintWriter writer, String className, String types, String returnType, String param) {
    writer.println("  @SuppressWarnings(\"unchecked\")");
    writer.println("  static " + types + " " + returnType + " to" + className + "(" + param + " value) {");
    writer.println("    return (" + returnType + ") value;");
    writer.println("  }");
    writer.println();
  }

  private static String typeOfClass(String typeName, String typeOfName, String type) {
    return "public sealed interface " + typeOfName + " extends " + type + " permits " + typeName + " {";
  }

  private static String type(String name, TypeParameterElement type1) {
    String bounds = bounds(type1);
    return !bounds.isEmpty() ? name + " extends " + bounds : name;
  }

  private static String bounds(TypeParameterElement typeParameterElement) {
    return typeParameterElement.getBounds().stream()
      .map(Object::toString)
      .filter(type -> !type.equals(Object.class.getName()))
      .collect(joining(","));
  }

  private static String packageName(String packageName) {
    return "package " + packageName + ";";
  }

  private static String importClass(String className) {
    return "import " + className + ";";
  }
}
