/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeTranslator;

import javax.lang.model.element.TypeElement;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ClassTreeTranslator extends TreeTranslator {

  private final ClassTransformer transformer;
  private final TypeElement element;

  public ClassTreeTranslator(ClassTransformer transformer, TypeElement element) {
    this.transformer = requireNonNull(transformer);
    this.element = requireNonNull(element);
  }

  @Override
  public void visitTopLevel(JCCompilationUnit unit) {
    result = transformer.transform(unit).orElse(unit);

    super.visitTopLevel(unit);
  }

  @Override
  public void visitClassDef(JCClassDecl clazz) {
    result = Optional.of(clazz)
        .filter(this::sameClass)
        .flatMap(transformer::transform)
        .orElse(clazz);
  }

  private boolean sameClass(JCClassDecl clazz) {
    return clazz.getSimpleName().equals(element.getSimpleName());
  }
}
