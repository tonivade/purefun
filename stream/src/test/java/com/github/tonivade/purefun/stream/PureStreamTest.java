/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.Function1.cons;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.effect.EIOOf.toEIO;
import static com.github.tonivade.purefun.effect.TaskOf.toTask;
import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
import static com.github.tonivade.purefun.effect.PureIOOf.toPureIO;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.PartialFunction1;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.annotation.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.EIO_;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.Task_;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.UIO_;
import com.github.tonivade.purefun.effect.URIO;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIO_;
import com.github.tonivade.purefun.instances.PureStreamInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.stream.PureStream.StreamOf;
import com.github.tonivade.purefun.type.Option;

public class PureStreamTest {

  private final StreamOf<IO_> streamOfIO = PureStreamInstances.ofIO();
  private final StreamOf<UIO_> streamOfUIO = PureStreamInstances.ofUIO();
  private final StreamOf<Task_> streamOfTask = PureStreamInstances.ofTask();
  private final StreamOf<Kind<EIO_, Throwable>> streamOfEIO = PureStreamInstances.ofEIO();
  private final StreamOf<Kind<Kind<PureIO_, Nothing>, Throwable>> streamOfPureIO = PureStreamInstances.ofPureIO();

  @Test
  public void map() {
    PureStream<IO_, String> pure1 = PureStream.pure("hola");
    PureStream<IO_, String> pure2 = PureStream.pure(" mundo");

    PureStream<IO_, String> result = pure1.concat(pure2).map(String::toUpperCase);

    IO<String> foldRight = result.foldRight(IO.pure(""), (a, b) -> b.fix(toIO()).map(x -> x + a))
        .fix(toIO());

    assertEquals("HOLA MUNDO", foldRight.unsafeRunSync());
  }

  @Test
  public void flatMap() {
    PureStream<IO_, String> pure1 = PureStream.pure("hola");
    PureStream<IO_, String> pure2 = PureStream.pure(" mundo");

    PureStream<IO_, String> result = pure1.concat(pure2).flatMap(string -> PureStream.pure(string.toUpperCase()));

    IO<String> foldLeft = result.asString().fix(toIO());

    assertEquals("HOLA MUNDO", foldLeft.unsafeRunSync());
  }

  @Test
  public void mapEval() {
    PureStream<IO_, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO_, Integer> result = stream.mapEval(i -> IO.task(() -> i * 2));

    assertEquals(Integer.valueOf(12), run(result.foldLeft(0, Integer::sum)));
  }

  @Test
  public void append() {
    PureStream<IO_, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO_, Integer> result = stream.append(IO.pure(4));

    assertEquals(listOf(1, 2, 3, 4), run(result.asSequence()));
  }

  @Test
  public void prepend() {
    PureStream<IO_, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO_, Integer> result = stream.prepend(IO.pure(0));

    assertEquals(listOf(0, 1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void take() {
    PureStream<IO_, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO_, Integer> result = stream.take(2);

    assertEquals(listOf(1, 2), run(result.asSequence()));
  }

  @Test
  public void drop() {
    PureStream<IO_, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO_, Integer> result = stream.drop(2);

    assertEquals(listOf(3), run(result.asSequence()));
  }

  @Test
  public void takeWhile() {
    PureStream<IO_, Integer> stream = PureStream.from(listOf(1, 2, 3, 4, 5));

    PureStream<IO_, Integer> result = stream.takeWhile(t -> t < 4);

    assertEquals(listOf(1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void dropWhile() {
    PureStream<IO_, Integer> stream = PureStream.from(listOf(1, 2, 3, 4, 5));

    PureStream<IO_, Integer> result = stream.dropWhile(t -> t < 4);

    assertEquals(listOf(4, 5), run(result.asSequence()));
  }

  @Test
  public void filter() {
    PureStream<IO_, Integer> stream = PureStream.from(java.util.stream.Stream.of(1, 2, 3, 4, 5));

    PureStream<IO_, Integer> result = stream.filter(t -> (t % 2) == 0);

    assertEquals(listOf(2, 4), run(result.asSequence()));
  }

  @Test
  public void collect() {
    PureStream<IO_, Integer> stream = PureStream.of(IO_.class).of(1, 2, 3, 4, 5);

    PureStream<IO_, Integer> result = stream.collect(PartialFunction1.of(t -> (t % 2) == 0, x -> x * 2));

    assertEquals(listOf(4, 8), run(result.asSequence()));
  }

  @Test
  public void iterate() {
    PureStream<IO_, Integer> stream = PureStream.iterate(0, i -> i + 1);

    PureStream<IO_, Integer> result = stream.takeWhile(t -> t < 4).dropWhile(t -> t < 2);

    assertEquals(listOf(2, 3), run(result.asSequence()));
  }

  @Test
  public void repeat() {
    PureStream<IO_, Integer> stream = PureStream.<IO_>of().of(1, 2, 3);

    PureStream<IO_, Integer> result = stream.repeat().take(7);

    assertEquals(listOf(1, 2, 3, 1, 2, 3, 1), run(result.asSequence()));
  }

  @Test
  public void intersperse() {
    PureStream<IO_, Integer> stream = PureStream.<IO_>of().of(1, 2);

    PureStream<IO_, Integer> result = stream.intersperse(IO.pure(0));

    assertEquals(listOf(1, 0, 2, 0), run(result.asSequence()));
  }

  @Test
  public void zip() {
    PureStream<IO_, String> stream = PureStream.from(listOf("a", "b", "c"));

    IO<Sequence<Tuple2<String, Integer>>> zip = PureStream.zipWithIndex(stream).asSequence().fix(toIO());

    assertEquals(listOf(Tuple2.of("a", 0), Tuple2.of("b", 1), Tuple2.of("c", 2)), zip.unsafeRunSync());
  }

  @Test
  public void merge() {
    PureStream<IO_, Integer> stream1 = PureStream.<IO_>of().of(1, 2, 3);
    PureStream<IO_, Integer> stream2 = PureStream.<IO_>of().of(4, 5, 6, 7);

    PureStream<IO_, Integer> merge = PureStream.merge(stream1, stream2);

    assertEquals(listOf(1, 4, 2, 5, 3, 6), run(merge.asSequence()));
  }

  @Test
  public void forAll() {
    PureStream<IO_, String> stream = PureStream.from(listOf("a", "b", "c"));

    Kind<IO_, Boolean> all = stream.exists(x -> x.toLowerCase().equals(x));
    Kind<IO_, Boolean> notAll = stream.exists(x -> x.toUpperCase().equals(x));

    assertTrue(run(all));
    assertFalse(run(notAll));
  }

  @Test
  public void exists() {
    PureStream<IO_, String> stream = PureStream.from(listOf("a", "b", "c"));

    Kind<IO_, Boolean> exists = stream.exists(x -> "c".equals(x));
    Kind<IO_, Boolean> notExists = stream.exists(x -> "z".equals(x));

    assertTrue(run(exists));
    assertFalse(run(notExists));
  }

  @Test
  public void foldLeftLazyness() {
    IO<String> fail = IO.raiseError(new IllegalAccessException());

    IO<String> result = PureStream.eval(fail).asString().fix(toIO());

    assertThrows(IllegalAccessException.class, result::unsafeRunSync);
  }

  @Test
  public void foldRightLazyness() {
    IO<String> fail = IO.raiseError(new IllegalAccessException());

    IO<String> result = PureStream.eval(fail)
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
  public void readFilePureIO() {
    URIO<Nothing, String> license = pureReadFilePureIO("../LICENSE");
    URIO<Nothing, String> notFound = pureReadFilePureIO("hjsjkdf");
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.unsafeRunSync(nothing())),
        () -> assertEquals("--- file not found ---", notFound.unsafeRunSync(nothing())));
  }

  @Test
  public void readFileAsync() {
    Future<String> license = pureReadFileIO("../LICENSE").runAsync();
    Future<String> notFound = pureReadFileIO("hjsjkdf").runAsync();
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.await().getOrElseThrow()),
        () -> assertEquals("--- file not found ---", notFound.await().getOrElseThrow()));
  }
  
  @Test
  public void test() {
    PureStream<IO_, Integer> stream = streamOfIO.from(listOf("a", "b", "c")).mapReplace(IO.pure(1));
    
    assertEquals("111", stream.asString().fix(toIO()).unsafeRunSync());
  }

  private IO<String> pureReadFileIO(String file) {
    return streamOfIO.eval(IO.task(() -> reader(file)))
        .flatMap(reader -> streamOfIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toIO())
        .recover(UncheckedIOException.class, cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileUIO(String file) {
    return streamOfUIO.eval(UIO.task(() -> reader(file)))
        .flatMap(reader -> streamOfUIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toUIO())
        .recoverWith(UncheckedIOException.class, cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileTask(String file) {
    return streamOfTask.eval(Task.task(() -> reader(file)))
        .flatMap(reader -> streamOfTask.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toTask())
        .recover(cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileEIO(String file) {
    return streamOfEIO.eval(EIO.task(() -> reader(file)))
        .flatMap(reader -> streamOfEIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(toEIO())
        .recover(cons("--- file not found ---"));
  }

  private URIO<Nothing, String> pureReadFilePureIO(String file) {
    return streamOfPureIO.eval(PureIO.<Nothing, BufferedReader>task(() -> reader(file)))
      .flatMap(reader -> streamOfPureIO.iterate(() -> Option.of(() -> readLine(reader))))
      .takeWhile(Option::isPresent)
      .map(Option::getOrElseThrow)
      .foldLeft("", (a, b) -> a + '\n' + b)
      .fix(toPureIO())
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
