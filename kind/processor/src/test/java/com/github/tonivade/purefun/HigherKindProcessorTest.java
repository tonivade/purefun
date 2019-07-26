/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

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

        "@HigherKind(name = \"x\")",
        "public class Foo<T> {",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }

  @Test
  public void compilesKind1WithBounds() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T extends Kind> {",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }

  @Test
  public void compilesKind2() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T, V> {",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }

  @Test
  public void compilesKind2WithBounds() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T extends Kind, V> {",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }

  @Test
  public void compilesKind3() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T, V, U> {",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }

  @Test
  public void compilesKind3WithBounds() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",

        "@HigherKind",
        "public class Foo<T extends Kind, V, U> {",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }
}
