/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.github.tonivade.purefun.HigherKind")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HigherKindProcessor extends AbstractJavacProcessor {

  @Override
  protected ClassTreeTranslator buildVisitor(JavacProcessingEnvironment javacProcessingEnvironment, TypeElement element) {
    return new ClassTreeTranslator(new HigherKindTransformer(javacProcessingEnvironment.getContext()), element);
  }
}
