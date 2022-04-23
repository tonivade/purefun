/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

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

@SupportedAnnotationTypes("com.github.tonivade.purefun.HigherKind")
public class HigherKindProcessor extends AbstractProcessor {

  private static final String GENERATED = "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")";

  private static final String JAVAX_ANNOTATION_GENERATED = "javax.annotation.Generated";
  private static final String JAVAX_ANNOTATION_PROCESSING_GENERATED = "javax.annotation.processing.Generated";

  private static final String KIND = "com.github.tonivade.purefun.Kind";
  private static final String WITNESS = "com.github.tonivade.purefun.Witness";
  private static final String FIXER = "com.github.tonivade.purefun.Fixer";
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
    return true;
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
    String witnessName = className + "_";
    String typeOfName = annotation.value().isEmpty() ? className + "Of" : annotation.value();
    writeWitness(packageName, witnessName);
    writeTypeOf(packageName, className, typeOfName, witnessName, element.getTypeParameters());
  }

  private void writeWitness(String packageName, String witnessName) throws IOException {
    JavaFileObject witnessFile = createFile(packageName, witnessName);
    try (PrintWriter writer = new PrintWriter(witnessFile.openWriter())) {
      witness(writer, packageName, witnessName);
    }
  }

  private void writeTypeOf(String packageName, String className, String typeOfName, String witnessName,
    List<? extends TypeParameterElement> types) throws IOException {
    JavaFileObject typeOfFile = createFile(packageName, typeOfName);
    try (PrintWriter writer = new PrintWriter(typeOfFile.openWriter())) {
      if (types.size() == 1) {
        generate1(writer, packageName, className, typeOfName, witnessName, types);
      } else if (types.size() == 2) {
        generate2(writer, packageName, className, typeOfName, witnessName, types);
      } else if (types.size() == 3) {
        generate3(writer, packageName, className, typeOfName, witnessName, types);
      } else {
        throw new UnsupportedOperationException("too many params: " + packageName + "." + className);
      }
    }
  }

  private void witness(PrintWriter writer, String packageName, String witnessName) {
    if (packageName != null) {
      writer.println(packageName(packageName));
      writer.println();
    }
    writer.println(import_(WITNESS));
    writer.println(import_(generated()));
    writer.println();
    writer.println(GENERATED);
    writer.println(witnessClass(witnessName));
    writer.println();
    writer.println(privateConstructor(witnessName));
    writer.println();
    writer.println(END);
  }

  private void generate1(PrintWriter writer, String packageName, String className,
      String typeOfName, String kindName, List<? extends TypeParameterElement> list) {
    String higher1 = "Kind<" + kindName + ", A>";
    String higher1Wildcard = "Kind<" + kindName + ", ? extends A>";
    String aType = type("A", list.get(0));
    String typeParams = "<" + aType + ">";
    String typeOfNameWithParams = typeOfName + typeParams;
    if (packageName != null) {
      writer.println(packageName(packageName));
      writer.println();
    }
    writer.println(import_(KIND));
    writer.println(import_(FIXER));
    writer.println(import_(generated()));
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(className, typeOfNameWithParams, higher1));
    writer.println();
    narrowK1(writer, className, aType, higher1Wildcard);
    toTypeOf1(writer, className, aType, higher1, typeOfName);
    writer.println(END);
  }

  private void generate2(PrintWriter writer, String packageName, String className,
      String typeOfName, String kindName, List<? extends TypeParameterElement> list) {
    String higher2 = "Kind<Kind<" + kindName + ", A>, B>";
    String higher1Wildcard = "Kind<Kind<" + kindName + ", A>, ? extends B>";
    String aType = type("A", list.get(0));
    String bType = type("B", list.get(1));
    String typeParams = "<" + aType + ", " + bType + ">";
    String typeOfNameWithParams = typeOfName + typeParams;
    if (packageName != null) {
      writer.println(packageName(packageName));
      writer.println();
    }
    writer.println();
    writer.println(import_(KIND));
    writer.println(import_(FIXER));
    writer.println(import_(generated()));
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(className, typeOfNameWithParams, higher2));
    writer.println();
    narrowK2(writer, className, aType, bType, higher1Wildcard);
    toTypeOf2(writer, className, aType, bType, higher2, typeOfName);
    writer.println(END);
  }

  private void generate3(PrintWriter writer, String packageName, String className,
      String typeOfName, String kindName, List<? extends TypeParameterElement> list) {
    String higher3 = "Kind<Kind<Kind<" + kindName + ", A>, B>, C>";
    String higher1Wildcard = "Kind<Kind<Kind<" + kindName + ", A>, B>, ? extends C>";
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
    writer.println(import_(KIND));
    writer.println(import_(FIXER));
    writer.println(import_(generated()));
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(className, typeOfNameWithParams, higher3));
    writer.println();
    narrowK3(writer, className, aType, bType, cType, higher1Wildcard);
    toTypeOf3(writer, className, aType, bType, cType, higher3, typeOfName);
    writer.println(END);
  }

  private JavaFileObject createFile(String packageName, String className) throws IOException {
    String qualifiedName = packageName != null ? packageName + "." + className : className;
    return processingEnv.getFiler().createSourceFile(qualifiedName);
  }

  private String generated() {
    if (processingEnv.getSourceVersion() == SourceVersion.RELEASE_8) {
      return JAVAX_ANNOTATION_GENERATED;
    }
    return JAVAX_ANNOTATION_PROCESSING_GENERATED;
  }

  private static String privateConstructor(String witnessName) {
    return "  private " + witnessName + "() {}";
  }

  private static void narrowK1(PrintWriter writer, String className, String aType, String hkt) {
    narrowK(writer, "<" + aType + ">", className + "<A>", hkt);
  }

  private static void narrowK2(PrintWriter writer, String className, String aType, String bType, String hkt) {
    narrowK(writer, "<" + aType + ", " + bType + ">", className + "<A, B>", hkt);
  }

  private static void narrowK3(PrintWriter writer, String className, String aType, String bType, String cType, String hkt) {
    narrowK(writer, "<" + aType + ", " + bType + ", " + cType + ">", className + "<A, B, C>", hkt);
  }
  
  private static void narrowK(PrintWriter writer, String types, String returnType, String param) {
    writer.println("  @SuppressWarnings(\"unchecked\")");
    writer.println("  static " + types + " " + returnType + " narrowK(" + param + " hkt) {");
    writer.println("    return (" + returnType + ") hkt;");
    writer.println("  }");
    writer.println();
  }
  
  private static void toTypeOf1(PrintWriter writer, String className, String aType, String hkt, String typeOf) {
    toTypeOf(writer, "<" + aType + ">", className + "<A>", hkt, typeOf, className);
  }
  
  private static void toTypeOf2(PrintWriter writer, String className, String aType, String bType, String hkt, String typeOf) {
    toTypeOf(writer, "<" + aType + ", " + bType + ">", className + "<A, B>", hkt, typeOf, className);
  }
  
  private static void toTypeOf3(PrintWriter writer, String className, String aType, String bType, String cType, String hkt, String typeOf) {
    toTypeOf(writer, "<" + aType + ", " + bType + ", " + cType + ">", className + "<A, B, C>", hkt, typeOf, className);
  }

  private static void toTypeOf(PrintWriter writer, String types, String returnType, String param, String typeOf, String type) {
    writer.println("  static " + types + " Fixer<" + param + ", " + returnType + "> to" + type + "() {");
    writer.println("    return " + typeOf + "::narrowK;");
    writer.println("  }");
    writer.println();
  }

  private static String witnessClass(String kindName) {
    return "public final class " + kindName + " implements Witness {";
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

  private static String import_(String className) {
    return "import " + className + ";";
  }
}
