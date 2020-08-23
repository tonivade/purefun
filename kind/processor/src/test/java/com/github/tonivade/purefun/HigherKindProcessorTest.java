/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
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
        "import javax.annotation.Generated;",

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
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A> extends Kind<Foo_, A> {",

        "static <A> Foo<A> narrowK(Kind<Foo_, A> hkt) {",
        "return (Foo<A>) hkt;",
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
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A> extends Kind<Foo_, A> {",
        
        "SealedFoo<A> youShallNotPass();",

        "static <A> Foo<A> narrowK(Kind<Foo_, A> hkt) {",
        "return (Foo<A>) hkt;",
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
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A> extends Kind<Foo_, A> {",

        "static <A> Foo<A> narrowK(Kind<Foo_, A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "}");

    JavaFileObject generatedWitness = forSourceLines("Foo_",
        "import com.github.tonivade.purefun.Witness;",
        "import javax.annotation.Generated;",

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
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A extends java.lang.String> extends Kind<Foo_, A> {",

        "static <A extends java.lang.String> Foo<A> narrowK(Kind<Foo_, A> hkt) {",
        "return (Foo<A>) hkt;",
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
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A, B> extends Kind<Kind<Foo_, A>, B> {",

        "static <A, B> Foo<A, B> narrowK(Kind<Kind<Foo_, A>, B> hkt) {",
        "return (Foo<A, B>) hkt;",
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
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public interface FooOf<A, B, C> extends Kind<Kind<Kind<Foo_, A>, B>, C> {",

        "static <A, B, C> Foo<A, B, C> narrowK(Kind<Kind<Kind<Foo_, A>, B>, C> hkt) {",
        "return (Foo<A, B, C>) hkt;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new HigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated, witness);
  }
}