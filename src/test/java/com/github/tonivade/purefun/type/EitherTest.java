/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.type;

import static com.github.tonivade.purefun.Function1.identity;
import static com.github.tonivade.purefun.data.ImmutableList.empty;
import static com.github.tonivade.purefun.data.Sequence.listOf;
import static com.github.tonivade.purefun.type.Eval.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.Higher2;
import com.github.tonivade.purefun.MappableLaws;
import com.github.tonivade.purefun.data.ImmutableList;
import com.github.tonivade.purefun.data.Sequence;
import com.github.tonivade.purefun.instances.EitherInstances;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.instances.OptionInstances;
import com.github.tonivade.purefun.typeclasses.Eq;
import com.github.tonivade.purefun.typeclasses.Foldable;
import com.github.tonivade.purefun.typeclasses.MonadError;
import com.github.tonivade.purefun.typeclasses.Monoid;
import com.github.tonivade.purefun.typeclasses.Traverse;

public class EitherTest {

  private final Function1<String, String> toUpperCase = string -> string.toUpperCase();
  private final Function1<String, String> toLowerCase = string -> string.toLowerCase();
  private final Function1<Integer, Integer> intDouble = i -> i * 2;

  @Test
  public void bimapRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").bimap(intDouble, toUpperCase);

    assertEquals(Either.right("HOLA MUNDO"), either);
  }

  @Test
  public void bimapLeft() {
    Either<Integer, String> either =
        Either.<Integer, String>left(10).bimap(intDouble, toUpperCase);

    assertEquals(Either.left(20), either);
  }

  @Test
  public void mapRight() {
    Either<Integer, String> either = Either.<Integer, String>right("Hola mundo").map(toUpperCase);

    assertEquals(Either.right("HOLA MUNDO"), either);
  }

  @Test
  public void mapRightLeft() {
    Either<Integer, String> either = Either.<Integer, String>left(10).map(toUpperCase);

    assertEquals(Either.left(10), either);
  }

  @Test
  public void mapLeft() {
    Either<Integer, String> either = Either.<Integer, String>left(10).mapLeft(intDouble);

    assertEquals(Either.left(20), either);
  }

  @Test
  public void mapLeftRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").mapLeft(intDouble);

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void flatMapRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").flatMap(toUpperCase.liftRight());

    assertEquals(Either.right("HOLA MUNDO"), either);
  }

  @Test
  public void flatMapRightLeft() {
    Either<Integer, String> either =
        Either.<Integer, String>left(10).flatMap(toUpperCase.liftRight());

    assertEquals(Either.left(10), either);
  }

  @Test
  public void flatMapLeft() {
    Either<Integer, String> either =
        Either.<Integer, String>left(10).flatMapLeft(intDouble.liftLeft());

    assertEquals(Either.left(20), either);
  }

  @Test
  public void flatMapLeftRight() {
    Either<Integer, String> either =
        Either.<Integer, String>right("Hola mundo").flatMapLeft(intDouble.liftLeft());

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void filter() {
    Option<Either<Integer, String>> option = Either.<Integer, String>right("Hola mundo")
        .filter(string -> string.startsWith("Hola"));

    assertEquals(Option.some(Either.right("Hola mundo")), option);
  }

  @Test
  public void notFilter() {
    Option<Either<Integer, String>> option = Either.<Integer, String>right("Hola mundo")
        .filter(string -> string.startsWith("hola"));

    assertEquals(Option.none(), option);
  }

  @Test
  public void filterLeft() {
    Option<Either<Integer, String>> option = Either.<Integer, String>left(10)
        .filter(string -> string.startsWith("Hola"));

    assertEquals(Option.none(), option);
  }

  @Test
  public void filterOrElse() {
    Either<Integer, String> either = Either.<Integer, String>right("Hola mundo")
        .filterOrElse(string -> string.startsWith("Hola"), () -> Either.right("or else"));

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void notFilterOrElse() {
    Either<Integer, String> either = Either.<Integer, String>right("Hola mundo")
        .filterOrElse(string -> string.startsWith("hola"), () -> Either.right("or else"));

    assertEquals(Either.right("or else"), either);
  }

  @Test
  public void filterOrElseLeft() {
    Either<Integer, String> either = Either.<Integer, String>left(10)
        .filterOrElse(string -> string.startsWith("hola"), () -> Either.right("or else"));

    assertEquals(Either.left(10), either);
  }

  @Test
  public void orElseRight() {
    String value = Either.<Integer, String>right("Hola mundo").getOrElse("or else");

    assertEquals("Hola mundo", value);
  }

  @Test
  public void orElseLeft() {
    String value = Either.<Integer, String>left(10).getOrElse("or else");

    assertEquals("or else", value);
  }

  @Test
  public void foldRight() {
    String value = Either.<Integer, String>right("Hola mundo").fold(String::valueOf, toUpperCase);

    assertEquals("HOLA MUNDO", value);
  }

  @Test
  public void foldLeft() {
    String value = Either.<Integer, String>left(10).fold(String::valueOf, toUpperCase);

    assertEquals("10", value);
  }

  @Test
  public void swapRight() {
    Either<String, Integer> either = Either.<Integer, String>right("Hola mundo").swap();

    assertEquals(Either.left("Hola mundo"), either);
  }

  @Test
  public void swapLeft() {
    Either<Integer, String> either = Either.<String, Integer>left("Hola mundo").swap();

    assertEquals(Either.right("Hola mundo"), either);
  }

  @Test
  public void right() {
    Either<Integer, String> right = Either.right("Hola mundo");

    assertAll(() -> assertTrue(right.isRight()),
              () -> assertFalse(right.isLeft()),
              () -> assertEquals("Right(Hola mundo)", right.toString()),
              () -> assertEquals("Hola mundo", right.get()),
              () -> assertEquals("Hola mundo", right.getRight()),
              () -> assertEquals(Option.some("Hola mundo"), right.right()),
              () -> assertEquals(Option.none(), right.left()),
              () -> assertEquals(Either.right("Hola mundo"), right),
              () -> assertEquals(Option.some("Hola mundo"), right.toOption()),
              () -> assertEquals(Validation.valid("Hola mundo"), right.toValidation()),
              () -> assertEquals(singletonList("Hola mundo"), right.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> right.getLeft()));
  }

  @Test
  public void left() {
    Either<Integer, String> left = Either.left(10);

    assertAll(() -> assertTrue(left.isLeft()),
              () -> assertFalse(left.isRight()),
              () -> assertEquals("Left(10)", left.toString()),
              () -> assertEquals(Integer.valueOf(10), left.getLeft()),
              () -> assertEquals(Option.some(10), left.left()),
              () -> assertEquals(Option.none(), left.right()),
              () -> assertEquals(Either.left(10), left),
              () -> assertEquals(Option.none(), left.toOption()),
              () -> assertEquals(Validation.invalid(10), left.toValidation()),
              () -> assertEquals(emptyList(), left.stream().collect(toList())),
              () -> assertThrows(NoSuchElementException.class, () -> left.get()),
              () -> assertThrows(NoSuchElementException.class, () -> left.getRight()));
  }

  @Test
  public void eq() {
    Either<Integer, String> left1 = Either.left(10);
    Either<Integer, String> left2 = Either.left(10);
    Either<Integer, String> right1 = Either.right("hola");
    Either<Integer, String> right2 = Either.right("hola");

    Eq<Higher2<Either.µ, Integer, String>> instance = EitherInstances.eq(Eq.any(), Eq.any());

    assertAll(
        () -> assertTrue(instance.eqv(left1, left2)),
        () -> assertTrue(instance.eqv(right1, right2)),
        () -> assertFalse(instance.eqv(left1, right1)),
        () -> assertFalse(instance.eqv(right2, left2)));
  }

  @Test
  public void flatten() {
    Either<String, Either<String, Integer>> eitherOfEither = Either.right(Either.right(10));

    assertEquals(Either.right(10), eitherOfEither.flatten());
  }

  @Test
  public void flattenError() {
    Either<String, Integer> either = Either.right(10);

    assertThrows(UnsupportedOperationException.class, () -> either.flatten());
  }

  @Test
  public void rightLaws() {
    MappableLaws.verifyLaws(Either.right("Hola"));
  }

  @Test
  public void leftLaws() {
    Either<String, Integer> either = Either.left("Hola");

    assertAll(() -> assertEquals(either,
                                 either.mapLeft(identity()),
                                 "identity law"),
              () -> assertEquals(either.mapLeft(toUpperCase).mapLeft(toLowerCase),
                                 either.mapLeft(toUpperCase.andThen(toLowerCase)),
                                 "composition law"),
              () -> assertEquals(either.mapLeft(toUpperCase).mapLeft(toLowerCase.andThen(toUpperCase)),
                                 either.mapLeft(toUpperCase.andThen(toLowerCase)).mapLeft(toUpperCase),
                                 "associativity law")
              );
  }

  @Test
  public void monadError() {
    RuntimeException error = new RuntimeException("error");
    MonadError<Higher1<Either.µ, Throwable>, Throwable> monadError = EitherInstances.<Throwable>monadError();

    Higher1<Higher1<Either.µ, Throwable>, String> pure = monadError.pure("is not ok");
    Higher1<Higher1<Either.µ, Throwable>, String> raiseError = monadError.raiseError(error);
    Higher1<Higher1<Either.µ, Throwable>, String> handleError =
        monadError.handleError(raiseError, e -> "not an error");
    Higher1<Higher1<Either.µ, Throwable>, String> ensureOk =
        monadError.ensure(pure, () -> error, value -> "is not ok".equals(value));
    Higher1<Higher1<Either.µ, Throwable>, String> ensureError =
        monadError.ensure(pure, () -> error, value -> "is ok?".equals(value));

    assertAll(
        () -> assertEquals(Either.left(error), raiseError),
        () -> assertEquals(Either.right("not an error"), handleError),
        () -> assertEquals(Either.left(error), ensureError),
        () -> assertEquals(Either.right("is not ok"), ensureOk));
  }

  @Test
  public void foldable() {
    Foldable<Higher1<Either.µ, Throwable>> instance = EitherInstances.foldable();

    assertAll(
        () -> assertEquals(empty(), instance.foldLeft(Either.left(new Error()), empty(), ImmutableList::append)),
        () -> assertEquals(listOf("hola!"), instance.foldLeft(Either.right("hola!"), empty(), ImmutableList::append)),
        () -> assertEquals(empty(), instance.foldRight(Either.left(new Error()), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals(listOf("hola!"), instance.foldRight(Either.right("hola!"), now(empty()), (a, lb) -> lb.map(b -> b.append(a))).value()),
        () -> assertEquals("", instance.fold(Monoid.string(), Either.left(new Error()))),
        () -> assertEquals("hola!", instance.fold(Monoid.string(), Either.right("hola!"))),
        () -> assertEquals(Option.none(), instance.reduce(Either.left(new Error()), String::concat)),
        () -> assertEquals(Option.some("hola!"), instance.reduce(Either.right("hola!"), String::concat)),
        () -> assertEquals(empty(), instance.foldMap(Sequence.monoid(), Either.left(new Error()), Sequence::listOf)),
        () -> assertEquals(listOf("hola!"), instance.foldMap(Sequence.monoid(), Either.right("hola!"), Sequence::listOf)),
        () -> assertEquals(Id.of(empty()), instance.foldM(IdInstances.monad(), Either.left(new Error()), empty(), (acc, a) -> Id.of(acc.append(a)))),
        () -> assertEquals(Id.of(listOf("hola!")), instance.foldM(IdInstances.monad(), Either.right("hola!"), empty(), (acc, a) -> Id.of(acc.append(a)))));
  }

  @Test
  public void traverse() {
    Traverse<Higher1<Either.µ, Throwable>> instance = EitherInstances.traverse();

    Exception error = new Exception("error");

    assertAll(
        () -> assertEquals(Option.some(Either.right("HELLO!")),
            instance.traverse(OptionInstances.applicative(), Either.right(Option.some("hello!")),
                t -> t.map(String::toUpperCase))),
        () -> assertEquals(Option.some(Either.left(error)),
            instance.traverse(OptionInstances.applicative(), Either.<Throwable, Option<String>>left(error),
                t -> t.map(String::toUpperCase))));
  }
}
