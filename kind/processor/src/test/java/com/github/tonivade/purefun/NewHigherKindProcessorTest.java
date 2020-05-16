package com.github.tonivade.purefun;

import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class NewHigherKindProcessorTest {

  @Test
  public void compilesKind1() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T> {",
        "}");

    JavaFileObject generated = forSourceLines("test.Foo_",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public final class Foo_ implements Kind {",

        "public static <A> Foo<A> narrowK(Higher1<Foo_, A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new NewHigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesKind1Bounds() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T extends String> {",
        "}");

    JavaFileObject generated = forSourceLines("test.Foo_",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public final class Foo_ implements Kind {",

        "public static <A extends java.lang.String> Foo<A> narrowK(Higher1<Foo_, A> hkt) {",
        "return (Foo<A>) hkt;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new NewHigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesKind2() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<A, B> {",
        "}");

    JavaFileObject generated = forSourceLines("test.Foo_",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",
        "import com.github.tonivade.purefun.Higher2;",
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public final class Foo_ implements Kind {",

        "public static <A, B> Foo<A, B> narrowK(Higher1<Higher1<Foo_, A>, B> hkt) {",
        "return (Foo<A, B>) hkt;",
        "}",

        "public static <A, B> Foo<A, B> narrowK(Higher2<Foo_, A, B> hkt) {",
        "return (Foo<A, B>) hkt;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new NewHigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }

  @Test
  public void compilesKind3() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<A, B, C> {",
        "}");

    JavaFileObject generated = forSourceLines("test.Foo_",
        "package test;",

        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",
        "import com.github.tonivade.purefun.Higher2;",
        "import com.github.tonivade.purefun.Higher3;",
        "import javax.annotation.Generated;",

        "@Generated(\"com.github.tonivade.purefun.HigherKindProcessor\")",
        "public final class Foo_ implements Kind {",

        "public static <A, B, C> Foo<A, B, C> narrowK(Higher1<Higher1<Higher1<Foo_, A>, B>, C> hkt) {",
        "return (Foo<A, B, C>) hkt;",
        "}",

        "public static <A, B, C> Foo<A, B, C> narrowK(Higher2<Higher1<Foo_, A>, B, C> hkt) {",
        "return (Foo<A, B, C>) hkt;",
        "}",

        "public static <A, B, C> Foo<A, B, C> narrowK(Higher3<Foo_, A, B, C> hkt) {",
        "return (Foo<A, B, C>) hkt;",
        "}",

        "}");

    assert_().about(javaSource()).that(file)
        .processedWith(new NewHigherKindProcessor())
        .compilesWithoutError().and().generatesSources(generated);
  }
}