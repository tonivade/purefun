/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.TreeTranslator;

import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.TypeElement;

@SupportedAnnotationTypes("com.github.tonivade.purefun.Instance")
public class InstanceProcessor extends AbstractJavacProcessor {

  @Override
  protected TreeTranslator buildVisitor(JavacProcessingEnvironment processingEnv, TypeElement element) {
    return new ClassTreeTranslator(new InstanceTransformer(processingEnv.getContext()), element);
  }
}
