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
    writer.println("package " + packageName + ";");
    writer.println();
    writer.println("import com.github.tonivade.purefun.Kind;");
    writer.println("import com.github.tonivade.purefun.Higher1;");
    writer.println("import javax.annotation.Generated;");
    writer.println();
    writer.println("@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")");
    writer.println("public final class " + kindName + " implements Kind {");
    writer.println();
    String aType = type("A", type1);
    writer.println("public static <" + aType + "> " + className + "<A> narrowK(Higher1<" + kindName + ", A> hkt) {");
    writer.println("return (" + className + "<A>) hkt;");
    writer.println("}");
    writer.println();
    writer.println("}");
  }

  private void generate2(String packageName, String className, String kindName, PrintWriter writer, 
      TypeParameterElement type1, TypeParameterElement type2) {
    writer.println("package " + packageName + ";");
    writer.println();
    writer.println("import com.github.tonivade.purefun.Kind;");
    writer.println("import com.github.tonivade.purefun.Higher1;");
    writer.println("import com.github.tonivade.purefun.Higher2;");
    writer.println("import javax.annotation.Generated;");
    writer.println();
    writer.println("@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")");
    writer.println("public final class " + kindName + " implements Kind {");
    writer.println();
    String aType = type("A", type1);
    String bType = type("B", type2);
    writer.println("public static <" + aType + ", " + bType + "> " + className + "<A, B> narrowK(Higher1<Higher1<" + kindName + ", A>, B> hkt) {");
    writer.println("return (" + className + "<A, B>) hkt;");
    writer.println("}");
    writer.println();
    writer.println("public static <" + aType + ", " + bType + "> " + className + "<A, B> narrowK(Higher2<" + kindName + ", A, B> hkt) {");
    writer.println("return (" + className + "<A, B>) hkt;");
    writer.println("}");
    writer.println();
    writer.println("}");
  }

  private void generate3(String packageName, String className, String kindName, PrintWriter writer, 
      TypeParameterElement type1, TypeParameterElement type2, TypeParameterElement type3) {
    writer.println("package " + packageName + ";");
    writer.println();
    writer.println("import com.github.tonivade.purefun.Kind;");
    writer.println("import com.github.tonivade.purefun.Higher1;");
    writer.println("import com.github.tonivade.purefun.Higher2;");
    writer.println("import com.github.tonivade.purefun.Higher3;");
    writer.println("import javax.annotation.Generated;");
    writer.println();
    writer.println("@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")");
    writer.println("public final class " + kindName + " implements Kind {");
    writer.println();
    String aType = type("A", type1);
    String bType = type("B", type2);
    String cType = type("C", type3);
    writer.println("public static <" + aType + ", " + bType + ", " + cType + "> " + className + "<A, B, C> narrowK(Higher1<Higher1<Higher1<" + kindName + ", A>, B>, C> hkt) {");
    writer.println("return (" + className + "<A, B, C>) hkt;");
    writer.println("}");
    writer.println();
    writer.println("public static <" + aType + ", " + bType + ", " + cType + "> " + className + "<A, B, C> narrowK(Higher2<Higher1<" + kindName + ", A>, B, C> hkt) {");
    writer.println("return (" + className + "<A, B, C>) hkt;");
    writer.println("}");
    writer.println();
    writer.println("public static <" + aType + ", " + bType + ", " + cType + "> " + className + "<A, B, C> narrowK(Higher3<" + kindName + ", A, B, C> hkt) {");
    writer.println("return (" + className + "<A, B, C>) hkt;");
    writer.println("}");
    writer.println();
    writer.println("}");
  }

  private String type(String name, TypeParameterElement type1) {
    return type1.getBounds().size() > 0 && !type1.getBounds().get(0).toString().equals(Object.class.getName()) ? 
        name + " extends " + type1.getBounds().stream().map(Object::toString).collect(joining(",")) : name;
  }
}
