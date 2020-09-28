/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.concurrent.ParOf.toPar;
import static com.github.tonivade.purefun.effect.EIO.pure;
import static com.github.tonivade.purefun.effect.EIO.raiseError;
import static com.github.tonivade.purefun.effect.EIO.task;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.concurrent.Par_;
import com.github.tonivade.purefun.instances.ParInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Async;

@ExtendWith(MockitoExtension.class)
public class EIOTest {

  @Captor
  private ArgumentCaptor<Try<Either<Throwable, Integer>>> captor;

  @Test
  public void mapRight() {
    Either<Throwable, Integer> result = parseInt("1").map(x -> x + 1).safeRunSync();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void mapLeft() {
    Either<Throwable, Integer> result = parseInt("lskjdf").map(x -> x + 1).safeRunSync();

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void mapError() {
    Either<String, Integer> result = parseInt("lskjdf").mapError(Throwable::getMessage).safeRunSync();

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void flatMapRight() {
    Either<Throwable, Integer> result = parseInt("1").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void flatMapLeft() {
    Either<Throwable, Integer> result = parseInt("lskjdf").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void flatMapError() {
    Either<String, Integer> result = parseInt("lskjdf").flatMapError(e -> raiseError(e.getMessage())).safeRunSync();

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void bimapRight() {
    Either<String, Integer> result =
        parseInt("1").bimap(Throwable::getMessage, x -> x + 1).safeRunSync();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bimapLeft() {
    Either<String, Integer> result =
        parseInt("lskjdf").bimap(Throwable::getMessage, x -> x + 1).safeRunSync();

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void foldRight() {
    Integer result = parseInt("1").recover(e -> -1).unsafeRunSync();

    assertEquals(1, result);
  }

  @Test
  public void foldLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).unsafeRunSync();

    assertEquals(-1, result);
  }

  @Test
  public void orElseRight() {
    Either<Throwable, Integer> result = parseInt("1").orElse(pure(2)).safeRunSync();

    assertEquals(Either.right(1), result);
  }

  @Test
  public void orElseLeft() {
    Either<Throwable, Integer> result = parseInt("kjsdfe").orElse(pure(2)).safeRunSync();

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    EIO<Throwable, String> bracket = EIO.bracket(open(resultSet), getString("id"));

    assertEquals(Either.right("value"), bracket.safeRunSync());
    verify(resultSet).close();
  }

  @Test
  public void asyncRight(@Mock Consumer1<? super Try<? extends Either<Throwable, ? extends Integer>>> callback) {
    parseInt("1").safeRunAsync(callback);

    verify(callback, timeout(500)).accept(Try.success(Either.right(1)));
  }

  @Test
  public void asyncLeft(@Mock Consumer1<? super Try<? extends Either<Throwable, ? extends Integer>>> callback) {
    parseInt("kjsdf").safeRunAsync(callback);

    verify(callback, timeout(500)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().get().getLeft().getClass());
  }

  @Test
  public void absorb() {
    Exception error = new Exception();
    EIO<Throwable, Either<Throwable, Integer>> task = pure(Either.left(error));

    Either<Throwable, Integer> result = EIO.absorb(task).safeRunSync();

    assertEquals(error, result.getLeft());
  }

  @Test
  public void foldMapRight() {
    Async<Par_> async = ParInstances.async();

    Kind<Par_, Integer> future = parseInt("0").foldMap(async);

    assertEquals(0, future.fix(toPar()).apply(Future.DEFAULT_EXECUTOR).get());
  }

  @Test
  public void foldMapLeft() {
    Async<Par_> async = ParInstances.async();

    Kind<Par_, Integer> future = parseInt("jkdf").foldMap(async);

    assertThrows(NumberFormatException.class, future.fix(toPar()).apply(Future.DEFAULT_EXECUTOR)::get);
  }

  @Test
  public void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);

    Either<Throwable, String> retry = task(computation).retry().safeRunSync();

    assertTrue(retry.isLeft());
    verify(computation, times(2)).get();
  }

  @Test
  public void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");

    Either<Throwable, String> repeat = task(computation).repeat().safeRunSync();

    assertEquals("hola", repeat.get());
    verify(computation, times(2)).get();
  }

  @Test
  public void testCompositionWithZIO() {
    ZIO<Environment, Throwable, Integer> getValue = ZIO.accessM(env -> ZIO.pure(env.getValue()));
    ZIO<Environment, Throwable, Integer> result = EIO.<Throwable>unit().<Environment>toZIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }

  private EIO<Throwable, Integer> parseInt(String string) {
    return task(() -> Integer.parseInt(string));
  }

  private EIO<Throwable, ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private Function1<ResultSet, EIO<Throwable, String>> getString(String column) {
    return resultSet -> task(() -> resultSet.getString(column));
  }
}
