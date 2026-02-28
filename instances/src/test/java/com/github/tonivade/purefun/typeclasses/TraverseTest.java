/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.typeclasses;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.typeclasses.Nested.nest;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.ConstInstances;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.instances.SequenceInstances;
import com.github.tonivade.purefun.instances.TryInstances;
import com.github.tonivade.purefun.type.Const;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.Try;

public class TraverseTest {

  @Test
  public void sequence() {
    Sequence<Option<String>> seq = listOf(Option.some("a"), Option.some("b"), Option.some("c"));

    Traverse<Sequence<?>> instance = SequenceInstances.traverse();

    Kind<Option<?>, Kind<Sequence<?>, String>> result =
        instance.traverse(OptionInstances.applicative(), seq, x -> x.map(String::toUpperCase));

    assertEquals(Option.some(listOf("A", "B", "C")), result);
  }

  @Test
  public void either() {
    Traverse<Either<Throwable, ?>> instance = EitherInstances.traverse();

    Exception error = new Exception("error");

    assertAll(
        () -> assertEquals(Option.some(Either.right("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Either.<Throwable, Option<String>>right(Option.some("hello!")),
                t -> t.map(String::toUpperCase))),
        () -> assertEquals(Option.some(Either.left(error)),
            instance.traverse(OptionInstances.applicative(), Either.<Throwable, Option<String>>left(error),
                t -> t.map(String::toUpperCase))));
  }

  @Test
  public void composed() {
    Traverse<Nested<Option<?>, Id<?>>> composed = Traverse.compose(OptionInstances.traverse(), IdInstances.traverse());

    assertEquals(Try.success(Option.some(Id.of("HOLA!"))),
        composed.traverse(TryInstances.applicative(), nest(Option.some(Id.of(Try.success("hola!")))),
            t -> t.map(String::toUpperCase)));
  }

  @Test
  public void id() {
    Traverse<Id<?>> instance = IdInstances.traverse();

    assertAll(
        () -> assertEquals(Option.some(Id.of("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Id.of(Option.some("hello!")),
                t -> t.map(String::toUpperCase))));
  }

  @Test
  public void testConst() {
    Traverse<Const<String, ?>> instance = ConstInstances.traverse();

    assertAll(
        () -> assertEquals(Option.some(Const.of("hello!")),
            instance.traverse(OptionInstances.applicative(), Const.<String, String>of("hello!"),
                t -> Option.some(t))));
  }

  @Test
  public void option() {
    Traverse<Option<?>> instance = OptionInstances.traverse();

    assertAll(
        () -> assertEquals(Try.success(Option.some("HELLO!")),
            instance.traverse(TryInstances.applicative(), Option.some(Try.success("hello!")),
                t -> t.map(String::toUpperCase))),
        () -> assertEquals(Try.success(Option.none()),
            instance.traverse(TryInstances.applicative(), Option.<Try<String>>none(),
                t -> t.map(String::toUpperCase))));
  }

  @Test
  public void testTry() {
    Traverse<Try<?>> instance = TryInstances.traverse();

    Exception error = new Exception("error");

    assertAll(
        () -> assertEquals(Option.some(Try.success("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Try.success(Option.some("hello!")),
                t -> t.map(String::toUpperCase))),
        () -> assertEquals(Option.some(Try.failure(error)),
            instance.traverse(OptionInstances.applicative(), Try.<Option<String>>failure(error),
                t -> t.map(String::toUpperCase))));
  }
}
