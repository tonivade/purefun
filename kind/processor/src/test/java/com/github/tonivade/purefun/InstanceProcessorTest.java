package com.github.tonivade.purefun;

import org.junit.jupiter.api.Test;

import javax.tools.JavaFileObject;

import static com.google.common.truth.Truth.assert_;
import static com.google.testing.compile.JavaFileObjects.forSourceLines;
import static com.google.testing.compile.JavaSourceSubjectFactory.javaSource;

public class InstanceProcessorTest {

  @Test
  public void compilesInstances() {
    JavaFileObject file = forSourceLines("test.Foo",
        "package test;",

        "import com.github.tonivade.purefun.Instance;",

        "public interface Foo {",
        "}",
        "@Instance",
        "interface Bar {",
        "}",
        "@Instance",
        "interface Baz<T> extends Bar {",
        "}"
    );

    assert_().about(javaSource()).that(file)
        .processedWith(new InstanceProcessor())
        .compilesWithoutError();
  }
}
