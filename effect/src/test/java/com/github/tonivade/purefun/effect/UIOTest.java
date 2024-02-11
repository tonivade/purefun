/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.effect.UIO.pure;
import static com.github.tonivade.purefun.effect.UIO.raiseError;
import static com.github.tonivade.purefun.effect.UIO.task;
import static com.github.tonivade.purefun.effect.UIO.unit;
import static com.github.tonivade.purefun.effect.UIOOf.toUIO;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.github.tonivade.purefun.core.Nothing;
import com.github.tonivade.purefun.core.Producer;
import com.github.tonivade.purefun.core.Tuple2;
import com.github.tonivade.purefun.core.Unit;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.UIOInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.For;

@ExtendWith(MockitoExtension.class)
public class UIOTest {

  @Captor
  private ArgumentCaptor<Try<Integer>> captor;

  @Test
  public void mapRight() {
    Integer result = parseInt("1").map(x -> x + 1).unsafeRunSync();

    assertEquals(2, result);
  }

  @Test
  public void mapLeft() {
    UIO<Integer> result = parseInt("lskjdf").map(x -> x + 1);

    assertThrows(NumberFormatException.class, result::unsafeRunSync);
  }

  @Test
  public void flatMapRight() {
    Integer result = parseInt("1").flatMap(x -> pure(x + 1)).unsafeRunSync();

    assertEquals(2, result);
  }

  @Test
  public void flatMapLeft() {
    UIO<Integer> result = parseInt("kjere").flatMap(x -> pure(x + 1));

    assertThrows(NumberFormatException.class, result::unsafeRunSync);
  }

  @Test
  public void redeemRight() {
    Integer result = parseInt("1").recover(e -> -1).unsafeRunSync();

    assertEquals(1, result);
  }

  @Test
  public void redeemLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).unsafeRunSync();

    assertEquals(-1, result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    UIO<String> bracket = UIO.bracket(open(resultSet), getString("id"));

    assertEquals("value", bracket.unsafeRunSync());
    verify(resultSet).close();
  }

  @Test
  public void bracketError() {
    UIO<String> bracket = UIO.bracket(openError(), getString("id"));

    assertThrows(SQLException.class, bracket::unsafeRunSync);
  }

  @Test
  public void asyncRight(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("1").safeRunAsync(callback);

    verify(callback, timeout(1000)).accept(Try.success(1));
  }

  @Test
  public void asyncLeft(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("kjsdf").safeRunAsync(callback);

    verify(callback, timeout(1000)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().getCause().getClass());
  }

  @Test
  public void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> retry = task(computation).retry().safeRunSync();

    assertTrue(retry.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  public void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> repeat = task(computation).repeat().safeRunSync();

    assertEquals("hola", repeat.getOrElseThrow());
    verify(computation, times(2)).get();
  }

  @Test
  public void testCompositionWithZIO() {
    PureIO<Environment, Nothing, Integer> getValue = PureIO.access(Environment::getValue);
    PureIO<Environment, Nothing, Integer> result = unit().<Environment, Nothing>toPureIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }
  
  @Test
  public void timed() {
    UIO<Tuple2<Duration, Unit>> timed = UIO.sleep(Duration.ofMillis(100)).timed();
    
    Tuple2<Duration, Unit> provide = timed.unsafeRunSync();
    
    assertTrue(provide.get1().toMillis() >= 100);
  }
  
  @Test
  public void traverse() {
    UIO<String> left = task(() -> "left");
    UIO<String> right = task(() -> "right");
    
    UIO<Sequence<String>> traverse = UIO.traverse(listOf(left, right));
    
    assertEquals(listOf("left", "right"), traverse.unsafeRunSync());
  }

  @Test
  public void raceA() {
    UIO<Either<Integer, String>> race = UIO.race(
        UIO.sleep(Duration.ofMillis(10)).map(x -> 10),
        UIO.sleep(Duration.ofMillis(100)).map(x -> "b"));
    
    Either<Integer, String> orElseThrow = race.unsafeRunSync();
    
    assertEquals(Either.left(10), orElseThrow);
  }

  @Test
  public void raceB() {
    UIO<Either<Integer, String>> race = UIO.race(
        UIO.sleep(Duration.ofMillis(100)).map(x -> 10),
        UIO.sleep(Duration.ofMillis(10)).map(x -> "b"));
    
    Either<Integer, String> orElseThrow = race.unsafeRunSync();
    
    assertEquals(Either.right("b"), orElseThrow);
  }
  
  @Test
  public void fork() {
    UIO<String> result = For.with(UIOInstances.monad())
      .then(UIO.pure("hola"))
      .flatMap(hello -> {
        UIO<Unit> sleep = UIO.sleep(Duration.ofSeconds(1));
        UIO<String> task = UIO.task(() -> hello + " toni");
        return sleep.andThen(task).fork();
      })
      .flatMap(Fiber::join).fix(toUIO());
    
    String orElseThrow = result.unsafeRunSync();

    assertEquals("hola toni", orElseThrow);
  }
  
  @Test
  public void timeoutFail() {
    assertThrows(TimeoutException.class, () -> UIO.never().timeout(Duration.ofSeconds(1)).unsafeRunSync());
  }
  
  @Test
  public void timeoutSuccess() {
    assertEquals(1, UIO.pure(1).timeout(Duration.ofSeconds(1)).unsafeRunSync());
  }
  
  @Test
  public void memoize(@Mock Function1<String, String> toUpperCase) {
    when(toUpperCase.apply(any()))
      .thenAnswer(args -> args.getArgument(0, String.class).toUpperCase());
    
    UIO<Function1<String, UIO<String>>> memoized = UIO.memoize((String str) -> UIO.pure(toUpperCase.apply(str)));
    
    UIO<String> flatMap = memoized.flatMap(x -> x.apply("hola"));
    flatMap.unsafeRunSync();
    flatMap.unsafeRunSync();
    flatMap.unsafeRunSync();
    flatMap.unsafeRunSync();
    
    verify(toUpperCase).apply("hola");
  }
  
  @Test
  public void fibonacciTest() {
    assertAll(
        () -> assertEquals(1, fib(1).unsafeRunSync()),
        () -> assertEquals(1, fib(2).unsafeRunSync()),
        () -> assertEquals(2, fib(3).unsafeRunSync()),
        () -> assertEquals(3, fib(4).unsafeRunSync()),
        () -> assertEquals(5, fib(5).unsafeRunSync()),
        () -> assertEquals(8, fib(6).unsafeRunSync()),
        () -> assertEquals(13, fib(7).unsafeRunSync()),
        () -> assertEquals(21, fib(8).unsafeRunSync()),
        () -> assertEquals(55, fib(10).unsafeRunSync()),
        () -> assertEquals(6765, fib(20).unsafeRunSync())
        );
  }

  private UIO<Integer> fib(int number) {
    if (number < 2) {
      return UIO.pure(number);
    }
    return UIO.parMap2(fib(number - 1), fib(number - 2), Integer::sum);
  }

  private UIO<Integer> parseInt(String string) {
    return task(() -> Integer.parseInt(string));
  }

  private UIO<ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private UIO<ResultSet> openError() {
    return raiseError(new SQLException("error"));
  }

  private Function1<ResultSet, UIO<String>> getString(String column) {
    return resultSet -> task(() -> resultSet.getString(column));
  }
}

