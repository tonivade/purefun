/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.core.Function1.identity;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.For;
import com.github.tonivade.purefun.typeclasses.Instances;

@ExtendWith(MockitoExtension.class)
public class PureIOTest {

  @Test
  public void recover() {
    PureIO<Void, Void, String> ups = PureIO.fromEither(() -> {
      throw new RuntimeException("ups!");
    });

    assertEquals("ups!", PureIO.redeem(ups).provide(null).getLeft().getMessage());
  }

  @Test
  public void accessM() {
    PureIO<String, Void, String> access = PureIO.<String, Void, String>access(a -> a.toUpperCase());

    assertEquals(Either.right("HELLO WORLD"), access.provide("hello world"));
  }

  @Test
  public void pure() {
    Either<Void, String> result = PureIO.<Void, Void, String>pure("hello world").provide(null);

    assertEquals(Either.right("hello world"), result);
  }

  @Test
  public void failure() {
    Either<String, Integer> result = PureIO.<Void, String, Integer>raiseError("error").provide(null);

    assertEquals(Either.left("error"), result);
  }

  @Test
  public void swapLeft() {
    Either<Integer, String> result = PureIO.<Void, String, Integer>raiseError("error").swap().provide(null);

    assertEquals(Either.right("error"), result);
  }

  @Test
  public void swapRight() {
    Either<String, Integer> result = PureIO.<Void, Integer, String>pure("value").swap().provide(null);

    assertEquals(Either.left("value"), result);
  }

  @Test
  public void task() {
    Either<Throwable, String> result = PureIO.<Void, String>task(() -> "hello world").provide(null);

    assertEquals(Either.right("hello world"), result);
  }

  @Test
  public void laterRight() {
    Either<Throwable, String> result = PureIO.<Void, Throwable, String>fromEither(() -> Either.right("hello world")).provide(null);

    assertEquals(Either.right("hello world"), result);
  }

  @Test
  public void laterLeft() {
    Either<String, Throwable> result = PureIO.<Void, String, Throwable>fromEither(() -> Either.left("hello world")).provide(null);

    assertEquals(Either.left("hello world"), result);
  }

  @Test
  public void deferRight() {
    Either<Throwable, String> result = PureIO.<Void, Throwable, String>defer(() -> PureIO.pure("hello world")).provide(null);

    assertEquals(Either.right("hello world"), result);
  }

  @Test
  public void deferLeft() {
    Either<String, Throwable> result = PureIO.<Void, String, Throwable>defer(() -> PureIO.raiseError("hello world")).provide(null);

    assertEquals(Either.left("hello world"), result);
  }

  @Test
  public void mapRight() {
    Either<Throwable, Integer> result =
        parseInt("1").map(x -> x + 1).provide(null);

    assertEquals(Either.right(2), result);
  }

  @Test
  public void mapLeft() {
    Either<Throwable, Integer> result =
        parseInt("lskjdf").map(x -> x + 1).provide(null);

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void mapError() {
    Either<String, Integer> result =
        parseInt("lskjdf").mapError(Throwable::getMessage).provide(null);

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void flatMapRight() {
    Either<Throwable, Integer> result =
        parseInt("1").flatMap(x -> PureIO.pure(x + 1)).provide(null);

    assertEquals(Either.right(2), result);
  }

  @Test
  public void flatMapLeft() {
    Either<Throwable, Integer> result =
        parseInt("lskjdf").flatMap(x -> PureIO.pure(x + 1)).provide(null);

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void flatMapError() {
    Either<String, Integer> result =
        parseInt("lskjdf").flatMapError(e -> PureIO.raiseError(e.getMessage())).provide(null);

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void bimapRight() {
    Either<String, Integer> result =
        parseInt("1").bimap(Throwable::getMessage, x -> x + 1).provide(null);

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bimapLeft() {
    Either<String, Integer> result =
        parseInt("lskjdf").bimap(Throwable::getMessage, x -> x + 1).provide(null);

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void foldRight() {
    Integer result =
        parseInt("1").fold(e -> -1, identity()).unsafeRunSync(null);

    assertEquals(1, result);
  }

  @Test
  public void foldLeft() {
    Integer result =
        parseInt("kjsdfdf").fold(e -> -1, identity()).unsafeRunSync(null);

    assertEquals(-1, result);
  }

  @Test
  public void orElseRight() {
    Either<Throwable, Integer> result =
        parseInt("1").orElse(PureIO.pure(2)).provide(null);

    assertEquals(Either.right(1), result);
  }

  @Test
  public void orElseLeft() {
    Either<Throwable, Integer> result =
        parseInt("kjsdfe").orElse(PureIO.pure(2)).provide(null);

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bracket(@Mock ResultSet resultSet) throws SQLException {
    when(resultSet.getString("id")).thenReturn("value");

    PureIO<Void, Throwable, String> bracket = PureIO.bracket(open(resultSet), getString("id"));

    assertEquals(Either.right("value"), bracket.provide(null));
    verify(resultSet).close();
  }

  @Test
  public void asyncSuccess() {
    PureIO<Void, Throwable, String> async = PureIO.async((env, callback) -> {
      Thread.sleep(100);
      callback.accept(Try.success(Either.right("1")));
    });

    Either<Throwable, String> result = async.provide(null);

    assertEquals("1", result.get());
  }

  @Test
  public void asyncFailure() {
    PureIO<Void, Throwable, String> async = PureIO.async((env, callback) -> {
      Thread.sleep(100);
      callback.accept(Try.success(Either.left(new UnsupportedOperationException())));
    });

    Either<Throwable, String> result = async.provide(null);

    assertTrue(result.getLeft() instanceof UnsupportedOperationException);
  }

  @Test
  public void safeRunAsync() {
    Ref<ImmutableList<String>> ref = Ref.of(ImmutableList.empty());
    UIO<ImmutableList<String>> currentThread =
        ref.updateAndGet(list -> list.append("thread-" + Thread.currentThread().threadId()));

    UIO<ImmutableList<String>> program = currentThread
        .andThen(currentThread
            .andThen(currentThread
                .andThen(currentThread
                    .andThen(currentThread))));

    ImmutableList<String> result = program.runAsync().await().getOrElseThrow();

    assertEquals(5, result.size());
  }

  @Test
  public void absorb() {
    Exception error = new Exception();
    PureIO<Void, Throwable, Either<Throwable, Integer>> task = PureIO.pure(Either.left(error));

    Either<Throwable, Integer> result = PureIO.absorb(task).provide(null);

    assertEquals(error, result.getLeft());
  }

  @Test
  public void retryError(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    when(computation.get()).thenReturn(Either.left(new UnsupportedOperationException()));

    Either<Throwable, String> provide = PureIO.fromEither(computation).retry(Duration.ofMillis(100), 3).provide(null);

    assertTrue(provide.isLeft());
    verify(computation, times(4)).get();
  }

  @Test
  public void retrySuccess(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    Mockito.<Either<Throwable, ? extends String>>when(computation.get())
        .thenReturn(Either.left(new UnsupportedOperationException()))
        .thenReturn(Either.left(new UnsupportedOperationException()))
        .thenReturn(Either.left(new UnsupportedOperationException()))
        .thenReturn(Either.right("OK"));

    Either<Throwable, String> provide = PureIO.fromEither(computation).retry(Duration.ofMillis(100), 3).provide(null);

    assertEquals("OK", provide.get());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatSuccess(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    Mockito.<Either<Throwable, ? extends String>>when(computation.get()).thenReturn(Either.right("hola"));

    Either<Throwable, String> provide = PureIO.fromEither(computation).repeat(Duration.ofMillis(100), 3).provide(null);

    assertEquals("hola", provide.get());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatStackSafe(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    Mockito.<Either<Throwable, ? extends String>>when(computation.get()).thenReturn(Either.right("hola"));

    Either<Throwable, String> provide = PureIO.fromEither(computation).repeat(10000).provide(null);

    assertEquals("hola", provide.get());
    verify(computation, times(10001)).get();
  }

  @Test
  public void repeatFailure(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    Mockito.<Either<Throwable, ? extends String>>when(computation.get())
        .thenReturn(Either.right("hola"))
        .thenReturn(Either.right("hola"))
        .thenReturn(Either.right("hola"))
        .thenReturn(Either.left(new UnsupportedOperationException()));

    Either<Throwable, String> provide = PureIO.fromEither(computation).repeat(Duration.ofMillis(100), 3).provide(null);

    assertTrue(provide.isLeft());
    verify(computation, times(4)).get();
  }

  @Test
  public void timed() {
    PureIO<Void, Throwable, Tuple2<Duration, Unit>> timed = PureIO.<Void, Throwable>sleep(Duration.ofMillis(100)).timed();

    Either<Throwable, Tuple2<Duration, Unit>> provide = timed.provide(null);

    assertTrue(provide.getRight().get1().toMillis() >= 100);
  }

  @Test
  public void flatMapped() {
    UIO<String> uio = UIO.unit()
        .map(ignore -> "hola")
        .map(ignore -> "hola")
        .map(ignore -> "hola")
        .map(ignore -> "adios");

    assertEquals("adios", uio.unsafeRunSync());
  }

  @Test
  public void stackSafety() {
    UIO<Integer> sum = sum(100000, 0);

    Future<Integer> futureSum = sum.runAsync();

    assertEquals(705082704, sum.unsafeRunSync());
    assertEquals(Try.success(705082704), futureSum.await());
  }

  @Test
  public void refineOrDie() {
    PureIO<Void, Throwable, String> error = PureIO.raiseError(new IOException());

    PureIO<Void, IOException, String> refine = error.refineOrDie(IOException.class);
    PureIO<Void, UnsupportedOperationException, String> die = error.refineOrDie(UnsupportedOperationException.class);

    assertEquals(IOException.class, refine.provide(null).getLeft().getClass());
    assertThrows(ClassCastException.class, () -> die.provide(null));
  }

  @Test
  public void toURIO() {
    PureIO<Void, Integer, String> unsupported = PureIO.raiseError(3);
    PureIO<Void, Throwable, String> error = PureIO.raiseError(new IOException());
    PureIO<Void, Throwable, String> success = PureIO.pure("hola");

    assertEquals("hola", success.toURIO().unsafeRunSync(null));
    assertThrows(IOException.class, () -> error.toURIO().unsafeRunSync(null));
    assertThrows(ClassCastException.class, () -> unsupported.toURIO().unsafeRunSync(null));
  }

  @Test
  public void toRIO() {
    PureIO<Void, Integer, String> unsupported = PureIO.raiseError(3);
    IOException exception = new IOException();
    PureIO<Void, Throwable, String> error = PureIO.raiseError(exception);
    PureIO<Void, Throwable, String> success = PureIO.pure("hola");

    assertEquals(Try.success("hola"), success.toRIO().safeRunSync(null));
    assertEquals(Try.failure(exception), error.toRIO().safeRunSync(null));
    assertThrows(ClassCastException.class, () -> unsupported.toRIO().safeRunSync(null));
  }

  @Test
  public void traverse() {
    PureIO<Void, Throwable, String> left = PureIO.task(() -> "left");
    PureIO<Void, Throwable, String> right = PureIO.task(() -> "right");

    PureIO<Void, Throwable, Sequence<String>> traverse = PureIO.traverse(listOf(left, right));

    assertEquals(Either.right(listOf("left", "right")), traverse.provide(null));
  }

  @Test
  public void raceA() {
    PureIO<Void, Void, Either<Integer, String>> race = PureIO.race(
        PureIO.<Void, Void>sleep(Duration.ofMillis(10)).map(x -> 10),
        PureIO.<Void, Void>sleep(Duration.ofMillis(100)).map(x -> "b"));

    Either<Integer, String> orElseThrow = race.provide(null).get();

    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    PureIO<Void, Void, Either<Integer, String>> race = PureIO.race(
        PureIO.<Void, Void>sleep(Duration.ofMillis(100)).map(x -> 10),
        PureIO.<Void, Void>sleep(Duration.ofMillis(10)).map(x -> "b"));

    Either<Integer, String> orElseThrow = race.provide(null).get();

    assertEquals(Either.right("b"), orElseThrow);
  }

  @Test
  public void fork() {
    PureIO<Void, Throwable, String> result = For.with(Instances.<PureIO<Void, Throwable, ?>>monad())
      .then(PureIO.pure("hola"))
      .flatMap(hello -> {
        PureIO<Void, Throwable, Unit> sleep = PureIO.sleep(Duration.ofSeconds(1));
        PureIO<Void, Throwable, String> task = PureIO.task(() -> hello + " toni");
        return sleep.andThen(task).fork();
      })
      .flatMap(Fiber::join).fix(PureIOOf::toPureIO);

    Either<Throwable, String> orElseThrow = result.runAsync(null).getOrElseThrow();

    assertEquals(Either.right("hola toni"), orElseThrow);
  }

  @Test
  public void timeoutFail() {
    assertThrows(TimeoutException.class, () -> PureIO.never().timeout(Duration.ofSeconds(1)).provide(null));
  }

  @Test
  public void timeoutSuccess() {
    assertEquals(Either.right(1), PureIO.pure(1).timeout(Duration.ofSeconds(1)).provide(null));
  }

  private PureIO<Void, Throwable, Integer> parseInt(String string) {
    return PureIO.task(() -> Integer.parseInt(string));
  }

  private PureIO<Void, Throwable, ResultSet> open(ResultSet resultSet) {
    return PureIO.pure(resultSet);
  }

  private Function1<ResultSet, PureIO<Void, Throwable, String>> getString(String column) {
    return resultSet -> PureIO.task(() -> resultSet.getString(column));
  }

  private UIO<Integer> sum(Integer n, Integer sum) {
    if (n == 0) {
      return UIO.pure(sum);
    }
    return UIO.defer(() -> sum(n - 1, sum + n));
  }
}
