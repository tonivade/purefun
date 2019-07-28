/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.Optional;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;

public class HigherKindTranslator extends TreeTranslator {

  private final HigherKindGenerator generator;

  public HigherKindTranslator(Context context) {
    JavacElements elements = JavacElements.instance(context);
    TreeMaker maker = TreeMaker.instance(context);
    this.generator = new HigherKindGenerator(elements, maker);
  }

  @Override
  public void visitTopLevel(JCCompilationUnit unit) {
    result = generator.imports(unit).orElse(unit);

    super.visitTopLevel(unit);
  }

  @Override
  public void visitClassDef(JCClassDecl clazz) {
    result = isHigherKindAnnotation(clazz)
        .flatMap(annotation -> generator.generate(clazz, annotation))
        .orElse(clazz);
  }

  private Optional<JCAnnotation> isHigherKindAnnotation(JCClassDecl clazz) {
    return clazz.mods.annotations.stream()
      .filter(annotation -> annotation.annotationType.type.toString().equals(HigherKind.class.getName()))
      .findFirst();
  }
}
