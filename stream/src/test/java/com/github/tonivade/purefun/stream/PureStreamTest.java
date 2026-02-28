/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.stream;

import static com.github.tonivade.purefun.core.Function1.cons;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static java.util.Objects.nonNull;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.core.PartialFunction1;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.effect.EIO;
import com.github.tonivade.purefun.effect.EIOOf;
import com.github.tonivade.purefun.effect.PureIO;
import com.github.tonivade.purefun.effect.PureIOOf;
import com.github.tonivade.purefun.effect.Task;
import com.github.tonivade.purefun.effect.TaskOf;
import com.github.tonivade.purefun.effect.UIO;
import com.github.tonivade.purefun.effect.UIOOf;
import com.github.tonivade.purefun.effect.URIO;
import com.github.tonivade.purefun.instances.PureStreamInstances;
import com.github.tonivade.purefun.monad.IO;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.stream.PureStream.Of;
import com.github.tonivade.purefun.type.Option;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class PureStreamTest {

  private final Of<IO<?>> streamOfIO = PureStreamInstances.ofIO();
  private final Of<UIO<?>> streamOfUIO = PureStreamInstances.ofUIO();
  private final Of<Task<?>> streamOfTask = PureStreamInstances.ofTask();
  private final Of<EIO<Throwable, ?>> streamOfEIO = PureStreamInstances.ofEIO();
  private final Of<PureIO<Void, Throwable, ?>> streamOfPureIO = PureStreamInstances.ofPureIO();

  @Test
  public void map() {
    PureStream<IO<?>, String> pure1 = PureStream.pure("hola");
    PureStream<IO<?>, String> pure2 = PureStream.pure(" mundo");

    PureStream<IO<?>, String> result = pure1.concat(pure2).map(String::toUpperCase);

    IO<String> foldRight = result.foldRight(IO.pure(""), (a, b) -> b.fix(IOOf::toIO).map(x -> x + a))
        .fix(IOOf::toIO);

    assertEquals("HOLA MUNDO", foldRight.unsafeRunSync());
  }

  @Test
  public void flatMap() {
    PureStream<IO<?>, String> pure1 = PureStream.pure("hola");
    PureStream<IO<?>, String> pure2 = PureStream.pure(" mundo");

    PureStream<IO<?>, String> result = pure1.concat(pure2).flatMap(string -> PureStream.pure(string.toUpperCase()));

    IO<String> foldLeft = result.asString().fix(IOOf::toIO);

    assertEquals("HOLA MUNDO", foldLeft.unsafeRunSync());
  }

  @Test
  public void mapEval() {
    PureStream<IO<?>, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO<?>, Integer> result = stream.mapEval(i -> IO.task(() -> i * 2));

    assertEquals(Integer.valueOf(12), run(result.foldLeft(0, Integer::sum)));
  }

  @Test
  public void append() {
    PureStream<IO<?>, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO<?>, Integer> result = stream.append(IO.pure(4));

    assertEquals(listOf(1, 2, 3, 4), run(result.asSequence()));
  }

  @Test
  public void prepend() {
    PureStream<IO<?>, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO<?>, Integer> result = stream.prepend(IO.pure(0));

    assertEquals(listOf(0, 1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void take() {
    PureStream<IO<?>, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO<?>, Integer> result = stream.take(2);

    assertEquals(listOf(1, 2), run(result.asSequence()));
  }

  @Test
  public void drop() {
    PureStream<IO<?>, Integer> stream = PureStream.from(listOf(1, 2, 3));

    PureStream<IO<?>, Integer> result = stream.drop(2);

    assertEquals(listOf(3), run(result.asSequence()));
  }

  @Test
  public void takeWhile() {
    PureStream<IO<?>, Integer> stream = PureStream.from(listOf(1, 2, 3, 4, 5));

    PureStream<IO<?>, Integer> result = stream.takeWhile(t -> t < 4);

    assertEquals(listOf(1, 2, 3), run(result.asSequence()));
  }

  @Test
  public void dropWhile() {
    PureStream<IO<?>, Integer> stream = PureStream.from(listOf(1, 2, 3, 4, 5));

    PureStream<IO<?>, Integer> result = stream.dropWhile(t -> t < 4);

    assertEquals(listOf(4, 5), run(result.asSequence()));
  }

  @Test
  public void filter() {
    PureStream<IO<?>, Integer> stream = PureStream.from(java.util.stream.Stream.of(1, 2, 3, 4, 5));

    PureStream<IO<?>, Integer> result = stream.filter(t -> (t % 2) == 0);

    assertEquals(listOf(2, 4), run(result.asSequence()));
  }

  @Test
  public void collect() {
    PureStream<IO<?>, Integer> stream = PureStream.<IO<?>>of().of(1, 2, 3, 4, 5);

    PureStream<IO<?>, Integer> result = stream.collect(PartialFunction1.of(t -> (t % 2) == 0, x -> x * 2));

    assertEquals(listOf(4, 8), run(result.asSequence()));
  }

  @Test
  public void iterate() {
    PureStream<IO<?>, Integer> stream = PureStream.iterate(0, i -> i + 1);

    PureStream<IO<?>, Integer> result = stream.takeWhile(t -> t < 4).dropWhile(t -> t < 2);

    assertEquals(listOf(2, 3), run(result.asSequence()));
  }

  @Test
  public void repeat() {
    PureStream<IO<?>, Integer> stream = PureStream.<IO<?>>of().of(1, 2, 3);

    PureStream<IO<?>, Integer> result = stream.repeat().take(7);

    assertEquals(listOf(1, 2, 3, 1, 2, 3, 1), run(result.asSequence()));
  }

  @Test
  public void intersperse() {
    PureStream<IO<?>, Integer> stream = PureStream.<IO<?>>of().of(1, 2);

    PureStream<IO<?>, Integer> result = stream.intersperse(IO.pure(0));

    assertEquals(listOf(1, 0, 2, 0), run(result.asSequence()));
  }

  @Test
  public void zip() {
    PureStream<IO<?>, String> stream = PureStream.from(listOf("a", "b", "c"));

    IO<Sequence<Tuple2<String, Integer>>> zip = PureStream.zipWithIndex(stream).asSequence().fix(IOOf::toIO);

    assertEquals(listOf(Tuple2.of("a", 0), Tuple2.of("b", 1), Tuple2.of("c", 2)), zip.unsafeRunSync());
  }

  @Test
  public void merge() {
    PureStream<IO<?>, Integer> stream1 = PureStream.<IO<?>>of().of(1, 2, 3);
    PureStream<IO<?>, Integer> stream2 = PureStream.<IO<?>>of().of(4, 5, 6, 7);

    PureStream<IO<?>, Integer> merge = PureStream.merge(stream1, stream2);

    assertEquals(listOf(1, 4, 2, 5, 3, 6), run(merge.asSequence()));
  }

  @Test
  public void forAll() {
    PureStream<IO<?>, String> stream = PureStream.from(listOf("a", "b", "c"));

    Kind<IO<?>, Boolean> all = stream.exists(x -> x.toLowerCase().equals(x));
    Kind<IO<?>, Boolean> notAll = stream.exists(x -> x.toUpperCase().equals(x));

    assertTrue(run(all));
    assertFalse(run(notAll));
  }

  @Test
  public void exists() {
    PureStream<IO<?>, String> stream = PureStream.from(listOf("a", "b", "c"));

    Kind<IO<?>, Boolean> exists = stream.exists(x -> "c".equals(x));
    Kind<IO<?>, Boolean> notExists = stream.exists(x -> "z".equals(x));

    assertTrue(run(exists));
    assertFalse(run(notExists));
  }

  @Test
  public void foldLeftLazyness() {
    IO<String> fail = IO.raiseError(new IllegalAccessException());

    IO<String> result = PureStream.eval(fail).asString().fix(IOOf::toIO);

    assertThrows(IllegalAccessException.class, result::unsafeRunSync);
  }

  @Test
  public void foldRightLazyness() {
    IO<String> fail = IO.raiseError(new IllegalAccessException());

    IO<String> result = PureStream.eval(fail)
      .foldRight(IO.pure(""), (a, b) -> b.fix(IOOf::toIO).map(x -> a + x))
      .fix(IOOf::toIO);

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
    URIO<Void, String> license = pureReadFilePureIO("../LICENSE");
    URIO<Void, String> notFound = pureReadFilePureIO("hjsjkdf");
    assertAll(
        () -> assertEquals(impureReadFile("../LICENSE"), license.unsafeRunSync(null)),
        () -> assertEquals("--- file not found ---", notFound.unsafeRunSync(null)));
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
    PureStream<IO<?>, Integer> stream = streamOfIO.from(listOf("a", "b", "c")).mapReplace(IO.pure(1));

    assertEquals("111", stream.asString().fix(IOOf::toIO).unsafeRunSync());
  }

  private IO<String> pureReadFileIO(String file) {
    return streamOfIO.eval(IO.task(() -> reader(file)))
        .flatMap(reader -> streamOfIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(IOOf::<String>toIO)
        .recover(UncheckedIOException.class, cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileUIO(String file) {
    return streamOfUIO.eval(UIO.task(() -> reader(file)))
        .flatMap(reader -> streamOfUIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(UIOOf::<String>toUIO)
        .recoverWith(UncheckedIOException.class, cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileTask(String file) {
    return streamOfTask.eval(Task.task(() -> reader(file)))
        .flatMap(reader -> streamOfTask.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(TaskOf::<String>toTask)
        .recover(cons("--- file not found ---"));
  }

  private UIO<String> pureReadFileEIO(String file) {
    return streamOfEIO.eval(EIO.task(() -> reader(file)))
        .flatMap(reader -> streamOfEIO.iterate(() -> Option.of(() -> readLine(reader))))
        .takeWhile(Option::isPresent)
        .map(Option::getOrElseThrow)
        .foldLeft("", (a, b) -> a + '\n' + b)
        .fix(EIOOf::<Throwable, String>toEIO)
        .recover(cons("--- file not found ---"));
  }

  private URIO<Void, String> pureReadFilePureIO(String file) {
    return streamOfPureIO.eval(PureIO.<Void, BufferedReader>task(() -> reader(file)))
      .flatMap(reader -> streamOfPureIO.iterate(() -> Option.of(() -> readLine(reader))))
      .takeWhile(Option::isPresent)
      .map(Option::getOrElseThrow)
      .foldLeft("", (a, b) -> a + '\n' + b)
      .fix(PureIOOf::<Void, Throwable, String>toPureIO)
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

  private static <T> T run(Kind<IO<?>, T> effect) {
    return effect.fix(IOOf::toIO).unsafeRunSync();
  }
}
