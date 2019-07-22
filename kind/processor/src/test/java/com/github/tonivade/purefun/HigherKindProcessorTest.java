/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import javax.tools.JavaFileObject;

import org.junit.jupiter.api.Test;

import com.google.testing.compile.JavaFileObjects;

public class HigherKindProcessorTest {

  @Test
  public void compiles() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",
        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",

        "@HigherKind",
        "public class Foo<T> implements Higher1<Foo.µ, T> {",
          "public static final class µ implements Kind {}",
        "}");

    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }
}
