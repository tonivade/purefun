/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeTranslator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.Set;

import static java.lang.String.format;

public abstract class AbstractJavacProcessor extends AbstractProcessor {

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latestSupported();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    return check() && apply(annotations, roundEnv);
  }

  protected abstract TreeTranslator buildVisitor(
      JavacProcessingEnvironment javacProcessingEnvironment, TypeElement element);

  private boolean apply(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
            format("@%s found at %s", annotation.getSimpleName(), element.getSimpleName()));
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
    JCTree unit = (JCTree) trees.getPath(element).getCompilationUnit();
    unit.accept(buildVisitor((JavacProcessingEnvironment) processingEnv, element));
  }
}
