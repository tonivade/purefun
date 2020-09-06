/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.Nothing.nothing;
import static com.github.tonivade.purefun.effect.RIO.pure;
import static com.github.tonivade.purefun.effect.RIO.raiseError;
import static com.github.tonivade.purefun.effect.RIO.task;
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
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.instances.IOInstances;
import static com.github.tonivade.purefun.monad.IOOf.toIO;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

@ExtendWith(MockitoExtension.class)
public class RIOTest {

  @Captor
  private ArgumentCaptor<Try<Integer>> captor;

  @Test
  public void mapRight() {
    Try<Integer> result = parseInt("1").map(x -> x + 1).safeRunSync(nothing());

    assertEquals(Try.success(2), result);
  }

  @Test
  public void mapLeft() {
    RIO<Nothing, Integer> result = parseInt("lskjdf").map(x -> x + 1);

    assertEquals(NumberFormatException.class, result.safeRunSync(nothing()).getCause().getClass());
  }

  @Test
  public void flatMapRight() {
    Try<Integer> result = parseInt("1").flatMap(x -> pure(x + 1)).safeRunSync(nothing());

    assertEquals(Try.success(2), result);
  }

  @Test
  public void flatMapLeft() {
    RIO<Nothing, Integer> result = parseInt("kjere").flatMap(x -> pure(x + 1));

    assertEquals(NumberFormatException.class, result.safeRunSync(nothing()).getCause().getClass());
  }

  @Test
  public void redeemRight() {
    Try<Integer> result = parseInt("1").recover(e -> -1).safeRunSync(nothing());

    assertEquals(Try.success(1), result);
  }

  @Test
  public void redeemLeft() {
    Try<Integer> result = parseInt("kjsdfdf").recover(e -> -1).safeRunSync(nothing());

    assertEquals(Try.success(-1), result);
  }

  @Test
  public void bracket() throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    when(resultSet.getString("id")).thenReturn("value");

    RIO<Nothing, String> bracket = RIO.bracket(open(resultSet), getString("id"));

    assertEquals(Try.success("value"), bracket.safeRunSync(nothing()));
    verify(resultSet).close();
  }

  @Test
  public void bracketError() {
    RIO<Nothing, String> bracket = RIO.bracket(openError(), getString("id"));

    assertEquals(SQLException.class, bracket.safeRunSync(nothing()).getCause().getClass());
  }

  @Test
  public void asyncRight(@Mock Consumer1<Try<Integer>> callback) {
    parseInt("1").safeRunAsync(nothing(), callback);

    verify(callback, timeout(1000)).accept(Try.success(1));
  }

  @Test
  public void asyncLeft(@Mock Consumer1<Try<Integer>> callback) {
    parseInt("kjsdf").safeRunAsync(nothing(), callback);

    verify(callback, timeout(100)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().getCause().getClass());
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

    assertThrows(NumberFormatException.class, future.fix(toIO())::unsafeRunSync);
  }

  @Test
  public void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);

    Try<String> retry = task(computation).retry().safeRunSync(nothing());

    assertTrue(retry.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  public void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");

    Try<String> repeat = task(computation).repeat().safeRunSync(nothing());

    assertEquals("hola", repeat.get());
    verify(computation, times(2)).get();
  }

  @Test
  public void testCompositionWithZIO() {
    ZIO<Environment, Throwable, Integer> getValue = ZIO.access(Environment::getValue);
    ZIO<Environment, Throwable, Unit> zio = RIO.<Environment>unit().toZIO();
    ZIO<Environment, Throwable, Integer> result = zio.andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }

  private RIO<Nothing, Integer> parseInt(String string) {
    return task(() -> Integer.parseInt(string));
  }

  private RIO<Nothing, ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private RIO<Nothing, ResultSet> openError() {
    return raiseError(new SQLException("error"));
  }

  private Function1<ResultSet, RIO<Nothing, String>> getString(String column) {
    return resultSet -> task(() -> resultSet.getString(column));
  }
}

