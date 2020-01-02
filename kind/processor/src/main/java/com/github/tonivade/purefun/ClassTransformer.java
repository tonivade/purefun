/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.sun.tools.javac.tree.JCTree;

import java.util.Optional;

public interface ClassTransformer {

  default Optional<JCTree.JCCompilationUnit> transform(JCTree.JCCompilationUnit clazz) {
    return Optional.empty();
  }

  default Optional<JCTree.JCClassDecl> transform(JCTree.JCClassDecl clazz) {
    return Optional.empty();
  }
}
