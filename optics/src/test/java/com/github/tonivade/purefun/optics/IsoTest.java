/*
 * Copyright (c) 2018-2023, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.awt.Point;
import org.junit.jupiter.api.Test;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.type.Option;
import com.github.tonivade.purefun.type.OptionOf;
import com.github.tonivade.purefun.type.Option_;

public class IsoTest {

  private final Iso<Point, Tuple2<Integer, Integer>> pointToTuple =
      Iso.of(p -> Tuple.of(p.x, p.y), t -> new Point(t.get1(), t.get2()));
  private final Iso<Tuple2<Integer, Integer>, Point> tupleToPoint = pointToTuple.reverse();
  private final Iso<Point, Point> pointToPoint = pointToTuple.compose(tupleToPoint);
  private final Iso<Point, Point> identity = Iso.identity();
  private final Iso<Option<String>, Kind<Option_, String>> optionToKind =
    Iso.of(Option::kind, OptionOf::<String>narrowK);

  private final Point point = new Point(1, 2);
  private final Tuple2<Integer, Integer> tuple = Tuple.of(1, 2);

  @Test
  public void iso() {
    assertAll(
      () -> assertEquals(tuple, pointToTuple.get(point)),
      () -> assertEquals(point, pointToTuple.set(tuple)),
      () -> assertEquals(tuple, tupleToPoint.set(point)),
      () -> assertEquals(point, tupleToPoint.get(tuple)),
      () -> assertEquals(tuple, tupleToPoint.reverse().reverse().set(point)),
      () -> assertEquals(point, tupleToPoint.reverse().reverse().get(tuple)),
      () -> assertEquals(point, pointToPoint.set(point)),
      () -> assertEquals(point, pointToPoint.get(point)),
      () -> assertEquals(point, identity.set(point)),
      () -> assertEquals(point, identity.get(point)),
      () -> assertEquals(new Point(2, 4), pointToTuple.modify(point, t -> t.map(x -> x + 1, y -> y * 2)))
    );
  }

  @Test
  public void isoLaws() {
    verifyLaws(pointToTuple, point, tuple);
    verifyLaws(tupleToPoint, tuple, point);
    verifyLaws(pointToPoint, point, point);
    verifyLaws(identity, point, point);
    verifyLaws(optionToKind, Option.some("hola"), Option.some("hola"));
  }

  private <S, A> void verifyLaws(Iso<S, A> iso, S target, A value) {
    assertAll(
      () -> assertEquals(target, iso.set(iso.get(target))),
      () -> assertEquals(value, iso.get(iso.set(value)))
    );
  }
}
