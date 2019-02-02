/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.stream.Stream.StreamOf;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.type.Option;

import org.junit.jupiter.api.Test;

public class StreamTest {

  final StreamOf<IO.µ> streamOfIO = Stream.ofIO();
  final StreamOf<Id.µ> streamOfId = Stream.ofId();

  @Test
  public void map() {
    Stream<IO.µ, String> pure1 = streamOfIO.pure("hola");
    Stream<IO.µ, String> pure2 = streamOfIO.pure(" mundo");

    Stream<IO.µ, String> result = pure1.concat(pure2).map(String::toUpperCase);

    IO<String> foldRight = result.foldRight(IO.pure(""), (a, b) -> b.fix1(IO::narrowK).map(x -> x + a))
        .fix1(IO::narrowK);

    assertEquals("HOLA MUNDO", foldRight.unsafeRunSync());
  }

  @Test
  public void mapOfId() {
    Stream<Id.µ, String> pure1 = streamOfId.pure("hola");
    Stream<Id.µ, String> pure2 = streamOfId.pure(" mundo");

    Stream<Id.µ, String> result = pure1.concat(pure2)
        .flatMap(string -> streamOfId.suspend(() -> streamOfId.pure(string.toUpperCase())));

    Id<String> foldLeft = result.asString().fix1(Id::narrowK);

    assertEquals("HOLA MUNDO", foldLeft.get());
  }

  @Test
  public void flatMap() {
    Stream<IO.µ, String> pure1 = streamOfIO.pure("hola");
    Stream<IO.µ, String> pure2 = streamOfIO.pure(" mundo");

    Stream<IO.µ, String> result = pure1.concat(pure2).flatMap(string -> streamOfIO.pure(string.toUpperCase()));

    IO<String> foldLeft = result.asString().fix1(IO::narrowK);

    assertEquals("HOLA MUNDO", foldLeft.unsafeRunSync());
  }

  @Test
  public void mapEval() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.mapEval(i -> IO.of(() -> i * 2));

    assertEquals(Integer.valueOf(12), run(result.foldLeft(0, (a, b) -> a + b)));
  }

  @Test
  public void append() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.append(IO.pure(4));

    assertEquals(listOf(1, 2, 3, 4), run(result.asSequence()));
  }

  @Test
  public void prepend() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.prepend(IO.pure(0));

    assertEquals(listOf(0, 1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void take() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.take(2);

    assertEquals(listOf(1, 2), run(result.asSequence()));
  }

  @Test
  public void drop() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO.µ, Integer> result = stream.drop(2);

    assertEquals(listOf(3), run(result.asSequence()));
  }

  @Test
  public void takeWhile() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(listOf(1, 2, 3, 4, 5));

    Stream<IO.µ, Integer> result = stream.takeWhile(t -> t < 4);

    assertEquals(listOf(1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void dropWhile() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(listOf(1, 2, 3, 4, 5));

    Stream<IO.µ, Integer> result = stream.dropWhile(t -> t < 4);

    assertEquals(listOf(4, 5), run(result.asSequence()));
  }

  @Test
  public void filter() {
    Stream<IO.µ, Integer> stream = streamOfIO.from(java.util.stream.Stream.of(1, 2, 3, 4, 5));

    Stream<IO.µ, Integer> result = stream.filter(t -> (t % 2) == 0);

    assertEquals(listOf(2, 4), run(result.asSequence()));
  }

  @Test
  public void collect() {
    Stream<IO.µ, Integer> stream = streamOfIO.of(1, 2, 3, 4, 5);

    Stream<IO.µ, Integer> result = stream.collect(PartialFunction1.of(t -> (t % 2) == 0, x -> x * 2));

    assertEquals(listOf(4, 8), run(result.asSequence()));
  }

  @Test
  public void iterate() {
    Stream<IO.µ, Integer> stream = streamOfIO.iterate(0, i -> i + 1);

    Stream<IO.µ, Integer> result = stream.takeWhile(t -> t < 4).dropWhile(t -> t < 2);

    assertEquals(listOf(2, 3), run(result.asSequence()));
  }

  @Test
  public void repeat() {
    Stream<IO.µ, Integer> stream = streamOfIO.of(1, 2, 3);

    Stream<IO.µ, Integer> result = stream.repeat().take(7);

    assertEquals(listOf(1, 2, 3, 1, 2, 3, 1), run(result.asSequence()));
  }

  @Test
  public void intersperse() {
    Stream<IO.µ, Integer> stream = streamOfIO.of(1, 2);

    Stream<IO.µ, Integer> result = stream.intersperse(IO.pure(0));

    assertEquals(listOf(1, 0, 2, 0), run(result.asSequence()));
  }

  @Test
  public void zip() {
    Stream<IO.µ, String> stream = streamOfIO.from(listOf("a", "b", "c"));

    IO<Sequence<Tuple2<String, Integer>>> zip = streamOfIO.zipWithIndex(stream).asSequence().fix1(IO::narrowK);

    assertEquals(listOf(Tuple2.of("a", 0), Tuple2.of("b", 1), Tuple2.of("c", 2)), zip.unsafeRunSync());
  }

  @Test
  public void merge() {
    Stream<IO.µ, Integer> stream1 = streamOfIO.of(1, 2, 3);
    Stream<IO.µ, Integer> stream2 = streamOfIO.of(4, 5, 6, 7);

    Stream<IO.µ, Integer> merge = streamOfIO.merge(stream1, stream2);

    assertEquals(listOf(1, 4, 2, 5, 3, 6), run(merge.asSequence()));
  }

  @Test
  public void forAll() {
    Stream<IO.µ, String> stream = streamOfIO.from(listOf("a", "b", "c"));

    Higher1<IO.µ, Boolean> all = stream.exists(x -> x.toLowerCase().equals(x));
    Higher1<IO.µ, Boolean> notAll = stream.exists(x -> x.toUpperCase().equals(x));

    assertTrue(run(all));
    assertFalse(run(notAll));
  }

  @Test
  public void exists() {
    Stream<IO.µ, String> stream = streamOfIO.from(listOf("a", "b", "c"));

    Higher1<IO.µ, Boolean> exists = stream.exists(x -> x.equals("c"));
    Higher1<IO.µ, Boolean> notExists = stream.exists(x -> x.equals("z"));

    assertTrue(run(exists));
    assertFalse(run(notExists));
  }

  @Test
  public void foldLeftLazyness() {
    IO<String> fail = IO.failure(new NullPointerException());

    IO<String> result = streamOfIO.eval(fail).asString().fix1(IO::narrowK);

    assertThrows(NullPointerException.class, result::unsafeRunSync);
  }

  @Test
  public void foldRightLazyness() {
    IO<String> fail = IO.failure(new NullPointerException());

    IO<String> result = streamOfIO.eval(fail)
        .foldRight(IO.pure(""), (a, b) -> b.fix1(IO::narrowK).map(x -> a + x)).fix1(IO::narrowK);

    assertThrows(NullPointerException.class, result::unsafeRunSync);
  }

  @Test
  public void readFile() {
    assertAll(
        () -> assertEquals(impureReadFile("LICENSE"), pureReadFile("LICENSE").unsafeRunSync()),
        () -> assertEquals("--- file not found ---", pureReadFile("lkjasdf").unsafeRunSync()));
  }

  private IO<String> pureReadFile(String file) {
    return streamOfIO.eval(IO.of(() -> reader(file)))
      .flatMap(reader -> streamOfIO.iterate(() -> Option.of(() -> readLine(reader))))
      .takeWhile(Option::isPresent)
      .map(Option::get)
      .foldLeft("", (a, b) -> a + "\n" + b)
      .fix1(IO::narrowK)
      .recoverWith(UncheckedIOException.class, cons("--- file not found ---"));
  }

  public String impureReadFile(String file) {
    String content = "";
    try (BufferedReader reader = reader(file)) {
      while (true) {
        String line = readLine(reader);
        if (nonNull(line)) {
          content = content + "\n" + line;
        } else break;
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return content;
  }

  private String readLine(BufferedReader reader) {
    try {
      return reader.readLine();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private BufferedReader reader(String file) {
    try {
      return Files.newBufferedReader(Paths.get(file));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static <T> T run(Higher1<IO.µ, T> effect) {
    return effect.fix1(IO::narrowK).unsafeRunSync();
  }
}
