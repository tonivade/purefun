package com.github.tonivade.purefun;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

@SupportedAnnotationTypes("com.github.tonivade.purefun.Instance")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class InstanceProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return check() && apply(annotations, roundEnv);
  }

  private boolean apply(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "@Instance found at " + element);
        try {
          generate((TypeElement) element);
        } catch (RuntimeException e) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "error generating code", element);
        }
      }
    }
    return true;
  }

  private boolean check() {
    if (!verifyJavac()) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "not supported");
      return false;
    }
    return true;
  }

  private boolean verifyJavac() {
    return processingEnv instanceof JavacProcessingEnvironment;
  }

  private void generate(TypeElement element) {
    Trees trees = Trees.instance(processingEnv);
    JCTree tree = (JCTree) trees.getPath(element).getCompilationUnit();
    tree.accept(new InstanceTranslator(((JavacProcessingEnvironment) processingEnv).getContext(), element));
  }
}
