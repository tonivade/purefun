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

  private static final String JAVA_8 = "1.8";

  private static final String GENERATED = "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")";

  private static final String IMPORT_JAVAX_ANNOTATION_GENERATED = "import javax.annotation.Generated;";
  private static final String IMPORT_JAVAX_ANNOTATION_PROCESSING_GENERATED = "import javax.annotation.processing.Generated;";

  private static final String IMPORT_KIND = "import com.github.tonivade.purefun.Kind;";
  private static final String IMPORT_WITNESS = "import com.github.tonivade.purefun.Witness;";
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
    String packageName =  qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    String className = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    String witnessName = className + "_";
    String typeOfName = className + "Of";
    writeWitness(packageName, witnessName);
    writeTypeOf(packageName, className, typeOfName, witnessName, element.getTypeParameters());
  }

  private void writeWitness(String packageName, String witnessName) throws IOException {
    JavaFileObject witnessFile = processingEnv.getFiler().createSourceFile(packageName + "." + witnessName);
    try (PrintWriter writer = new PrintWriter(witnessFile.openWriter())) {
      witness(writer, packageName, witnessName);
    }
  }

  private void writeTypeOf(String packageName, String className, String typeOfName, String witnessName,
    List<? extends TypeParameterElement> types) throws IOException {
    JavaFileObject typeOfFile = processingEnv.getFiler().createSourceFile(packageName + "." + typeOfName);
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
    writer.println(packageName(packageName));
    writer.println();
    writer.println(IMPORT_WITNESS);
    writer.println(generatedImport());
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
    String aType = type("A", list.get(0));
    String typeOfNameWithParams = typeOfName + "<" + aType + ">";
    writer.println(packageName(packageName));
    writer.println();
    writer.println(IMPORT_KIND);
    writer.println(generatedImport());
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(typeOfNameWithParams, higher1));
    writer.println();
    narrowK1(writer, className, aType, higher1);
    writer.println(END);
  }

  private void generate2(PrintWriter writer, String packageName, String className,
      String typeOfName, String kindName, List<? extends TypeParameterElement> list) {
    String higher1 = "Kind<Kind<" + kindName + ", A>, B>";
    String aType = type("A", list.get(0));
    String bType = type("B", list.get(1));
    String typeOfNameWithParams = typeOfName + "<" + aType + ", " + bType + ">";
    writer.println(packageName(packageName));
    writer.println();
    writer.println(IMPORT_KIND);
    writer.println(generatedImport());
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(typeOfNameWithParams, higher1));
    writer.println();
    narrowK2(writer, className, aType, bType, higher1);
    writer.println(END);
  }

  private void generate3(PrintWriter writer, String packageName, String className,
      String typeOfName, String kindName, List<? extends TypeParameterElement> list) {
    String higher1 = "Kind<Kind<Kind<" + kindName + ", A>, B>, C>";
    String aType = type("A", list.get(0));
    String bType = type("B", list.get(1));
    String cType = type("C", list.get(2));
    String typeOfNameWithParams = typeOfName + "<" + aType + ", " + bType + ", " + cType + ">";
    writer.println(packageName(packageName));
    writer.println();
    writer.println(IMPORT_KIND);
    writer.println(generatedImport());
    writer.println();
    writer.println(GENERATED);
    writer.println(typeOfClass(typeOfNameWithParams, higher1));
    writer.println();
    narrowK3(writer, className, aType, bType, cType, higher1);
    writer.println(END);
  }

  private String privateConstructor(String witnessName) {
    return "  private " + witnessName + "() {}";
  }

  private void narrowK1(PrintWriter writer, String className, String aType, String hkt) {
    narrowK(writer, "<" + aType + ">", className + "<A>", hkt);
  }

  private void narrowK2(PrintWriter writer, String className, String aType, String bType, String hkt) {
    narrowK(writer, "<" + aType + ", " + bType + ">", className + "<A, B>", hkt);
  }

  private void narrowK3(PrintWriter writer, String className, String aType, String bType, String cType, String hkt) {
    narrowK(writer, "<" + aType + ", " + bType + ", " + cType + ">", className + "<A, B, C>", hkt);
  }

  private void narrowK(PrintWriter writer, String types, String returnType, String param) {
    writer.println("  static " + types + " " + returnType + " narrowK(" + param + " hkt) {");
    writer.println("    return (" + returnType + ") hkt;");
    writer.println("  }");
    writer.println();
  }

  private String witnessClass(String kindName) {
    return "public final class " + kindName + " implements Witness {";
  }

  private String typeOfClass(String typeOfName, String type) {
    return "public interface " + typeOfName + " extends " + type + " {";
  }

  private String generatedImport() {
    String version = System.getProperty("java.specification.version");
    if (version.equals(JAVA_8)) {
      return IMPORT_JAVAX_ANNOTATION_GENERATED;
    } else {
      return IMPORT_JAVAX_ANNOTATION_PROCESSING_GENERATED;
    }
  }

  private String type(String name, TypeParameterElement type1) {
    String bounds = bounds(type1);
    return !bounds.isEmpty() ? name + " extends " + bounds : name;
  }

  private String bounds(TypeParameterElement type1) {
    return type1.getBounds().stream()
      .map(Object::toString)
      .filter(type -> !type.equals(Object.class.getName()))
      .collect(joining(","));
  }

  private String packageName(String packageName) {
    return "package " + packageName + ";";
  }
}
