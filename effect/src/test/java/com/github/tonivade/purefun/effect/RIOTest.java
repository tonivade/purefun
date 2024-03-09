/*
 * Copyright (c) 2018-2024, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.effect.RIO.pure;
import static com.github.tonivade.purefun.effect.RIO.raiseError;
import static com.github.tonivade.purefun.effect.RIO.task;
import static com.github.tonivade.purefun.effect.RIOOf.toRIO;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.github.tonivade.purefun.core.Consumer1;
import com.github.tonivade.purefun.core.Function1;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.RIOInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.For;

@ExtendWith(MockitoExtension.class)
public class RIOTest {

  @Captor
  private ArgumentCaptor<Try<Integer>> captor;

  @Test
  public void mapRight() {
    Try<Integer> result = parseInt("1").map(x -> x + 1).safeRunSync(null);

    assertEquals(Try.success(2), result);
  }

  @Test
  public void mapLeft() {
    RIO<Void, Integer> result = parseInt("lskjdf").map(x -> x + 1);

    assertEquals(NumberFormatException.class, result.safeRunSync(null).getCause().getClass());
  }

  @Test
  public void flatMapRight() {
    Try<Integer> result = parseInt("1").flatMap(x -> pure(x + 1)).safeRunSync(null);

    assertEquals(Try.success(2), result);
  }

  @Test
  public void flatMapLeft() {
    RIO<Void, Integer> result = parseInt("kjere").flatMap(x -> pure(x + 1));

    assertEquals(NumberFormatException.class, result.safeRunSync(null).getCause().getClass());
  }

  @Test
  public void redeemRight() {
    Try<Integer> result = parseInt("1").recover(e -> -1).safeRunSync(null);

    assertEquals(Try.success(1), result);
  }

  @Test
  public void redeemLeft() {
    Try<Integer> result = parseInt("kjsdfdf").recover(e -> -1).safeRunSync(null);

    assertEquals(Try.success(-1), result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    RIO<Void, String> bracket = RIO.bracket(open(resultSet), getString("id"));

    assertEquals(Try.success("value"), bracket.safeRunSync(null));
    verify(resultSet).close();
  }

  @Test
  public void bracketError() {
    RIO<Void, String> bracket = RIO.bracket(openError(), getString("id"));

    assertEquals(SQLException.class, bracket.safeRunSync(null).getCause().getClass());
  }

  @Test
  public void asyncRight(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("1").safeRunAsync(null, callback);

    verify(callback, timeout(1000)).accept(Try.success(1));
  }

  @Test
  public void asyncLeft(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("kjsdf").safeRunAsync(null, callback);

    verify(callback, timeout(100)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().getCause().getClass());
  }

  @Test
  public void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> retry = task(computation).retry().safeRunSync(null);

    assertTrue(retry.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  public void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> repeat = task(computation).repeat().safeRunSync(null);

    assertEquals("hola", repeat.getOrElseThrow());
    verify(computation, times(2)).get();
  }

  @Test
  public void testCompositionWithZIO() {
    PureIO<Environment, Throwable, Integer> getValue = PureIO.access(Environment::getValue);
    PureIO<Environment, Throwable, Unit> zio = RIO.<Environment>unit().toPureIO();
    PureIO<Environment, Throwable, Integer> result = zio.andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }

  @Test
  public void traverse() {
    RIO<Void, String> left = task(() -> "left");
    RIO<Void, String> right = task(() -> "right");

    RIO<Void, Sequence<String>> traverse = RIO.traverse(listOf(left, right));

    assertEquals(Try.success(listOf("left", "right")), traverse.safeRunSync(null));
  }

  @Test
  void raceA() {
    RIO<Void, Either<Integer, String>> race = RIO.race(
        RIO.<Void>sleep(Duration.ofMillis(10)).map(x -> 10),
        RIO.<Void>sleep(Duration.ofMillis(100)).map(x -> "b"));

    Try<Either<Integer, String>> orElseThrow = race.safeRunSync(null);

    assertEquals(Try.success(Either.left(10)), orElseThrow);
  }

  @Test
  void raceB() {
    RIO<Void, Either<Integer, String>> race = RIO.race(
        RIO.<Void>sleep(Duration.ofMillis(100)).map(x -> 10),
        RIO.<Void>sleep(Duration.ofMillis(10)).map(x -> "b"));

    Try<Either<Integer, String>> orElseThrow = race.safeRunSync(null);

    assertEquals(Try.success(Either.right("b")), orElseThrow);
  }

  @Test
  void fork() {
    RIO<Void, String> result = For.with(RIOInstances.<Void>monad())
      .then(RIO.<Void, String>pure("hola"))
      .flatMap(hello -> {
        RIO<Void, Unit> sleep = RIO.sleep(Duration.ofSeconds(1));
        RIO<Void, String> task = RIO.task(() -> hello + " toni");
        return sleep.andThen(task).fork();
      })
      .flatMap(Fiber::join).fix(toRIO());

    Try<String> orElseThrow = result.safeRunSync(null);

    assertEquals(Try.success("hola toni"), orElseThrow);
  }

  @Test
  void timeoutFail() {
    Try<Unit> safeRunSync = RIO.<Void, Unit>never().timeout(Duration.ofSeconds(1)).safeRunSync(null);

    assertTrue(safeRunSync.getCause() instanceof TimeoutException);
  }

  @Test
  void timeoutSuccess() {
    assertEquals(Try.success(1), RIO.pure(1).timeout(Duration.ofSeconds(1)).safeRunSync(null));
  }

  private RIO<Void, Integer> parseInt(String string) {
    return task(() -> Integer.parseInt(string));
  }

  private RIO<Void, ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private RIO<Void, ResultSet> openError() {
    return raiseError(new SQLException("error"));
  }

  private Function1<ResultSet, RIO<Void, String>> getString(String column) {
    return resultSet -> task(() -> resultSet.getString(column));
  }
}
