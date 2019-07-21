package com.github.tonivade.purefun;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.sun.source.tree.Tree;
import com.sun.source.util.Trees;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;

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
        
        clazz.defs.forEach(tree -> {
          if (tree.getKind() == Tree.Kind.METHOD) {
            JCMethodDecl method = (JCMethodDecl) tree;
            if (method.getName().contentEquals("narrowK")) {
              processingEnv.getMessager().printMessage(Kind.NOTE, "narrowK: " + method);
            }
          }
        });
        
        result = maker.ClassDef(
            clazz.mods, 
            clazz.name, 
            clazz.typarams, 
            clazz.extending, 
            clazz.implementing,
            clazz.defs);
      }
    });

    List<? extends TypeParameterElement> typeParameters = element.getTypeParameters();
    ClassName kind = ClassName.get(element);
    
    TypeSpec typeSpec = null;
    if (typeParameters.size() == 1) {
      typeSpec = buildTypeSpec(element, generateNarrowK1(element, typeParameters));
    } else if (typeParameters.size() == 2) {
      typeSpec = buildTypeSpec(element, generateNarrowK2(element, typeParameters));
    } else if (typeParameters.size() == 3) {
      typeSpec = buildTypeSpec(element, generateNarrowK3(element, typeParameters));
    } else {
      throw new UnsupportedOperationException();
    }
      
    saveFile(kind, typeSpec);
  }

  private void saveFile(ClassName kind, TypeSpec typeSpec) {
    try {
      JavaFile file = JavaFile.builder(kind.packageName(), typeSpec).build();
      file.writeTo(processingEnv.getFiler());
    } catch (IOException e) {
      processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "error writing file " + kind);
    }
  }

  private TypeSpec buildTypeSpec(TypeElement element, MethodSpec methodSpec) {
    TypeSpec.Builder builder = null;
    if (element.getKind().isClass()) {
      builder = TypeSpec.classBuilder(ClassName.get(element).simpleName() + "Kind");
    } else if (element.getKind().isInterface()) {
      builder = TypeSpec.interfaceBuilder(ClassName.get(element).simpleName() + "Kind");
    } else {
      throw new UnsupportedOperationException();
    }
    return builder.addMethod(methodSpec).addModifiers(Modifier.PUBLIC).build();
  }

  private MethodSpec generateNarrowK1(TypeElement element, List<? extends TypeParameterElement> typeParameters) {
    TypeVariableName typeVariable1 = TypeVariableName.get(typeParameters.get(0));
    ClassName kind1 = ClassName.get(element);
    ClassName mu = ClassName.get(kind1.packageName(), kind1.simpleName() + ".µ");
    ClassName higher1 = ClassName.get(Higher1.class);
    ParameterizedTypeName higher1OfT = ParameterizedTypeName.get(higher1, mu, typeVariable1);
    ParameterizedTypeName kind1OfT = ParameterizedTypeName.get(kind1, typeVariable1);
    return MethodSpec.methodBuilder("narrowK")
      .returns(kind1OfT)
      .addTypeVariable(typeVariable1)
      .addParameter(higher1OfT, "hkt")
      .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
      .addCode("return ($T) hkt;", kind1OfT)
      .build();
  }

  private MethodSpec generateNarrowK2(TypeElement element, List<? extends TypeParameterElement> typeParameters) {
    TypeVariableName typeVariable1 = TypeVariableName.get(typeParameters.get(0));
    TypeVariableName typeVariable2 = TypeVariableName.get(typeParameters.get(1));
    ClassName kind2 = ClassName.get(element);
    ClassName mu = ClassName.get(kind2.packageName(), kind2.simpleName() + ".µ");
    ClassName higher2 = ClassName.get(Higher2.class);
    ParameterizedTypeName higher2OfT = ParameterizedTypeName.get(higher2, mu, typeVariable1, typeVariable2);
    ParameterizedTypeName kind2OfT = ParameterizedTypeName.get(kind2, typeVariable1, typeVariable2);
    return MethodSpec.methodBuilder("narrowK")
      .returns(kind2OfT)
      .addTypeVariable(typeVariable1)
      .addTypeVariable(typeVariable2)
      .addParameter(higher2OfT, "hkt")
      .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
      .addCode("return ($T) hkt;", kind2OfT)
      .build();
  }

  private MethodSpec generateNarrowK3(TypeElement element, List<? extends TypeParameterElement> typeParameters) {
    TypeVariableName typeVariable1 = TypeVariableName.get(typeParameters.get(0));
    TypeVariableName typeVariable2 = TypeVariableName.get(typeParameters.get(1));
    TypeVariableName typeVariable3 = TypeVariableName.get(typeParameters.get(2));
    ClassName kind3 = ClassName.get(element);
    ClassName mu = ClassName.get(kind3.packageName(), kind3.simpleName() + ".µ");
    ClassName higher3 = ClassName.get(Higher2.class);
    ParameterizedTypeName higher3OfT = ParameterizedTypeName.get(higher3, mu, typeVariable1, typeVariable2, typeVariable3);
    ParameterizedTypeName kind3OfT = ParameterizedTypeName.get(kind3, typeVariable1, typeVariable2, typeVariable3);
    return MethodSpec.methodBuilder("narrowK")
      .returns(kind3OfT)
      .addTypeVariable(typeVariable1)
      .addTypeVariable(typeVariable2)
      .addTypeVariable(typeVariable3)
      .addParameter(higher3OfT, "hkt")
      .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
      .addCode("return ($T) hkt;", kind3OfT)
      .build();
  }
}
