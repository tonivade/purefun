/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.effect.URIO.pure;
import static com.github.tonivade.purefun.effect.URIO.raiseError;
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
import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.URIOInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.For;

@ExtendWith(MockitoExtension.class)
public class URIOTest {

  @Captor
  private ArgumentCaptor<Try<Integer>> captor;

  @Test
  public void mapRight() {
    Integer result = parseInt("1").map(x -> x + 1).unsafeRunSync(nothing());

    assertEquals(2, result);
  }

  @Test
  public void mapLeft() {
    URIO<Nothing, Integer> result = parseInt("lskjdf").map(x -> x + 1);

    assertThrows(NumberFormatException.class, () -> result.unsafeRunSync(nothing()));
  }

  @Test
  public void flatMapRight() {
    Integer result = parseInt("1").flatMap(x -> pure(x + 1)).unsafeRunSync(nothing());

    assertEquals(2, result);
  }

  @Test
  public void flatMapLeft() {
    URIO<Nothing, Integer> result = parseInt("kjere").flatMap(x -> pure(x + 1));

    assertThrows(NumberFormatException.class, () -> result.unsafeRunSync(nothing()));
  }

  @Test
  public void redeemRight() {
    Integer result = parseInt("1").recover(e -> -1).unsafeRunSync(nothing());

    assertEquals(1, result);
  }

  @Test
  public void redeemLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).unsafeRunSync(nothing());

    assertEquals(-1, result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    URIO<Nothing, String> bracket = URIO.bracket(open(resultSet), getString("id"));

    assertEquals("value", bracket.unsafeRunSync(nothing()));
    verify(resultSet).close();
  }

  @Test
  public void bracketError() {
    URIO<Nothing, String> bracket = URIO.bracket(openError(), getString("id"));

    assertThrows(SQLException.class, () -> bracket.unsafeRunSync(nothing()));
  }

  @Test
  public void asyncRight(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("1").safeRunAsync(nothing(), callback);

    verify(callback, timeout(1000)).accept(Try.success(1));
  }

  @Test
  public void asyncLeft(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("kjsdf").safeRunAsync(nothing(), callback);

    verify(callback, timeout(1000)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().getCause().getClass());
  }

  @Test
  public void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> retry = task(computation).retry().safeRunSync(nothing());

    assertTrue(retry.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  public void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> repeat = task(computation).repeat().safeRunSync(nothing());

    assertEquals("hola", repeat.getOrElseThrow());
    verify(computation, times(2)).get();
  }

  @Test
  public void testCompositionWithZIO() {
    PureIO<Environment, Nothing, Integer> getValue = PureIO.access(Environment::getValue);
    PureIO<Environment, Nothing, Integer> result = URIO.<Environment>unit().<Nothing>toPureIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }
  
  @Test
  public void traverse() {
    URIO<Nothing, String> left = task(() -> "left");
    URIO<Nothing, String> right = task(() -> "right");
    
    URIO<Nothing, Sequence<String>> traverse = URIO.traverse(listOf(left, right));
    
    assertEquals(listOf("left", "right"), traverse.unsafeRunSync(nothing()));
  }

  @Test
  public void raceA() {
    URIO<Nothing, Either<Integer, String>> race = URIO.race(
        URIO.<Nothing>sleep(Duration.ofMillis(10)).map(x -> 10),
        URIO.<Nothing>sleep(Duration.ofMillis(100)).map(x -> "b"));
    
    Either<Integer, String> orElseThrow = race.unsafeRunSync(nothing());
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    URIO<Nothing, Either<Integer, String>> race = URIO.race(
        URIO.<Nothing>sleep(Duration.ofMillis(100)).map(x -> 10),
        URIO.<Nothing>sleep(Duration.ofMillis(10)).map(x -> "b"));
    
    Either<Integer, String> orElseThrow = race.unsafeRunSync(nothing());
    
    assertEquals(Either.right("b"), orElseThrow);
  }
  
  @Test
  public void fork() {
    URIO<Nothing, String> result = For.with(URIOInstances.<Nothing>monad())
      .then(URIO.<Nothing, String>pure("hola"))
      .flatMap(hello -> {
        URIO<Nothing, Unit> sleep = URIO.sleep(Duration.ofSeconds(1));
        URIO<Nothing, String> task = URIO.task(() -> hello + " toni");
        return sleep.andThen(task).fork();
      })
      .flatMap(Fiber::join).fix(URIOOf.toURIO());
    
    String orElseThrow = result.unsafeRunSync(nothing());

    assertEquals("hola toni", orElseThrow);
  }
  
  @Test
  public void timeoutFail() {
    assertThrows(TimeoutException.class, () -> URIO.never().timeout(Duration.ofSeconds(1)).unsafeRunSync(nothing()));
  }
  
  @Test
  public void timeoutSuccess() {
    assertEquals(1, URIO.pure(1).timeout(Duration.ofSeconds(1)).unsafeRunSync(nothing()));
  }

  private URIO<Nothing, Integer> parseInt(String string) {
    return task(() -> Integer.parseInt(string));
  }

  private URIO<Nothing, ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private URIO<Nothing, ResultSet> openError() {
    return raiseError(new SQLException("error"));
  }

  private Function1<ResultSet, URIO<Nothing, String>> getString(String column) {
    return resultSet -> task(() -> resultSet.getString(column));
  }
}

