/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.concurrent.FutureOf.toFuture;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.effect.EIOOf.toEIO;
import static com.github.tonivade.purefun.effect.TaskOf.toTask;
import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
import static com.github.tonivade.purefun.effect.ZIOOf.toZIO;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
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

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.ZIO;
import com.github.tonivade.purefun.effect.ZIO_;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.StreamInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.stream.Stream.StreamOf;
import com.github.tonivade.purefun.type.Option;

public class StreamTest {

  private final StreamOf<IO_> streamOfIO = StreamInstances.ofIO();
  private final StreamOf<UIO_> streamOfUIO = StreamInstances.ofUIO();
  private final StreamOf<Task_> streamOfTask = StreamInstances.ofTask();
  private final StreamOf<Kind<EIO_, Throwable>> streamOfEIO = StreamInstances.ofEIO();
  private final StreamOf<Kind<Kind<ZIO_, Nothing>, Throwable>> streamOfZIO = StreamInstances.ofZIO();

  @Test
  public void map() {
    Stream<IO_, String> pure1 = streamOfIO.pure("hola");
    Stream<IO_, String> pure2 = streamOfIO.pure(" mundo");

    Stream<IO_, String> result = pure1.concat(pure2).map(String::toUpperCase);

    IO<String> foldRight = result.foldRight(IO.pure(""), (a, b) -> b.fix(toIO()).map(x -> x + a))
        .fix(toIO());

    assertEquals("HOLA MUNDO", foldRight.unsafeRunSync());
  }

  @Test
  public void flatMap() {
    Stream<IO_, String> pure1 = streamOfIO.pure("hola");
    Stream<IO_, String> pure2 = streamOfIO.pure(" mundo");

    Stream<IO_, String> result = pure1.concat(pure2).flatMap(string -> streamOfIO.pure(string.toUpperCase()));

    IO<String> foldLeft = result.asString().fix(toIO());

    assertEquals("HOLA MUNDO", foldLeft.unsafeRunSync());
  }

  @Test
  public void mapEval() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO_, Integer> result = stream.mapEval(i -> IO.task(() -> i * 2));

    assertEquals(Integer.valueOf(12), run(result.foldLeft(0, Integer::sum)));
  }

  @Test
  public void append() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO_, Integer> result = stream.append(IO.pure(4));

    assertEquals(listOf(1, 2, 3, 4), run(result.asSequence()));
  }

  @Test
  public void prepend() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO_, Integer> result = stream.prepend(IO.pure(0));

    assertEquals(listOf(0, 1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void take() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO_, Integer> result = stream.take(2);

    assertEquals(listOf(1, 2), run(result.asSequence()));
  }

  @Test
  public void drop() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf(1, 2, 3));

    Stream<IO_, Integer> result = stream.drop(2);

    assertEquals(listOf(3), run(result.asSequence()));
  }

  @Test
  public void takeWhile() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf(1, 2, 3, 4, 5));

    Stream<IO_, Integer> result = stream.takeWhile(t -> t < 4);

    assertEquals(listOf(1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void dropWhile() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf(1, 2, 3, 4, 5));

    Stream<IO_, Integer> result = stream.dropWhile(t -> t < 4);

    assertEquals(listOf(4, 5), run(result.asSequence()));
  }

  @Test
  public void filter() {
    Stream<IO_, Integer> stream = streamOfIO.from(java.util.stream.Stream.of(1, 2, 3, 4, 5));

    Stream<IO_, Integer> result = stream.filter(t -> (t % 2) == 0);

    assertEquals(listOf(2, 4), run(result.asSequence()));
  }

  @Test
  public void collect() {
    Stream<IO_, Integer> stream = streamOfIO.of(1, 2, 3, 4, 5);

    Stream<IO_, Integer> result = stream.collect(PartialFunction1.of(t -> (t % 2) == 0, x -> x * 2));

    assertEquals(listOf(4, 8), run(result.asSequence()));
  }

  @Test
  public void iterate() {
    Stream<IO_, Integer> stream = streamOfIO.iterate(0, i -> i + 1);

    Stream<IO_, Integer> result = stream.takeWhile(t -> t < 4).dropWhile(t -> t < 2);

    assertEquals(listOf(2, 3), run(result.asSequence()));
  }

  @Test
  public void repeat() {
    Stream<IO_, Integer> stream = streamOfIO.of(1, 2, 3);

    Stream<IO_, Integer> result = stream.repeat().take(7);

    assertEquals(listOf(1, 2, 3, 1, 2, 3, 1), run(result.asSequence()));
  }

  @Test
  public void intersperse() {
    Stream<IO_, Integer> stream = streamOfIO.of(1, 2);

    Stream<IO_, Integer> result = stream.intersperse(IO.pure(0));

    assertEquals(listOf(1, 0, 2, 0), run(result.asSequence()));
  }

  @Test
  public void zip() {
    Stream<IO_, String> stream = streamOfIO.from(listOf("a", "b", "c"));

    IO<Sequence<Tuple2<String, Integer>>> zip = streamOfIO.zipWithIndex(stream).asSequence().fix(toIO());

    assertEquals(listOf(Tuple2.of("a", 0), Tuple2.of("b", 1), Tuple2.of("c", 2)), zip.unsafeRunSync());
  }

  @Test
  public void merge() {
    Stream<IO_, Integer> stream1 = streamOfIO.of(1, 2, 3);
    Stream<IO_, Integer> stream2 = streamOfIO.of(4, 5, 6, 7);

    Stream<IO_, Integer> merge = streamOfIO.merge(stream1, stream2);

    assertEquals(listOf(1, 4, 2, 5, 3, 6), run(merge.asSequence()));
  }

  @Test
  public void forAll() {
    Stream<IO_, String> stream = streamOfIO.from(listOf("a", "b", "c"));

    Kind<IO_, Boolean> all = stream.exists(x -> x.toLowerCase().equals(x));
    Kind<IO_, Boolean> notAll = stream.exists(x -> x.toUpperCase().equals(x));

    assertTrue(run(all));
    assertFalse(run(notAll));
  }

  @Test
  public void exists() {
    Stream<IO_, String> stream = streamOfIO.from(listOf("a", "b", "c"));

    Kind<IO_, Boolean> exists = stream.exists(x -> "c".equals(x));
    Kind<IO_, Boolean> notExists = stream.exists(x -> "z".equals(x));

    assertTrue(run(exists));
    assertFalse(run(notExists));
  }

  @Test
  public void foldLeftLazyness() {
    IO<String> fail = IO.raiseError(new IllegalAccessException());

    IO<String> result = streamOfIO.eval(fail).asString().fix(toIO());

    assertThrows(IllegalAccessException.class, result::unsafeRunSync);
  }

  @Test
  public void foldRightLazyness() {
    IO<String> fail = IO.raiseError(new IllegalAccessException());

    IO<String> result = streamOfIO.eval(fail)
      .foldRight(IO.pure(""), (a, b) -> b.fix(toIO()).map(x -> a + x))
      .fix(toIO());

    assertThrows(IllegalAccessException.class, result::unsafeRunSync);
  }

  @Test
  public void readFileIO() {
    IO<String> license = pureReadFileIO("../LICENSE");
    IO<String> notFound = pureReadFileIO("hjsjkdf");
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.unsafeRunSync()),
        () -> assertEquals("--- file not found ---", notFound.unsafeRunSync()));
  }

  @Test
  public void readFileUIO() {
    UIO<String> license = pureReadFileUIO("../LICENSE");
    UIO<String> notFound = pureReadFileUIO("hjsjkdf");
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.unsafeRunSync()),
        () -> assertEquals("--- file not found ---", notFound.unsafeRunSync()));
  }

  @Test
  public void readFileTask() {
    UIO<String> license = pureReadFileTask("../LICENSE");
    UIO<String> notFound = pureReadFileTask("hjsjkdf");
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.unsafeRunSync()),
        () -> assertEquals("--- file not found ---", notFound.unsafeRunSync()));
  }

  @Test
  public void readFileEIO() {
    UIO<String> license = pureReadFileEIO("../LICENSE");
    UIO<String> notFound = pureReadFileEIO("hjsjkdf");
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.unsafeRunSync()),
        () -> assertEquals("--- file not found ---", notFound.unsafeRunSync()));
  }

  @Test
  public void readFileZIO() {
    ZIO<Nothing, Nothing, String> license = pureReadFileZIO("../LICENSE");
    ZIO<Nothing, Nothing, String> notFound = pureReadFileZIO("hjsjkdf");
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.provide(nothing()).get()),
        () -> assertEquals("--- file not found ---", notFound.provide(nothing()).get()));
  }

  @Test
  public void readFileAsync() {
    Future<String> license = pureReadFileIO("../LICENSE").foldMap(FutureInstances.async()).fix(toFuture());
    Future<String> notFound = pureReadFileIO("hjsjkdf").foldMap(FutureInstances.async()).fix(toFuture());
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.await().get()),
        () -> assertEquals("--- file not found ---", notFound.await().get()));
  }
  
  @Test
  public void test() {
    Stream<IO_, Integer> stream = streamOfIO.from(listOf("a", "b", "c")).mapReplace(IO.pure(1));
    
    assertEquals("111", stream.asString().fix(toIO()).unsafeRunSync());
  }

  private IO<String> pureReadFileIO(String file) {
    return streamOfIO.eval(IO.task(() -> reader(file)))
        .flatMap(reader -> streamOfIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::get)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toIO())
        .recoverWith(UncheckedIOException.class, cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileUIO(String file) {
    return streamOfUIO.eval(UIO.task(() -> reader(file)))
        .flatMap(reader -> streamOfUIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::get)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toUIO())
        .recoverWith(UncheckedIOException.class, cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileTask(String file) {
    return streamOfTask.eval(Task.task(() -> reader(file)))
        .flatMap(reader -> streamOfTask.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::get)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toTask())
        .recover(cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileEIO(String file) {
    return streamOfEIO.eval(EIO.task(() -> reader(file)))
        .flatMap(reader -> streamOfEIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::get)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toEIO())
        .recover(cons("--- file not found ---"));
  }

  private ZIO<Nothing, Nothing, String> pureReadFileZIO(String file) {
    return streamOfZIO.eval(ZIO.<Nothing, BufferedReader>task(() -> reader(file)))
      .flatMap(reader -> streamOfZIO.iterate(() -> Option.of(() -> readLine(reader))))
      .takeWhile(Option::isPresent)
      .map(Option::get)
      .foldLeft("", (a, b) -> a + '\n' + b)
      .fix(toZIO())
      .recover(cons("--- file not found ---"));
  }

  private String impureReadFile(String file) {
    String content = "";
    try (BufferedReader reader = reader(file)) {
      while (true) {
        String line = readLine(reader);
        if (nonNull(line)) {
          content = content + "\n" + line;
        } else {
          break;
        }
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

  private static <T> T run(Kind<IO_, T> effect) {
    return effect.fix(toIO()).unsafeRunSync();
  }
}
