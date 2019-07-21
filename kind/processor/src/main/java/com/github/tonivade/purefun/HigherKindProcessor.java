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
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

@SupportedAnnotationTypes("com.github.tonivade.purefun.HigherKind")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HigherKindProcessor extends AbstractProcessor {
  
  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (TypeElement annotation : annotations) {
      for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "found at " + element);
        generateNarrowK((TypeElement) element);
      }
    }
    return true;
  }

  private void generateNarrowK(TypeElement element) {
    Trees trees = Trees.instance(processingEnv);
    JCTree tree = (JCTree) trees.getTree(element);
    tree.accept(new TreeTranslator() {
      @Override
      public void visitClassDef(JCClassDecl clazz) {
        // TODO add narrowK method
        TreeMaker maker = TreeMaker.instance(((JavacProcessingEnvironment) processingEnv).getContext());
        JavacElements elements = JavacElements.instance(((JavacProcessingEnvironment) processingEnv).getContext());
        
        JCMethodDecl narrowK = narrowK(clazz, maker, elements);

        result = maker.ClassDef(
            clazz.mods, 
            clazz.name, 
            clazz.typarams, 
            clazz.extending, 
            clazz.implementing,
            clazz.defs.append(narrowK));
      }
    });
  }

  private JCMethodDecl narrowK(JCClassDecl clazz, TreeMaker maker, JavacElements elements) {
    JCModifiers mods = maker.Modifiers(Flags.PUBLIC | Flags.STATIC);
    JCBlock block = 
        maker.Block(0, 
            List.of(
                maker.Return(
                    maker.TypeCast(
                        maker.TypeApply(
                            maker.Ident(clazz.name), 
                            List.of(maker.Ident(elements.getName("T")))), 
                        maker.Ident(elements.getName("hkt"))))));
    Name name = elements.getName("narrowK");
    JCExpression restype = 
        maker.TypeApply(
            maker.Ident(clazz.name), 
            List.of(maker.Ident(elements.getName("T"))));
    List<JCTypeParameter> typarams = 
        List.of(maker.TypeParameter(elements.getName("T"), List.nil()));
    List<JCVariableDecl> params = 
        List.of(
            maker.VarDef(
                maker.Modifiers(Flags.ReceiverParamFlags), 
                elements.getName("hkt"), 
                maker.TypeApply(
                    maker.Ident(elements.getName("Higher1")), 
                    List.of(
                        maker.Select(maker.Ident(clazz.name), elements.getName("µ")),
                        maker.Ident(elements.getName("T")))), 
                null));
    List<JCExpression> thrown = List.nil();
    JCExpression defaultValue = null;

    return maker.MethodDef(
        mods, 
        name, 
        restype, 
        typarams, 
        params, 
        thrown, 
        block, 
        defaultValue);
  }
}
