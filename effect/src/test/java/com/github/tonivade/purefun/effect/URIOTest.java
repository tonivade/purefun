/*
 * Copyright (c) 2018-2025, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.effect.URIO.never;
import static com.github.tonivade.purefun.effect.URIO.pure;
import static com.github.tonivade.purefun.effect.URIO.raiseError;
import static com.github.tonivade.purefun.effect.URIO.sleep;
import static com.github.tonivade.purefun.effect.URIO.task;
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
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.For;
import com.github.tonivade.purefun.typeclasses.Instances;

@ExtendWith(MockitoExtension.class)
public class URIOTest {

  @Captor
  private ArgumentCaptor<Try<Integer>> captor;

  @Test
  public void mapRight() {
    Integer result = parseInt("1").map(x -> x + 1).unsafeRunSync(null);

    assertEquals(2, result);
  }

  @Test
  public void mapLeft() {
    URIO<Void, Integer> result = parseInt("lskjdf").map(x -> x + 1);

    assertThrows(NumberFormatException.class, () -> result.unsafeRunSync(null));
  }

  @Test
  public void flatMapRight() {
    Integer result = parseInt("1").flatMap(x -> pure(x + 1)).unsafeRunSync(null);

    assertEquals(2, result);
  }

  @Test
  public void flatMapLeft() {
    URIO<Void, Integer> result = parseInt("kjere").flatMap(x -> pure(x + 1));

    assertThrows(NumberFormatException.class, () -> result.unsafeRunSync(null));
  }

  @Test
  public void redeemRight() {
    Integer result = parseInt("1").recover(e -> -1).unsafeRunSync(null);

    assertEquals(1, result);
  }

  @Test
  public void redeemLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).unsafeRunSync(null);

    assertEquals(-1, result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    URIO<Void, String> bracket = URIO.bracket(open(resultSet), getString("id"));

    assertEquals("value", bracket.unsafeRunSync(null));
    verify(resultSet).close();
  }

  @Test
  public void bracketError() {
    URIO<Void, String> bracket = URIO.bracket(openError(), getString("id"));

    assertThrows(SQLException.class, () -> bracket.unsafeRunSync(null));
  }

  @Test
  public void asyncRight(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("1").safeRunAsync(null, callback);

    verify(callback, timeout(1000)).accept(Try.success(1));
  }

  @Test
  public void asyncLeft(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("kjsdf").safeRunAsync(null, callback);

    verify(callback, timeout(1000)).accept(captor.capture());

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
    PureIO<Environment, Void, Integer> getValue = PureIO.access(Environment::getValue);
    PureIO<Environment, Void, Integer> result = URIO.<Environment>unit().<Void>toPureIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }

  @Test
  public void traverse() {
    URIO<Void, String> left = task(() -> "left");
    URIO<Void, String> right = task(() -> "right");

    URIO<Void, Sequence<String>> traverse = URIO.traverse(listOf(left, right));

    assertEquals(listOf("left", "right"), traverse.unsafeRunSync(null));
  }

  @Test
  public void raceA() {
    URIO<Void, Either<Integer, String>> race = URIO.race(
        URIO.<Void>sleep(Duration.ofMillis(10)).map(x -> 10),
        URIO.<Void>sleep(Duration.ofMillis(100)).map(x -> "b"));

    Either<Integer, String> orElseThrow = race.unsafeRunSync(null);

    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    URIO<Void, Either<Integer, String>> race = URIO.race(
        URIO.<Void>sleep(Duration.ofMillis(100)).map(x -> 10),
        URIO.<Void>sleep(Duration.ofMillis(10)).map(x -> "b"));

    Either<Integer, String> orElseThrow = race.unsafeRunSync(null);

    assertEquals(Either.right("b"), orElseThrow);
  }

  @Test
  public void fork() {
    URIO<Void, String> result = For.with(Instances.<URIO<Void, ?>>monad())
      .then(URIO.<Void, String>pure("hola"))
      .flatMap(hello -> {
        URIO<Void, Unit> sleep = sleep(Duration.ofSeconds(1));
        URIO<Void, String> task = task(() -> hello + " toni");
        return sleep.andThen(task).fork();
      })
      .flatMap(Fiber::join).fix(URIOOf::toURIO);

    String orElseThrow = result.unsafeRunSync(null);

    assertEquals("hola toni", orElseThrow);
  }

  @Test
  public void timeoutFail() {
    assertThrows(TimeoutException.class, () -> never().timeout(Duration.ofSeconds(1)).unsafeRunSync(null));
  }

  @Test
  public void timeoutSuccess() {
    assertEquals(1, pure(1).timeout(Duration.ofSeconds(1)).unsafeRunSync(null));
  }

  private URIO<Void, Integer> parseInt(String string) {
    return task(() -> Integer.parseInt(string));
  }

  private URIO<Void, ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private URIO<Void, ResultSet> openError() {
    return raiseError(new SQLException("error"));
  }

  private Function1<ResultSet, URIO<Void, String>> getString(String column) {
    return resultSet -> task(() -> resultSet.getString(column));
  }
}

