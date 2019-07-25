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
import com.sun.tools.javac.tree.JCTree.JCIdent;
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
    Optional<JCAnnotation> higherKindAnnotation = isHigherKindAnnotation(clazz);
    if (higherKindAnnotation.isPresent()) {
      if (clazz.typarams.length() == 1) {
        result = generateHigher1Kind(clazz, higherKindAnnotation.get());
      }
    } else {
      result = clazz;
    }
  }

  private JCClassDecl generateHigher1Kind(JCClassDecl clazz, JCAnnotation annotation) {
    Name typeParam = clazz.typarams.head.name;
    Name kindName = elements.getName("µ");
    Name varName = elements.getName("hkt");

    JCMethodDecl narrowK = narrowKindFor(clazz, typeParam, kindName, varName);
    JCClassDecl witness = kindWitness("µ");
    JCTypeApply higher1 = higher1Kind(clazz.name, kindName, typeParam);
    fixPos(witness, clazz.pos);
    fixPos(narrowK, witness.pos);

    printNote("witness generated: " + witness);
    printNote("method narrowK generated: " + narrowK);
    printNote("implements generated: " + higher1);

    return maker.ClassDef(
      clazz.mods,
      clazz.name,
      clazz.typarams,
      clazz.extending,
      clazz.implementing.append(higher1),
      clazz.defs.append(witness).append(narrowK));
  }

  private void printNote(String note) {
    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, note);
  }

  private Optional<JCAnnotation> isHigherKindAnnotation(JCClassDecl clazz) {
    return clazz.mods.annotations.stream()
      .filter(annotation -> annotation.annotationType.type.toString().equals(HigherKind.class.getName()))
      .findFirst();
  }
  
  private JCClassDecl kindWitness(String name) {
    return maker.ClassDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC), 
        elements.getName(name), 
        List.nil(), 
        null, 
        List.of(implementsKind()), 
        List.nil());
  }

  private JCIdent implementsKind() {
    return maker.Ident(elements.getName("Kind"));
  }

  private JCMethodDecl narrowKindFor(JCClassDecl clazz, Name typeParam, Name kindName, Name varName) {
    return narrowK(typeParam, kindName, clazz.name, varName);
  }

  private JCMethodDecl narrowK(Name typeParam, Name kindName, Name className, Name varName) {
    return maker.MethodDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
        elements.getName("narrowK"),
        returnType(className, typeParam),
        List.of(typeParam(typeParam)),
        List.of(variable(varName, higher1Kind(className, kindName, typeParam))),
        List.nil(),
        block(returns(typeCast(className, typeParam, varName))),
        null);
  }

  private JCBlock block(JCReturn returnValue) {
    return maker.Block(0, List.of(returnValue));
  }

  private JCReturn returns(JCExpression expression) {
    return maker.Return(expression);
  }

  private JCTypeCast typeCast(Name className, Name typeParam, Name varName) {
    return maker.TypeCast(
        returnType(className, typeParam),
        maker.Ident(varName));
  }

  private JCVariableDecl variable(Name varName, JCExpression typeDef) {
    return maker.VarDef(
        maker.Modifiers(Flags.ReceiverParamFlags),
        varName,
        typeDef,
        null);
  }

  private JCTypeApply higher1Kind(Name className, Name kindName, Name typeParam) {
    return maker.TypeApply(
        maker.Ident(elements.getName("Higher1")),
        List.of(
            maker.Select(maker.Ident(className), kindName),
            maker.Ident(typeParam)));
  }

  private JCTypeParameter typeParam(Name typeParam) {
    return maker.TypeParameter(typeParam, List.nil());
  }

  private JCTypeApply returnType(Name className, Name typeParam) {
    return maker.TypeApply(
        maker.Ident(className),
        List.of(maker.Ident(typeParam)));
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
