/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.zio;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.Nothing.nothing;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Nothing;
import com.github.tonivade.purefun.concurrent.Future;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.instances.FutureInstances;
import com.github.tonivade.purefun.type.Either;

public class ZIOTest {

  @Test
  public void mapRight() {
    Either<Throwable, Integer> result =
        parseInt("1").map(x -> x + 1).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void mapLeft() {
    Either<Throwable, Integer> result =
        parseInt("lskjdf").map(x -> x + 1).provide(nothing());

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void mapError() {
    Either<String, Integer> result =
        parseInt("lskjdf").mapError(Throwable::getMessage).provide(nothing());

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void flatMapRight() {
    Either<Throwable, Integer> result =
        parseInt("1").flatMap(x -> ZIO.pure(x + 1)).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void flatMapLeft() {
    Either<Throwable, Integer> result =
        parseInt("lskjdf").flatMap(x -> ZIO.pure(x + 1)).provide(nothing());

    assertEquals(NumberFormatException.class, result.getLeft().getClass());
  }

  @Test
  public void flatMapError() {
    Either<String, Integer> result =
        parseInt("lskjdf").flatMapError(e -> ZIO.raiseError(e.getMessage())).provide(nothing());

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void bimapRight() {
    Either<String, Integer> result =
        parseInt("1").bimap(Throwable::getMessage, x -> x + 1).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void bimapLeft() {
    Either<String, Integer> result =
        parseInt("lskjdf").bimap(Throwable::getMessage, x -> x + 1).provide(nothing());

    assertEquals(Either.left("For input string: \"lskjdf\""), result);
  }

  @Test
  public void foldRight() {
    Either<Nothing, Integer> result =
        parseInt("1").fold(e -> -1, identity()).provide(nothing());

    assertEquals(Either.right(1), result);
  }

  @Test
  public void foldLeft() {
    Either<Nothing, Integer> result =
        parseInt("kjsdfdf").fold(e -> -1, identity()).provide(nothing());

    assertEquals(Either.right(-1), result);
  }

  @Test
  public void orElseRight() {
    Either<Throwable, Integer> result =
        parseInt("1").orElse(() -> ZIO.pure(2)).provide(nothing());

    assertEquals(Either.right(1), result);
  }

  @Test
  public void orElseLeft() {
    Either<Throwable, Integer> result =
        parseInt("kjsdfe").orElse(() -> ZIO.pure(2)).provide(nothing());

    assertEquals(Either.right(2), result);
  }

  @Test
  public void safeRunAsync() {
    Ref<ImmutableList<String>> ref = Ref.of(ImmutableList.empty());
    ZIO<Nothing, Throwable, ImmutableList<String>> currentThread =
        ref.updateAndGet(list -> list.append(Thread.currentThread().getName()));

    ZIO<Nothing, Throwable, ImmutableList<String>> program = currentThread
        .andThen(currentThread
            .andThen(currentThread
                .andThen(currentThread
                    .andThen(currentThread))));

    Either<Throwable, ImmutableList<String>> result =
        program.foldMap(nothing(), FutureInstances.monadDefer()).fix1(Future::narrowK)
          .await(Duration.ofSeconds(5)).get();

    System.out.println(result);
    assertEquals(Either.right(5), result.map(ImmutableList::size));
  }

  private ZIO<Nothing, Throwable, Integer> parseInt(String string) {
    return ZIO.from(() -> Integer.parseInt(string));
  }
}
