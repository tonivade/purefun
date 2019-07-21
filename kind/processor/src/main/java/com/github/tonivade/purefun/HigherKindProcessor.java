package com.github.tonivade.purefun;

import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("com.github.tonivade.purefun.HigherKind")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HigherKindProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found at " + element);
      }
    }
    return true;
  }
}
