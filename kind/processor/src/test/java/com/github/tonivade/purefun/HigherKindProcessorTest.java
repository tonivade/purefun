package com.github.tonivade.purefun;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

import javax.tools.JavaFileObject;

import org.junit.jupiter.api.Test;

import com.google.testing.compile.JavaFileObjects;

public class HigherKindProcessorTest {

  @Test
  public void testName() {
    JavaFileObject file = JavaFileObjects.forSourceLines("test.Foo", 
        "package test;",

        "import com.github.tonivade.purefun.HigherKind;",
        "import com.github.tonivade.purefun.Kind;",
        "import com.github.tonivade.purefun.Higher1;",

        "@HigherKind",
        "public class Foo<T> implements Higher1<Foo.µ, T> {",
          "public static final class µ implements Kind {}",
          "public static <T> Foo<T> narrowK(Higher1<Foo.µ, T> hkt) {",
            "return (Foo<T>) hkt;",
          "}",
        "}");
    
    assert_().about(javaSource()).that(file)
      .processedWith(new HigherKindProcessor())
      .compilesWithoutError();
  }
}
