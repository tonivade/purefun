/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
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

import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;

@SupportedAnnotationTypes("com.github.tonivade.purefun.HigherKind")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HigherKindProcessor extends AbstractProcessor {

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "@HigherKind found at " + element);
        try {
          generate((TypeElement) element);
        } catch (RuntimeException e) {
          processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "error generating code", element);
        }
      }
    }
    return true;
  }

  private void generate(TypeElement element) {
    Trees trees = Trees.instance(processingEnv);
    JCTree tree = (JCTree) trees.getPath(element).getCompilationUnit();
    tree.accept(new HigherKindTranslator(((JavacProcessingEnvironment) processingEnv).getContext()));
  }
}
