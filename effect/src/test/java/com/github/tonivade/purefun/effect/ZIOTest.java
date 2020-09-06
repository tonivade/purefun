/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.concurrent.FutureOf.toFuture;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@ExtendWith(MockitoExtension.class)
public class ZIOTest {

  @Test
  public void mapRight() {
    Either<Throwable, Integer> result =
        parseInt("1").map(x -> x + 1).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void mapLeft() {
    Either<Throwable, Integer> result =
        parseInt("lskjdf").map(x -> x + 1).provide(nothing());

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void mapError() {
    Either<String, Integer> result =
        parseInt("lskjdf").mapError(Throwable::getMessage).provide(nothing());

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void flatMapRight() {
    Either<Throwable, Integer> result =
        parseInt("1").flatMap(x -> ZIO.pure(x + 1)).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void flatMapLeft() {
    Either<Throwable, Integer> result =
        parseInt("lskjdf").flatMap(x -> ZIO.pure(x + 1)).provide(nothing());

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void flatMapError() {
    Either<String, Integer> result =
        parseInt("lskjdf").flatMapError(e -> ZIO.raiseError(e.getMessage())).provide(nothing());

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void bimapRight() {
    Either<String, Integer> result =
        parseInt("1").bimap(Throwable::getMessage, x -> x + 1).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bimapLeft() {
    Either<String, Integer> result =
        parseInt("lskjdf").bimap(Throwable::getMessage, x -> x + 1).provide(nothing());

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void foldRight() {
    Either<Nothing, Integer> result =
        parseInt("1").fold(e -> -1, identity()).provide(nothing());

    assertEquals(Either.right(1), result);
  }

  @Test
  public void foldLeft() {
    Either<Nothing, Integer> result =
        parseInt("kjsdfdf").fold(e -> -1, identity()).provide(nothing());

    assertEquals(Either.right(-1), result);
  }

  @Test
  public void orElseRight() {
    Either<Throwable, Integer> result =
        parseInt("1").orElse(ZIO.pure(2)).provide(nothing());

    assertEquals(Either.right(1), result);
  }

  @Test
  public void orElseLeft() {
    Either<Throwable, Integer> result =
        parseInt("kjsdfe").orElse(ZIO.pure(2)).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bracket(@Mock ResultSet resultSet) throws SQLException {
    when(resultSet.getString("id")).thenReturn("value");

    ZIO<Nothing, Throwable, String> bracket = ZIO.bracket(open(resultSet), getString("id"));

    assertEquals(Either.right("value"), bracket.provide(nothing()));
    verify(resultSet).close();
  }

  @Test
  public void safeRunAsync() {
    Ref<ImmutableList<String>> ref = Ref.of(ImmutableList.empty());
    UIO<ImmutableList<String>> currentThread =
        ref.updateAndGet(list -> list.append(Thread.currentThread().getName()));

    UIO<ImmutableList<String>> program = currentThread
        .andThen(currentThread
            .andThen(currentThread
                .andThen(currentThread
                    .andThen(currentThread))));

    ImmutableList<String> result =
        program.foldMap(FutureInstances.monadDefer())
            .fix(toFuture())
            .await().get();

    assertEquals(5, result.size());
  }

  @Test
  public void absorb() {
    Exception error = new Exception();
    ZIO<Nothing, Throwable, Either<Throwable, Integer>> task = ZIO.pure(Either.left(error));

    Either<Throwable, Integer> result = ZIO.absorb(task).provide(nothing());

    assertEquals(error, result.getLeft());
  }

  @Test
  public void foldMapRight() {
    MonadDefer<IO_> monadDefer = IOInstances.monadDefer();

    Kind<IO_, Integer> future = parseInt("0").foldMap(nothing(), monadDefer);

    assertEquals(0, future.fix(toIO()).unsafeRunSync());
  }

  @Test
  public void foldMapLeft() {
    MonadDefer<IO_> monadDefer = IOInstances.monadDefer();

    Kind<IO_, Integer> future = parseInt("jkdf").foldMap(nothing(), monadDefer);

    assertThrows(NumberFormatException.class, () -> future.fix(toIO()).unsafeRunSync());
  }

  @Test
  public void retryError(@Mock Producer<Either<Throwable, String>> computation) {
    when(computation.get()).thenReturn(Either.left(new UnsupportedOperationException()));

    Either<Throwable, String> provide = ZIO.fromEither(computation).retry(Duration.ofMillis(100), 3).provide(nothing());

    assertTrue(provide.isLeft());
    verify(computation, times(4)).get();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void retrySuccess(@Mock Producer<Either<Throwable, String>> computation) {
    when(computation.get()).thenReturn(
        Either.left(new UnsupportedOperationException()),
        Either.left(new UnsupportedOperationException()),
        Either.left(new UnsupportedOperationException()),
        Either.right("OK"));

    Either<Throwable, String> provide = ZIO.fromEither(computation).retry(Duration.ofMillis(100), 3).provide(nothing());

    assertEquals("OK", provide.get());
    verify(computation, times(4)).get();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void retrySuccessFuture(@Mock Producer<Either<Throwable, String>> computation) {
    when(computation.get()).thenReturn(
        Either.left(new UnsupportedOperationException()),
        Either.left(new UnsupportedOperationException()),
        Either.left(new UnsupportedOperationException()),
        Either.right("OK"));

    Future<String> provide = ZIO.fromEither(computation).retry(Duration.ofMillis(100), 3)
      .foldMap(nothing(), FutureInstances.monadDefer()).fix(toFuture());

    assertEquals("OK", provide.get());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatSuccess(@Mock Producer<Either<Throwable, String>> computation) {
    when(computation.get()).thenReturn(Either.right("hola"));

    Either<Throwable, String> provide = ZIO.fromEither(computation).repeat(Duration.ofMillis(100), 3).provide(nothing());

    assertEquals("hola", provide.get());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatSuccessFuture(@Mock Producer<Either<Throwable, String>> computation) {
    when(computation.get()).thenReturn(Either.right("hola"));

    Future<String> provide = ZIO.fromEither(computation).repeat(Duration.ofMillis(100), 3)
      .foldMap(nothing(), FutureInstances.monadDefer()).fix(toFuture());

    assertEquals("hola", provide.get());
    verify(computation, times(4)).get();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void repeatFailure(@Mock Producer<Either<Throwable, String>> computation) {
    when(computation.get()).thenReturn(
        Either.right("hola"),
        Either.right("hola"),
        Either.right("hola"),
        Either.left(new UnsupportedOperationException()));

    Either<Throwable, String> provide = ZIO.fromEither(computation).repeat(Duration.ofMillis(100), 3).provide(nothing());

    assertTrue(provide.isLeft());
    verify(computation, times(4)).get();
  }
  
  @Test
  public void timed() {
    ZIO<Nothing, Throwable, Tuple2<Duration, Unit>> timed = ZIO.<Nothing>sleep(Duration.ofMillis(100)).timed();
    
    Either<Throwable, Tuple2<Duration, Unit>> provide = timed.provide(nothing());
    
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

    Future<Integer> futureSum = sum.foldMap(FutureInstances.monadDefer()).fix(toFuture());

    assertEquals(705082704, sum.unsafeRunSync());
    assertEquals(Try.success(705082704), futureSum.await());
  }
  
  @Test
  public void refineOrDie() {
    ZIO<Nothing, Throwable, String> error = ZIO.raiseError(new IOException());
    
    ZIO<Nothing, IOException, String> refine = error.refineOrDie(IOException.class);
    ZIO<Nothing, UnsupportedOperationException, String> die = error.refineOrDie(UnsupportedOperationException.class);
    
    assertEquals(IOException.class, refine.provide(nothing()).getLeft().getClass());
    assertThrows(ClassCastException.class, () -> die.provide(nothing()));
  }
  
  @Test
  public void toURIO() {
    ZIO<Nothing, Integer, String> unsupported = ZIO.raiseError(3);
    ZIO<Nothing, Throwable, String> error = ZIO.raiseError(new IOException());
    ZIO<Nothing, Throwable, String> success = ZIO.pure("hola");
    
    assertEquals("hola", success.toURIO().unsafeRunSync(nothing()));
    assertThrows(IOException.class, () -> error.toURIO().unsafeRunSync(nothing()));
    assertThrows(ClassCastException.class, () -> unsupported.toURIO().unsafeRunSync(nothing()));
  }
  
  @Test
  public void toRIO() {
    ZIO<Nothing, Integer, String> unsupported = ZIO.raiseError(3);
    IOException exception = new IOException();
    ZIO<Nothing, Throwable, String> error = ZIO.raiseError(exception);
    ZIO<Nothing, Throwable, String> success = ZIO.pure("hola");
    
    assertEquals(Try.success("hola"), success.toRIO().safeRunSync(nothing()));
    assertEquals(Try.failure(exception), error.toRIO().safeRunSync(nothing()));
    assertThrows(ClassCastException.class, () -> unsupported.toRIO().safeRunSync(nothing()));
  }

  private ZIO<Nothing, Throwable, Integer> parseInt(String string) {
    return ZIO.task(() -> Integer.parseInt(string));
  }

  private ZIO<Nothing, Throwable, ResultSet> open(ResultSet resultSet) {
    return ZIO.pure(resultSet);
  }

  private Function1<ResultSet, ZIO<Nothing, Throwable, String>> getString(String column) {
    return resultSet -> ZIO.task(() -> resultSet.getString(column));
  }

  private UIO<Integer> sum(Integer n, Integer sum) {
    if (n == 0) {
      return UIO.pure(sum);
    }
    return UIO.defer(() -> sum(n - 1, sum + n));
  }
}
