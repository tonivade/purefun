/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.effect.Task.pure;
import static com.github.tonivade.purefun.effect.Task.task;
import static com.github.tonivade.purefun.effect.Task.unit;
import static com.github.tonivade.purefun.effect.TaskOf.toTask;
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
import com.github.tonivade.purefun.instances.TaskInstances;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.Fiber;
import com.github.tonivade.purefun.typeclasses.For;

@ExtendWith(MockitoExtension.class)
class TaskTest {

  @Captor
  private ArgumentCaptor<Try<Integer>> captor;

  @Test
  void mapRight() {
    Try<Integer> result = parseInt("1").map(x -> x + 1).safeRunSync();

    assertEquals(Try.success(2), result);
  }

  @Test
  void mapLeft() {
    Try<Integer> result = parseInt("lskjdf").map(x -> x + 1).safeRunSync();

    assertEquals(NumberFormatException.class, result.getCause().getClass());
  }

  @Test
  void flatMapRight() {
    Try<Integer> result = parseInt("1").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(Try.success(2), result);
  }

  @Test
  void flatMapLeft() {
    Try<Integer> result = parseInt("lskjdf").flatMap(x -> pure(x + 1)).safeRunSync();

    assertEquals(NumberFormatException.class, result.getCause().getClass());
  }

  @Test
  void foldRight() {
    Integer result = parseInt("1").recover(e -> -1).unsafeRunSync();

    assertEquals(1, result);
  }

  @Test
  void foldLeft() {
    Integer result = parseInt("kjsdfdf").recover(e -> -1).unsafeRunSync();

    assertEquals(-1, result);
  }

  @Test
  void orElseRight() {
    Try<Integer> result = parseInt("1").orElse(pure(2)).safeRunSync();

    assertEquals(Try.success(1), result);
  }

  @Test
  void orElseLeft() {
    Try<Integer> result = parseInt("kjsdfe").orElse(pure(2)).safeRunSync();

    assertEquals(Try.success(2), result);
  }

  @Test
  void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    Task<String> bracket = Task.bracket(open(resultSet), getString("id"));

    assertEquals(Try.success("value"), bracket.safeRunSync());
    verify(resultSet).close();
  }

  @Test
  void asyncRight(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("1").safeRunAsync(callback);

    verify(callback, timeout(100)).accept(Try.success(1));
  }

  @Test
  void asyncLeft(@Mock Consumer1<? super Try<? extends Integer>> callback) {
    parseInt("kjsdf").safeRunAsync(callback);

    verify(callback, timeout(500)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().getCause().getClass());
  }

  @Test
  void absorb() {
    Exception error = new Exception();
    Task<Either<Throwable, Integer>> task = pure(Either.left(error));

    Try<Integer> result = Task.absorb(task).safeRunSync();

    assertEquals(error, result.getCause());
  }

  @Test
  void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> retry = task(computation).retry().safeRunSync();

    assertTrue(retry.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");
    when(computation.liftTry()).thenCallRealMethod();
    when(computation.liftEither()).thenCallRealMethod();

    Try<String> repeat = task(computation).repeat().safeRunSync();

    assertEquals("hola", repeat.getOrElseThrow());
    verify(computation, times(2)).get();
  }

  @Test
  void testCompositionWithZIO() {
    PureIO<Environment, Throwable, Integer> getValue = PureIO.accessM(env -> PureIO.pure(env.getValue()));
    PureIO<Environment, Throwable, Integer> result = unit().<Environment>toPureIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }
  
  @Test
  void traverse() {
    Task<String> left = task(() -> "left");
    Task<String> right = task(() -> "right");
    
    Task<Sequence<String>> traverse = Task.traverse(listOf(left, right));
    
    assertEquals(Try.success(listOf("left", "right")), traverse.safeRunSync());
  }

  @Test
  void raceA() {
    Task<Either<Integer, String>> race = Task.race(
        Task.sleep(Duration.ofMillis(10)).map(x -> 10),
        Task.sleep(Duration.ofMillis(100)).map(x -> "b"));
    
    Try<Either<Integer, String>> orElseThrow = race.safeRunSync();
    
    assertEquals(Try.success(Either.left(10)), orElseThrow);
  }

  @Test
  void raceB() {
    Task<Either<Integer, String>> race = Task.race(
        Task.sleep(Duration.ofMillis(100)).map(x -> 10),
        Task.sleep(Duration.ofMillis(10)).map(x -> "b"));
    
    Try<Either<Integer, String>> orElseThrow = race.safeRunSync();
    
    assertEquals(Try.success(Either.right("b")), orElseThrow);
  }
  
  @Test
  void fork() {
    Task<String> result = For.with(TaskInstances.monad())
      .then(Task.pure("hola"))
      .flatMap(hello -> {
        Task<Unit> sleep = Task.sleep(Duration.ofSeconds(1));
        Task<String> task = Task.task(() -> hello + " toni");
        return sleep.andThen(task).fork();
      })
      .flatMap(Fiber::join).fix(toTask());
    
    Try<String> orElseThrow = result.safeRunSync();

    assertEquals(Try.success("hola toni"), orElseThrow);
  }
  
  @Test
  void timeoutFail() {
    Try<Unit> safeRunSync = Task.<Unit>never().timeout(Duration.ofSeconds(1)).safeRunSync();
    
    assertTrue(safeRunSync.getCause() instanceof TimeoutException);
  }
  
  @Test
  void timeoutSuccess() {
    assertEquals(Try.success(1), Task.pure(1).timeout(Duration.ofSeconds(1)).safeRunSync());
  }
  
  @Test
  void liftTry() {
    Task<String> flatMap = Task.pure("Toni").flatMap(Task.liftTry(this::helloWorld));
    
    assertEquals(Try.success("Hello Toni!"), flatMap.safeRunSync());
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
  
  private Try<String> helloWorld(String name) {
    return Try.success("Hello " + name + "!");
  }
}
