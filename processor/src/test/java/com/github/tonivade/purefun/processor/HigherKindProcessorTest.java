/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.processor;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;
import javax.tools.JavaFileObject;
import org.junit.jupiter.api.Test;

public class HigherKindProcessorTest {

  @Test
  public void compilesKind1() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public final class Foo<T> implements FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import java.util.function.Function;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<A> extends Kind<Foo<?>, A> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A> Foo<A> narrowK(Kind<Foo<?>, ? extends A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "static <A> Function<Kind<Foo<?>, ? extends A>, Foo<A>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesKind1NoPackage() {
    JavaFileObject file = forSourceLines("test.Foo",
        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public final class Foo<T> implements FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "import com.github.tonivade.purefun.Kind;",
        "import java.util.function.Function;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<A> extends Kind<Foo<?>, A> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A> Foo<A> narrowK(Kind<Foo<?>, ? extends A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "static <A> Function<Kind<Foo<?>, ? extends A>, Foo<A>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesKind1Bounds() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public final class Foo<T extends String> implements FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import java.util.function.Function;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<A extends java.lang.String> extends Kind<Foo<?>, A> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A extends java.lang.String> Foo<A> narrowK(Kind<Foo<?>, ? extends A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "static <A extends java.lang.String> Function<Kind<Foo<?>, ? extends A>, Foo<A>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesError() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public final class Foo {",
        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .failsToCompile();
  }

  @Test
  public void compilesErrorNonFinal() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T extends String> implements FooOf<T> {",
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
        "public final class Foo<A, B> implements FooOf<A, B> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import java.util.function.Function;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<A, B> extends Kind<Kind<Foo<?, ?>, A>, B> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A, B> Foo<A, B> narrowK(Kind<Kind<Foo<?, ?>, A>, ? extends B> hkt) {",
        "return (Foo<A, B>) hkt;",
        "}",

        "static <A, B> Function<Kind<Kind<Foo<?, ?>, A>, ? extends B>, Foo<A, B>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesKind3() {
    JavaFileObject file = forSourceLines("Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public final class Foo<A, B, C> implements FooOf<A, B, C> {",
        "}");

    JavaFileObject generated = forSourceLines("FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import java.util.function.Function;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<A, B, C> extends Kind<Kind<Kind<Foo<?, ?, ?>, A>, B>, C> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A, B, C> Foo<A, B, C> narrowK(Kind<Kind<Kind<Foo<?, ?, ?>, A>, B>, ? extends C> hkt) {",
        "return (Foo<A, B, C>) hkt;",
        "}",

        "static <A, B, C> Function<Kind<Kind<Kind<Foo<?, ?, ?>, A>, B>, ? extends C>, Foo<A, B, C>> toFoo() {",
        "return FooOf::narrowK;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }
}