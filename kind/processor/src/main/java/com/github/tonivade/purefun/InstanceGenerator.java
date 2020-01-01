package com.github.tonivade.purefun;

import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;

import java.util.Optional;
import java.util.function.Function;

public class InstanceGenerator {

  public static final String METHOD_NAME = "instance";
  public static final String VARIABLE_NAME = "_INSTANCE";

  private final JavacElements elements;
  private final TreeMaker maker;

  public InstanceGenerator(JavacElements elements, TreeMaker maker) {
    this.elements = elements;
    this.maker = maker;
  }

  public Optional<JCTree> generate(JCTree.JCClassDecl clazz) {
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
        maker.NewClass(
            null,
            List.nil(),
            maker.Ident(className),
            List.nil(),
            maker.ClassDef(
                maker.Modifiers(0),
                elements.getName(""),
                List.nil(),
                null,
                List.nil(),
                List.nil()
            )
        ));
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

  private List<JCTree.JCExpression> params2Ident(List<JCTree.JCTypeParameter> typeParams) {
    return mapParams(this::ident, typeParams);
  }

  private List<JCTree.JCTypeParameter> params2Type(List<JCTree.JCTypeParameter> typeParams) {
    return mapParams(this::typeParam, typeParams);
  }

  private <T extends JCTree> List<T> mapParams(Function<JCTree.JCTypeParameter, T> mapper, List<JCTree.JCTypeParameter> typeParams) {
    return typeParams.stream().map(mapper)
        .reduce(List.nil(), List::append, List::appendList);
  }

  private JCTree.JCIdent ident(JCTree.JCTypeParameter typeParam) {
    return maker.Ident(typeParam.name);
  }

  private JCTree.JCTypeParameter typeParam(JCTree.JCTypeParameter typeParam) {
    return maker.TypeParameter(typeParam.name, typeParam.bounds, typeParam.annotations);
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
