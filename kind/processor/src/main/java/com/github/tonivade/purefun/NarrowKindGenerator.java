/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

public class NarrowKindGenerator extends TreeTranslator {

  private final ProcessingEnvironment processingEnv;
  private final JavacElements elements;
  private final TreeMaker maker;

  public NarrowKindGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
    this.elements = JavacElements.instance(context);
    this.maker = TreeMaker.instance(context);
  }

  @Override
  public void visitClassDef(JCClassDecl clazz) {
    if (isHigherKindAnnotation(clazz).isPresent()) {
      JCMethodDecl narrowK = narrowKindFor(clazz);
      fixPos(narrowK, clazz.pos);

      processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "method narrowK generated: " + narrowK);

      result = maker.ClassDef(
        clazz.mods,
        clazz.name,
        clazz.typarams,
        clazz.extending,
        clazz.implementing,
        clazz.defs.append(narrowK));
    } else {
      result = clazz;
    }
  }

  private Optional<JCAnnotation> isHigherKindAnnotation(JCClassDecl clazz) {
    return clazz.mods.annotations.stream()
      .filter(annotation -> annotation.annotationType.type.toString().equals(HigherKind.class.getName()))
      .findFirst();
  }

  private JCMethodDecl narrowKindFor(JCClassDecl clazz) {
    return narrowK("T", "µ", clazz.name, "hkt");
  }

  private JCMethodDecl narrowK(String typeParam, String kindName, Name className, String varName) {
    return maker.MethodDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
        elements.getName("narrowK"),
        returnType(className, typeParam),
        typeParam(typeParam),
        List.of(higherKind1(className, kindName, typeParam, varName)),
        List.nil(),
        block(returnValue(typeCast(className, typeParam, varName))),
        null);
  }

  private JCBlock block(JCReturn returnValue) {
    return maker.Block(0, List.of(returnValue));
  }

  private JCReturn returnValue(JCExpression expression) {
    return maker.Return(expression);
  }

  private JCTypeCast typeCast(Name className, String typeParam, String varName) {
    return maker.TypeCast(
        returnType(className, typeParam),
        maker.Ident(elements.getName(varName)));
  }

  private JCVariableDecl higherKind1(Name className, String kindName, String typeParam, String varName) {
    return maker.VarDef(
        maker.Modifiers(Flags.ReceiverParamFlags),
        elements.getName(varName),
        maker.TypeApply(
            maker.Ident(elements.getName("Higher1")),
            List.of(
                maker.Select(maker.Ident(className), elements.getName(kindName)),
                maker.Ident(elements.getName(typeParam)))),
        null);
  }

  private List<JCTypeParameter> typeParam(String typeParam) {
    return List.of(maker.TypeParameter(elements.getName(typeParam), List.nil()));
  }

  private JCTypeApply returnType(Name className, String typeParam) {
    return maker.TypeApply(
        maker.Ident(className),
        List.of(maker.Ident(elements.getName(typeParam))));
  }

  private void fixPos(JCTree newTree, int basePos) {
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
}
