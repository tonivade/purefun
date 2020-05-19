/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import static com.github.tonivade.purefun.effect.UIO.pure;
import static com.github.tonivade.purefun.effect.UIO.raiseError;
import static com.github.tonivade.purefun.effect.UIO.task;
import static com.github.tonivade.purefun.effect.UIO.unit;
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
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.Producer;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IOOf;
import com.github.tonivade.purefun.monad.IO_;
import com.github.tonivade.purefun.type.Either;
import com.github.tonivade.purefun.type.Try;
import com.github.tonivade.purefun.typeclasses.MonadDefer;

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
  public void asyncRight(@Mock Consumer1<Try<Integer>> callback) {
    parseInt("1").async(callback);

    verify(callback, timeout(100)).accept(Try.success(1));
  }

  @Test
  public void asyncLeft(@Mock Consumer1<Try<Integer>> callback) {
    parseInt("kjsdf").async(callback);

    verify(callback, timeout(100)).accept(captor.capture());

    assertEquals(NumberFormatException.class, captor.getValue().getCause().getClass());
  }

  @Test
  public void foldMapRight() {
    MonadDefer<IO_> monadDefer = IOInstances.monadDefer();

    Higher1<IO_, Integer> future = parseInt("0").foldMap(monadDefer);

    assertEquals(0, future.fix1(IOOf::narrowK).unsafeRunSync());
  }

  @Test
  public void foldMapLeft() {
    MonadDefer<IO_> monadDefer = IOInstances.monadDefer();

    Higher1<IO_, Integer> future = parseInt("jkdf").foldMap(monadDefer);

    assertThrows(NumberFormatException.class, future.fix1(IOOf::narrowK)::unsafeRunSync);
  }

  @Test
  public void retry(@Mock Producer<String> computation) {
    when(computation.get()).thenThrow(UnsupportedOperationException.class);

    Try<String> retry = task(computation).retry().safeRunSync();

    assertTrue(retry.isFailure());
    verify(computation, times(2)).get();
  }

  @Test
  public void repeat(@Mock Producer<String> computation) {
    when(computation.get()).thenReturn("hola");

    Try<String> repeat = task(computation).repeat().safeRunSync();

    assertEquals("hola", repeat.get());
    verify(computation, times(2)).get();
  }

  @Test
  public void testCompositionWithZIO() {
    ZIO<Environment, Nothing, Integer> getValue = ZIO.access(Environment::getValue);
    ZIO<Environment, Nothing, Integer> result = unit().<Environment, Nothing>toZIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
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

