/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

public abstract class AbstractClassTransformer implements ClassTransformer {

  protected final JavacElements elements;
  protected final TreeMaker maker;

  public AbstractClassTransformer(Context context) {
    this(JavacElements.instance(context), TreeMaker.instance(context));
  }

  private AbstractClassTransformer(JavacElements elements, TreeMaker maker) {
    this.elements = requireNonNull(elements);
    this.maker = requireNonNull(maker);
  }

  protected List<JCTree.JCExpression> params2Ident(JCTree.JCTypeParameter... typeParams) {
    return params2Ident(asList(typeParams));
  }

  protected List<JCTree.JCTypeParameter> params2Type(JCTree.JCTypeParameter... typeParams) {
    return params2Type(asList(typeParams));
  }

  protected List<JCTree.JCExpression> params2Ident(Iterable<JCTree.JCTypeParameter> typeParams) {
    return mapParams(typeParams, this::ident);
  }

  protected List<JCTree.JCTypeParameter> params2Type(Iterable<JCTree.JCTypeParameter> typeParams) {
    return mapParams(typeParams, this::typeParam);
  }

  protected void fixPos(JCTree newTree, int basePos) {
    newTree.accept(new TreeScanner() {
      @Override
      public void scan(JCTree tree) {
        if (tree != null) {
          tree.pos += basePos;
          super.scan(tree);
        }
      }
    });
  }

  private <T extends JCTree> List<T> mapParams(Iterable<JCTree.JCTypeParameter> typeParams, Function<JCTree.JCTypeParameter, T> mapper) {
    return toStream(typeParams).map(mapper)
        .reduce(List.nil(), List::append, List::appendList);
  }

  private JCTree.JCIdent ident(JCTree.JCTypeParameter typeParam) {
    return maker.Ident(typeParam.name);
  }

  private JCTree.JCTypeParameter typeParam(JCTree.JCTypeParameter typeParam) {
    return maker.TypeParameter(typeParam.name, typeParam.bounds, typeParam.annotations);
  }

  private Stream<JCTree.JCTypeParameter> toStream(Iterable<JCTree.JCTypeParameter> typeParams) {
    return StreamSupport.stream(typeParams.spliterator(), false);
  }
}
