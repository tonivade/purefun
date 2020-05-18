package com.github.tonivade.purefun;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import java.io.IOException;
import java.io.PrintWriter;
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
  private static final String IMPORT_HIGHER1 = "import com.github.tonivade.purefun.Higher1;";
  private static final String IMPORT_HIGHER2 = "import com.github.tonivade.purefun.Higher2;";
  private static final String IMPORT_HIGHER3 = "import com.github.tonivade.purefun.Higher3;";

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
    int params = element.getTypeParameters().size();
    String qualifiedName = element.getQualifiedName().toString();
    String packageName =  qualifiedName.substring(0, qualifiedName.lastIndexOf('.'));
    String className = qualifiedName.substring(qualifiedName.lastIndexOf('.') + 1);
    String kindName = className + "_";
    JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(packageName + "." + kindName);

    try (PrintWriter writer = new PrintWriter(sourceFile.openWriter())) {
      if (params == 1) {
        generate1(packageName, className, kindName, writer, element.getTypeParameters().get(0));
      } else if (params == 2) {
        generate2(packageName, className, kindName, writer,
            element.getTypeParameters().get(0), element.getTypeParameters().get(1));
      } else if (params == 3) {
        generate3(packageName, className, kindName, writer,
            element.getTypeParameters().get(0), element.getTypeParameters().get(1), element.getTypeParameters().get(2));
      } else {
        throw new UnsupportedOperationException("too many params: " + qualifiedName);
      }
    }
  }

  private void generate1(String packageName, String className, String kindName, PrintWriter writer,
      TypeParameterElement type1) {
    writer.println(packageName(packageName));
    writer.println();
    writer.println(IMPORT_KIND);
    writer.println(IMPORT_HIGHER1);
    writer.println(generatedImport());
    writer.println();
    writer.println(GENERATED);
    writer.println(className(kindName));
    writer.println();
    String aType = type("A", type1);
    String higher1 = "Higher1<" + kindName + ", A>";
    narrowK1(writer, className, aType, higher1);
    writer.println("}");
  }

  private void generate2(String packageName, String className, String kindName, PrintWriter writer,
      TypeParameterElement type1, TypeParameterElement type2) {
    writer.println(packageName(packageName));
    writer.println();
    writer.println(IMPORT_KIND);
    writer.println(IMPORT_HIGHER1);
    writer.println(IMPORT_HIGHER2);
    writer.println(generatedImport());
    writer.println();
    writer.println(GENERATED);
    writer.println(className(kindName));
    writer.println();
    String aType = type("A", type1);
    String bType = type("B", type2);
    narrowK2(writer, className, aType, bType, "Higher1<Higher1<" + kindName + ", A>, B>");
    narrowK2(writer, className, aType, bType, "Higher2<" + kindName + ", A, B>");
    writer.println("}");
  }

  private void generate3(String packageName, String className, String kindName, PrintWriter writer,
      TypeParameterElement type1, TypeParameterElement type2, TypeParameterElement type3) {
    writer.println(packageName(packageName));
    writer.println();
    writer.println(IMPORT_KIND);
    writer.println(IMPORT_HIGHER1);
    writer.println(IMPORT_HIGHER2);
    writer.println(IMPORT_HIGHER3);
    writer.println(generatedImport());
    writer.println();
    writer.println(GENERATED);
    writer.println(className(kindName));
    writer.println();
    String aType = type("A", type1);
    String bType = type("B", type2);
    String cType = type("C", type3);
    narrowK3(writer, className, aType, bType, cType, "Higher1<Higher1<Higher1<" + kindName + ", A>, B>, C>");
    narrowK3(writer, className, aType, bType, cType, "Higher2<Higher1<" + kindName + ", A>, B, C>");
    narrowK3(writer, className, aType, bType, cType, "Higher3<" + kindName + ", A, B, C>");
    writer.println("}");
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
    writer.println("  public static " + types + " " + returnType + " narrowK(" + param + " hkt) {");
    writer.println("    return (" + returnType + ") hkt;");
    writer.println("  }");
    writer.println();
  }

  private String className(String kindName) {
    return "public final class " + kindName + " implements Kind {";
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
    return !type1.getBounds().isEmpty() && !type1.getBounds().get(0).toString().equals(Object.class.getName()) ?
        name + " extends " + type1.getBounds().stream().map(Object::toString).collect(joining(",")) : name;
  }

  private String packageName(String packageName) {
    return "package " + packageName + ";";
  }
}
