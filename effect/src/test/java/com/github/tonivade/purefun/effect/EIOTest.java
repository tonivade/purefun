/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.effect;

import com.github.tonivade.purefun.Consumer1;
import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.instances.IOInstances;
import com.github.tonivade.purefun.monad.IO;
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

import static com.github.tonivade.purefun.effect.EIO.from;
import static com.github.tonivade.purefun.effect.EIO.pure;
import static com.github.tonivade.purefun.effect.EIO.raiseError;
import static java.util.concurrent.ThreadLocalRandom.current;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EIOTest {

  @Mock
  private Consumer1<Try<Either<Throwable, Integer>>> callback;
  @Captor
  private ArgumentCaptor<Try<Either<Throwable, Integer>>> captor;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

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
    Either<Throwable, Integer> result = parseInt("1").orElse(() -> pure(2)).safeRunSync();

    assertEquals(Either.right(1), result);
  }

  @Test
  public void orElseLeft() {
    Either<Throwable, Integer> result = parseInt("kjsdfe").orElse(() -> pure(2)).safeRunSync();

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
  public void asyncRight() {
    parseInt("1").async(callback);

    verify(callback, timeout(500)).accept(Try.success(Either.right(1)));
  }

  @Test
  public void asyncLeft() {
    parseInt("kjsdf").async(callback);

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
    MonadDefer<IO.µ> monadDefer = IOInstances.monadDefer();

    Higher1<IO.µ, Either<Throwable, Integer>> future = parseInt("0").foldMap(monadDefer);

    assertEquals(Either.right(0), future.fix1(IO::narrowK).unsafeRunSync());
  }

  @Test
  public void foldMapLeft() {
    MonadDefer<IO.µ> monadDefer = IOInstances.monadDefer();

    Higher1<IO.µ, Either<Throwable, Integer>> future = parseInt("jkdf").foldMap(monadDefer);

    assertEquals(NumberFormatException.class, future.fix1(IO::narrowK).unsafeRunSync().getLeft().getClass());
  }

  @Test
  public void testCompositionWithZIO() {
    ZIO<Environment, Throwable, Integer> getValue = ZIO.accessM(env -> ZIO.pure(env.getValue()));
    ZIO<Environment, Throwable, Integer> result = EIO.<Throwable>unit().<Environment>toZIO().andThen(getValue);

    Environment env = new Environment(current().nextInt());

    assertEquals(Either.right(env.getValue()), result.provide(env));
  }

  private EIO<Throwable, Integer> parseInt(String string) {
    return from(() -> Integer.parseInt(string));
  }

  private EIO<Throwable, ResultSet> open(ResultSet resultSet) {
    return pure(resultSet);
  }

  private Function1<ResultSet, EIO<Throwable, String>> getString(String column) {
    return resultSet -> from(() -> resultSet.getString(column));
  }
}
