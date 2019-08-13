/*
 * Copyright (c) 2018-2019, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import com.github.tonivade.purefun.Tuple;
import com.github.tonivade.purefun.Tuple2;
import org.junit.jupiter.api.Test;

import java.awt.*;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IsoTest {

   private final Iso<Point, Tuple2<Integer, Integer>> pointToTuple =
       Iso.of(
         point -> Tuple.of(point.x, point.y),
         tuple -> new Point(tuple.get1(), tuple.get2()));
   private final Iso<Tuple2<Integer, Integer>, Point> tupleToPoint = pointToTuple.reverse();

   private final Point point = new Point(1, 2);
   private final Tuple2<Integer, Integer> tuple2 = Tuple.of(1, 2);

   @Test
   public void iso() {
      assertAll(
         () -> assertEquals(tuple2, pointToTuple.get(point)),
         () -> assertEquals(point, pointToTuple.set(tuple2)),
         () -> assertEquals(tuple2, tupleToPoint.set(point)),
         () -> assertEquals(point, tupleToPoint.get(tuple2)),
         () -> assertEquals(tuple2, tupleToPoint.reverse().reverse().set(point)),
         () -> assertEquals(point, tupleToPoint.reverse().reverse().get(tuple2)),
         () -> assertEquals(new Point(2, 4), pointToTuple.modify(point, tuple -> tuple.map(x -> x + 1, y -> y * 2)))
      );
   }
}
