/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

public class HigherKindProcessorTest {

  private final JavaFileObject witness = forSourceLines("test.Foo_",
        "package test;",

        "import com.github.tonivade.purefun.Witness;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public final class Foo_ implements Witness {",
        "private Foo_() {}",
        "}");

  @Test
  public void compilesKind1() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T> implements FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Fixer;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A> extends Kind<Foo_, A> {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A> Foo<A> narrowK(Kind<Foo_, ? extends A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "static <A> Fixer<Kind<Foo_, A>, Foo<A>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated, witness);
  }

  @Test
  public void compilesKind1Sealed() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind(sealed = true)",
        "public interface Foo<T> extends FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Fixer;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A> extends Kind<Foo_, A> {",
        
        "SealedFoo<A> youShallNotPass();",

        "@SuppressWarnings(\"unchecked\")",
        "static <A> Foo<A> narrowK(Kind<Foo_, ? extends A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "static <A> Fixer<Kind<Foo_, A>, Foo<A>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}",
        
        "interface SealedFoo<A> extends Foo<A> {",
        "default SealedFoo<A> youShallNotPass() {",
        "throw new UnsupportedOperationException();",
        "}",
        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated, witness);
  }

  @Test
  public void compilesKind1NoPackage() {
    JavaFileObject file = forSourceLines("test.Foo",
        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T> implements FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Fixer;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A> extends Kind<Foo_, A> {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A> Foo<A> narrowK(Kind<Foo_, ? extends A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "static <A> Fixer<Kind<Foo_, A>, Foo<A>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    JavaFileObject generatedWitness = forSourceLines("Foo_",
        "import com.github.tonivade.purefun.Witness;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public final class Foo_ implements Witness {",
        "private Foo_() {}",
        "}");
    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated, generatedWitness);
  }

  @Test
  public void compilesKind1Bounds() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T extends String> implements FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Fixer;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A extends java.lang.String> extends Kind<Foo_, A> {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A extends java.lang.String> Foo<A> narrowK(Kind<Foo_, ? extends A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "static <A extends java.lang.String> Fixer<Kind<Foo_, A>, Foo<A>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated, witness);
  }

  @Test
  public void compilesError() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo {",
        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .failsToCompile();
  }

  @Test
  public void compilesKind2() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<A, B> implements FooOf<A, B> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Fixer;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A, B> extends Kind<Kind<Foo_, A>, B> {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A, B> Foo<A, B> narrowK(Kind<Kind<Foo_, A>, ? extends B> hkt) {",
        "return (Foo<A, B>) hkt;",
        "}",
        
        "static <A, B> Fixer<Kind<Kind<Foo_, A>, B>, Foo<A, B>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated, witness);
  }

  @Test
  public void compilesKind3() {
    JavaFileObject file = forSourceLines("Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<A, B, C> implements FooOf<A, B, C> {",
        "}");

    JavaFileObject generated = forSourceLines("FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Fixer;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A, B, C> extends Kind<Kind<Kind<Foo_, A>, B>, C> {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A, B, C> Foo<A, B, C> narrowK(Kind<Kind<Kind<Foo_, A>, B>, ? extends C> hkt) {",
        "return (Foo<A, B, C>) hkt;",
        "}",
        
        "static <A, B, C> Fixer<Kind<Kind<Kind<Foo_, A>, B>, C>, Foo<A, B, C>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated, witness);
  }
}