/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.util.Optional;

public class InstanceTransformer extends AbstractClassTransformer {

  public static final String METHOD_NAME = "instance";
  public static final String VARIABLE_NAME = "_INSTANCE";

  public InstanceTransformer(Context context) {
    super(context);
  }

  @Override
  public Optional<JCTree.JCClassDecl> transform(JCTree.JCClassDecl clazz) {
    JCTree.JCVariableDecl instance = createInstance(clazz.name);
    JCTree.JCMethodDecl method = instanceMethod(clazz.name, clazz.typarams);
    fixPos(instance, clazz.pos);
    fixPos(method, clazz.pos + instance.pos);
    clazz.defs = clazz.defs.append(instance).append(method);
    return Optional.of(clazz);
  }

  private JCTree.JCVariableDecl createInstance(Name className) {
    return maker.VarDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC | Flags.FINAL),
        elements.getName(VARIABLE_NAME),
        maker.Ident(className),
        newInstance(className));
  }

  private JCTree.JCNewClass newInstance(Name className) {
    return maker.NewClass(
        null,
        List.nil(),
        maker.Ident(className),
        List.nil(),
        callEmptyConstructor()
    );
  }

  private JCTree.JCClassDecl callEmptyConstructor() {
    return maker.ClassDef(
        maker.Modifiers(0),
        elements.getName(""),
        List.nil(),
        null,
        List.nil(),
        List.nil()
    );
  }

  private JCTree.JCMethodDecl instanceMethod(Name className, List<JCTree.JCTypeParameter> typeParams) {
    return maker.MethodDef(
        maker.Modifiers(Flags.PUBLIC | Flags.STATIC),
        elements.getName(METHOD_NAME),
        returnType(className, typeParams),
        params2Type(typeParams),
        List.nil(),
        List.nil(),
        block(returns(typeCast(className, elements.getName(VARIABLE_NAME), typeParams))),
        null);
  }

  private JCTree.JCTypeApply returnType(Name className, List<JCTree.JCTypeParameter> typeParams) {
    return maker.TypeApply(
        maker.Ident(className),
        params2Ident(typeParams));
  }

  private JCTree.JCBlock block(JCTree.JCReturn returnValue) {
    return maker.Block(0, List.of(returnValue));
  }

  private JCTree.JCReturn returns(JCTree.JCExpression expression) {
    return maker.Return(expression);
  }

  private JCTree.JCTypeCast typeCast(Name className, Name varName, List<JCTree.JCTypeParameter> typeParams) {
    return maker.TypeCast(
        returnType(className, typeParams),
        maker.Ident(varName));
  }
}
