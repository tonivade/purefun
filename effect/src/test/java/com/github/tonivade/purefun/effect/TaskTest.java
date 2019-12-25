/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.github.tonivade.purefun.effect.Task.pure;
import static com.github.tonivade.purefun.effect.Task.task;
import static com.github.tonivade.purefun.effect.Task.unit;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskTest {

  @Mock
  private Consumer1<Try<Integer>> callback;
  @Captor
  private ArgumentCaptor<Try<Integer>> captor;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  public void mapRight() {
    Try<Integer> result = parseInt("1").map(x -> x + 1).safeRunSync();

    assertEquals(Try.success(2), result);
  }

  @Test
  public void mapLeft() {
    Try<Integer> result = parseInt("lskjdf").map(x -> x + 1).safeRunSync();

    assertEquals(NumberFormatException.class, result.getCause().getClass());
  }

  @Test
  public void flatMapRight() {
    Try<Integer> result = parseInt("1").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(Try.success(2), result);
  }

  @Test
  public void flatMapLeft() {
    Try<Integer> result = parseInt("lskjdf").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(NumberFormatException.class, result.getCause().getClass());
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
    Try<Integer> result = parseInt("1").orElse(() -> pure(2)).safeRunSync();

    assertEquals(Try.success(1), result);
  }

  @Test
  public void orElseLeft() {
    Try<Integer> result = parseInt("kjsdfe").orElse(() -> pure(2)).safeRunSync();

    assertEquals(Try.success(2), result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    Task<String> bracket = Task.bracket(open(resultSet), getString("id"));

    assertEquals(Try.success("value"), bracket.safeRunSync());
    verify(resultSet).close();
  }

  @Test
  public void asyncRight() {
    parseInt("1").async(callback);

    verify(callback, timeout(100)).accept(Try.success(1));
  }

  @Test
  public void asyncLeft() {
    parseInt("kjsdf").async(callback);

    verify(callback, timeout(100)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().getCause().getClass());
  }

  @Test
  public void absorb() {
    Exception error = new Exception();
    Task<Either<Throwable, Integer>> task = pure(Either.left(error));

    Try<Integer> result = Task.absorb(task).safeRunSync();

    assertEquals(error, result.getCause());
  }

  @Test
  public void foldMapRight() {
    MonadDefer<Future.µ> monadDefer = FutureInstances.monadDefer();

    Higher1<Future.µ, Integer> future = parseInt("0").foldMap(monadDefer);

    assertEquals(Try.success(0), future.fix1(Future::narrowK).await());
  }

  @Test
  public void foldMapLeft() {
    MonadDefer<Future.µ> monadDefer = FutureInstances.monadDefer();

    Higher1<Future.µ, Integer> future = parseInt("jdjd").foldMap(monadDefer);

    assertEquals(NumberFormatException.class, future.fix1(Future::narrowK).await().getCause().getClass());
  }

  @Test
  public void testCompositionWithZIO() {
    ZIO<Environment, Throwable, Integer> getValue = ZIO.accessM(env -> ZIO.pure(env.getValue()));
    ZIO<Environment, Throwable, Integer> result = unit().<Environment>toZIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }

  private Task<Integer> parseInt(String string) {
    return task(() -> Integer.parseInt(string));
  }

  private Task<ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private Function1<ResultSet, Task<String>> getString(String column) {
    return resultSet -> task(() -> resultSet.getString(column));
  }
}
