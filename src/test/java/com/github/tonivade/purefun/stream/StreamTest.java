/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Eval.now;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Comonad;
import com.github.tonivade.purefun.typeclasses.Monad;

public class StreamTest {

  final Monad<IO.µ> monad = IO.monad();
  final Comonad<IO.µ> comonad = IO.comonad();

  @Test
  public void map() {
    Stream<IO.µ, String> pure1 = Stream.pure(monad, comonad, "hola");
    Stream<IO.µ, String> pure2 = Stream.pure(monad, comonad, " mundo");

    Stream<IO.µ, String> result = pure1.concat(pure2).map(String::toUpperCase);

    Eval<IO<String>> foldRight = result.foldRight(now(""), (a, b) -> b.map(x -> x + a)).map(IO::narrowK);

    assertEquals("HOLA MUNDO", foldRight.value().unsafeRunSync());
  }

  @Test
  public void flatMap() {
    Stream<IO.µ, String> pure1 = Stream.pure(monad, comonad, "hola");
    Stream<IO.µ, String> pure2 = Stream.pure(monad, comonad, " mundo");

    Stream<IO.µ, String> result = pure1.concat(pure2)
        .flatMap(string -> Stream.pure(monad, comonad, string.toUpperCase()));

    IO<String> foldLeft = result.asString().fix1(IO::narrowK);

    assertEquals("HOLA MUNDO", foldLeft.unsafeRunSync());
  }

  @Test
  public void mapEval() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, Sequence.listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.mapEval(i -> IO.of(() -> i * 2));

    assertEquals(Integer.valueOf(12), run(result.foldLeft(0, (a, b) -> a + b)));
  }

  @Test
  public void append() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, Sequence.listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.append(IO.pure(4));

    assertEquals(listOf(1, 2, 3, 4), run(result.asSequence()));
  }

  @Test
  public void prepend() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, Sequence.listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.prepend(IO.pure(0));

    assertEquals(listOf(0, 1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void take() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, Sequence.listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.take(2);

    assertEquals(listOf(1, 2), run(result.asSequence()));
  }

  @Test
  public void drop() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, Sequence.listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.drop(2);

    assertEquals(listOf(3), run(result.asSequence()));
  }

  @Test
  public void takeWhile() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, Sequence.listOf(1, 2, 3, 4, 5));

    Stream<IO.µ, Integer> result = stream.takeWhile(t -> t < 4);

    assertEquals(listOf(1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void dropWhile() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, Sequence.listOf(1, 2, 3, 4, 5));

    Stream<IO.µ, Integer> result = stream.dropWhile(t -> t < 4);

    assertEquals(listOf(4, 5), run(result.asSequence()));
  }

  @Test
  public void filter() {
    Stream<IO.µ, Integer> stream = Stream.from(monad, comonad, java.util.stream.Stream.of(1, 2, 3, 4, 5));

    Stream<IO.µ, Integer> result = stream.filter(t -> (t % 2) == 0);

    assertEquals(listOf(2, 4), run(result.asSequence()));
  }

  @Test
  public void iterate() {
    Stream<IO.µ, Integer> stream = Stream.iterate(monad, comonad, 0, i -> i + 1);

    Stream<IO.µ, Integer> result = stream.takeWhile(t -> t < 4).dropWhile(t -> t < 2);

    assertEquals(listOf(2, 3), run(result.asSequence()));
  }

  @Test
  public void repeat() {
    Stream<IO.µ, Integer> stream = Stream.of(monad, comonad, 1, 2, 3);

    Stream<IO.µ, Integer> result = stream.repeat().take(7);

    assertEquals(listOf(1, 2, 3, 1, 2, 3, 1), run(result.asSequence()));
  }

  @Test
  public void intersperse() {
    Stream<IO.µ, Integer> stream = Stream.of(monad, comonad, 1, 2);

    Stream<IO.µ, Integer> result = stream.intersperse(IO.pure(0));

    assertEquals(listOf(1, 0, 2, 0), run(result.asSequence()));
  }

  private static <T> T run(Higher1<IO.µ, T> effect) {
    return effect.fix1(IO::narrowK).unsafeRunSync();
  }
}
