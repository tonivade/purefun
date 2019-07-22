/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.Diagnostic;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;

public class NarrowKindGenerator extends TreeTranslator {

  private final ProcessingEnvironment processingEnv;
  private final TreeMaker maker;
  private final JavacElements elements;

  public NarrowKindGenerator(ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.maker = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
    this.elements = JavacElements.instance(((JavacProcessingEnvironment) processingEnv).getContext());
  }

  @Override
  public void visitClassDef(JCClassDecl clazz) {
    JCMethodDecl narrowK = narrowK(clazz);

    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "method narrowK generated: " + narrowK);

    result = maker.ClassDef(
        clazz.mods,
        clazz.name,
        clazz.typarams,
        clazz.extending,
        clazz.implementing,
        clazz.defs.append(narrowK));
  }

  private JCMethodDecl narrowK(JCClassDecl clazz) {
    return maker.MethodDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
        elements.getName("narrowK"),
        maker.TypeApply(
            maker.Ident(clazz.name),
            List.of(maker.Ident(elements.getName("T")))),
        List.of(maker.TypeParameter(elements.getName("T"), List.nil())),
        List.of(
            maker.VarDef(
                maker.Modifiers(Flags.ReceiverParamFlags),
                elements.getName("hkt"),
                maker.TypeApply(
                    maker.Ident(elements.getName("Higher1")),
                    List.of(
                        maker.Select(maker.Ident(clazz.name), elements.getName("µ")),
                        maker.Ident(elements.getName("T")))),
                null)),
        List.nil(),
        maker.Block(0,
            List.of(
                maker.Return(
                    maker.TypeCast(
                        maker.TypeApply(
                            maker.Ident(clazz.name),
                            List.of(maker.Ident(elements.getName("T")))),
                        maker.Ident(elements.getName("hkt")))))),
        null);
  }
}
