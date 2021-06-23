/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;

@ExtendWith(MockitoExtension.class)
public class ZIOTest {
  
  @Test
  public void recover() {
    ZIO<Nothing, Nothing, String> ups = ZIO.fromEither(() -> {
      throw new RuntimeException("ups!");
    });
    
    assertEquals("ups!", ZIO.redeem(ups).provide(nothing()).getLeft().getMessage());
  }
  
  @Test
  public void accessM() {
    ZIO<String, Nothing, String> access = ZIO.<String, Nothing, String>access(a -> a.toUpperCase());
    
    assertEquals(Either.right("HELLO WORLD"), access.provide("hello world"));
  }
  
  @Test
  public void pure() {
    Either<Nothing, String> result = ZIO.<Nothing, Nothing, String>pure("hello world").provide(nothing());
    
    assertEquals(Either.right("hello world"), result);
  }
  
  @Test
  public void failure() {
    Either<String, Integer> result = ZIO.<Nothing, String, Integer>raiseError("error").provide(nothing());
    
    assertEquals(Either.left("error"), result);
  }
  
  @Test
  public void task() {
    Either<Throwable, String> result = ZIO.<Nothing, String>task(() -> "hello world").provide(nothing());

    assertEquals(Either.right("hello world"), result);
  }
  
  @Test
  public void laterRight() {
    Either<Throwable, String> result = ZIO.<Nothing, Throwable, String>fromEither(() -> Either.right("hello world")).provide(nothing());

    assertEquals(Either.right("hello world"), result);
  }
  
  @Test
  public void laterLeft() {
    Either<String, Throwable> result = ZIO.<Nothing, String, Throwable>fromEither(() -> Either.left("hello world")).provide(nothing());

    assertEquals(Either.left("hello world"), result);
  }
  
  @Test
  public void deferRight() {
    Either<Throwable, String> result = ZIO.<Nothing, Throwable, String>defer(() -> ZIO.pure("hello world")).provide(nothing());

    assertEquals(Either.right("hello world"), result);
  }
  
  @Test
  public void deferLeft() {
    Either<String, Throwable> result = ZIO.<Nothing, String, Throwable>defer(() -> ZIO.raiseError("hello world")).provide(nothing());

    assertEquals(Either.left("hello world"), result);
  }

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
    Integer result =
        parseInt("1").fold(e -> -1, identity()).unsafeRunSync(nothing());

    assertEquals(1, result);
  }

  @Test
  public void foldLeft() {
    Integer result =
        parseInt("kjsdfdf").fold(e -> -1, identity()).unsafeRunSync(nothing());

    assertEquals(-1, result);
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
  @Disabled
  public void bracket(@Mock ResultSet resultSet) throws SQLException {
    when(resultSet.getString("id")).thenReturn("value");

    ZIO<Nothing, Throwable, String> bracket = ZIO.bracket(open(resultSet), getString("id"));

    assertEquals(Either.right("value"), bracket.provide(nothing()));
    verify(resultSet).close();
  }
  
  @Test
  public void asyncSuccess() {
    ZIO<Nothing, Throwable, String> async = ZIO.async((env, callback) -> {
      Thread.sleep(100);
      callback.accept(Either.right("1"));
    });
    
    Either<Throwable, String> result = async.provide(nothing());
    
    assertEquals("1", result.get());
  }
  
  @Test
  public void asyncFailure() {
    ZIO<Nothing, Throwable, String> async = ZIO.async((env, callback) -> {
      Thread.sleep(100);
      callback.accept(Either.left(new UnsupportedOperationException()));
    });
    
    Either<Throwable, String> result = async.provide(nothing());
   
    assertTrue(result.getLeft() instanceof UnsupportedOperationException);
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

    ImmutableList<String> result = program.runAsync().await().get();

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
  public void retryError(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    when(computation.get()).thenReturn(Either.left(new UnsupportedOperationException()));

    Either<Throwable, String> provide = ZIO.fromEither(computation).retry(Duration.ofMillis(100), 3).provide(nothing());

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

    Either<Throwable, String> provide = ZIO.fromEither(computation).retry(Duration.ofMillis(100), 3).provide(nothing());

    assertEquals("OK", provide.get());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatSuccess(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    Mockito.<Either<Throwable, ? extends String>>when(computation.get()).thenReturn(Either.right("hola"));

    Either<Throwable, String> provide = ZIO.fromEither(computation).repeat(Duration.ofMillis(100), 3).provide(nothing());

    assertEquals("hola", provide.get());
    verify(computation, times(4)).get();
  }

  @Test
  public void repeatFailure(@Mock Producer<Either<Throwable, ? extends String>> computation) {
    Mockito.<Either<Throwable, ? extends String>>when(computation.get())
        .thenReturn(Either.right("hola"))
        .thenReturn(Either.right("hola"))
        .thenReturn(Either.right("hola"))
        .thenReturn(Either.left(new UnsupportedOperationException()));

    Either<Throwable, String> provide = ZIO.fromEither(computation).repeat(Duration.ofMillis(100), 3).provide(nothing());

    assertTrue(provide.isLeft());
    verify(computation, times(4)).get();
  }
  
  @Test
  public void timed() {
    ZIO<Nothing, Throwable, Tuple2<Duration, Unit>> timed = ZIO.<Nothing, Throwable>sleep(Duration.ofMillis(100)).timed();
    
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

    Future<Integer> futureSum = sum.runAsync();

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
  
  @Test
  public void traverse() {
    ZIO<Nothing, Throwable, String> left = ZIO.task(() -> "left");
    ZIO<Nothing, Throwable, String> right = ZIO.task(() -> "right");
    
    ZIO<Nothing, Throwable, Sequence<String>> traverse = ZIO.traverse(listOf(left, right));
    
    assertEquals(Either.right(listOf("left", "right")), traverse.provide(nothing()));
  }

  @Test
  public void raceA() {
    ZIO<Nothing, Nothing, Either<Integer, String>> race = ZIO.race(
        ZIO.<Nothing, Nothing>sleep(Duration.ofMillis(10)).map(x -> 10),
        ZIO.<Nothing, Nothing>sleep(Duration.ofMillis(100)).map(x -> "b"));
    
    Either<Integer, String> orElseThrow = race.provide(nothing()).get();
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    ZIO<Nothing, Nothing, Either<Integer, String>> race = ZIO.race(
        ZIO.<Nothing, Nothing>sleep(Duration.ofMillis(100)).map(x -> 10),
        ZIO.<Nothing, Nothing>sleep(Duration.ofMillis(10)).map(x -> "b"));
    
    Either<Integer, String> orElseThrow = race.provide(nothing()).get();
    
    assertEquals(Either.right("b"), orElseThrow);
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
