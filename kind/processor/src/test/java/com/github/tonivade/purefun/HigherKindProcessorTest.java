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
  public void compiles() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",
        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",

        "@HigherKind",
        "public class Foo<T> {",
          "public String toString() { return \"Foo\"; }",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }

  @Test
  public void compilesWhenMoreThanOneDefinition() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",
        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",

        "@HigherKind",
        "public class Foo<T> {",
          "public String toString() { return \"Foo\"; }",
        "}",
        "interface FooModule {}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }
}
