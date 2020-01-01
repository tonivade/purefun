package com.github.tonivade.purefun;

import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;

import javax.lang.model.element.TypeElement;
import java.util.Optional;

public class InstanceTranslator extends TreeTranslator {

  private final InstanceGenerator generator;
  private final TypeElement element;

  public InstanceTranslator(Context context, TypeElement element) {
    JavacElements elements = JavacElements.instance(context);
    TreeMaker maker = TreeMaker.instance(context);
    this.generator = new InstanceGenerator(elements, maker);
    this.element = element;
  }

  @Override
  public void visitClassDef(JCTree.JCClassDecl clazz) {
    result = isInstanceAnnotation(clazz)
        .filter(x -> clazz.getSimpleName().equals(element.getSimpleName()))
        .flatMap(annotation -> generator.generate(clazz))
        .orElse(clazz);
  }

  private Optional<JCTree.JCAnnotation> isInstanceAnnotation(JCTree.JCClassDecl clazz) {
    return clazz.mods.annotations.stream()
        .filter(annotation -> annotation.annotationType.type.toString().equals(Instance.class.getName()))
        .findFirst();
  }
}
