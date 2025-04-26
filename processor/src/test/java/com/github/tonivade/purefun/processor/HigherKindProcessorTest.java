/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
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
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<T> extends Kind<Foo<?>, T> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <T> Foo<T> toFoo(Kind<Foo<?>, ? extends T> value) {",
        "return (Foo<T>) value;",
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
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<T> extends Kind<Foo<?>, T> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <T> Foo<T> toFoo(Kind<Foo<?>, ? extends T> value) {",
        "return (Foo<T>) value;",
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
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<T extends java.lang.String> extends Kind<Foo<?>, T> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <T extends java.lang.String> Foo<T> toFoo(Kind<Foo<?>, ? extends T> value) {",
        "return (Foo<T>) value;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesKind1RecursiveBounds() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public final class Foo<T extends Kind<T, ?>> implements FooOf<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.FooOf",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<T extends com.github.tonivade.purefun.Kind<T, ?>> extends Kind<Foo<?>, T> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <T extends com.github.tonivade.purefun.Kind<T, ?> Foo<T> toFoo(Kind<Foo<?>, ? extends T> value) {",
        "return (Foo<T>) value;",
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
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<A, B> extends Kind<Foo<A, ?>, B> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A, B> Foo<A, B> toFoo(Kind<Foo<A, ?>, ? extends B> value) {",
        "return (Foo<A, B>) value;",
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
        "import javax.annotation.processing.Generated;",

        "@Generated(\"com.github.tonivade.purefun.processor.HigherKindProcessor\")",
        "public sealed interface FooOf<A, B, C> extends Kind<Foo<A, B, ?>, C> permits Foo {",

        "@SuppressWarnings(\"unchecked\")",
        "static <A, B, C> Foo<A, B, C> toFoo(Kind<Foo<A, B, ?>, ? extends C> value) {",
        "return (Foo<A, B, C>) value;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }
}