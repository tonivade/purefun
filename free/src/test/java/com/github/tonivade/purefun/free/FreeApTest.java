package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.instances.IdInstances;
import com.github.tonivade.purefun.type.Id;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.FunctionK;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FreeApTest {

  @Test
  public void test() {
    Applicative<Higher1<FreeAp.µ, DSL.µ>> applicative = FreeAp.applicativeF();

    FreeAp<DSL.µ, Integer> readInt = DSL.readInt();
    FreeAp<DSL.µ, String> readString = DSL.readString();
    Higher1<Higher1<FreeAp.µ, DSL.µ>, Tuple2<Integer, String>> tuple =
        applicative.tuple(readInt.kind1(), readString.kind1());

    FreeAp<DSL.µ, Tuple2<Integer, String>> fix = FreeAp.narrowK(tuple);

    Higher1<Id.µ, Tuple2<Integer, String>> map = fix.foldMap(idTransform(), IdInstances.applicative());

    assertEquals(Id.of(Tuple2.of(1, "string")), map.fix1(Id::narrowK));
  }

  @SuppressWarnings("unchecked")
  private FunctionK<DSL.µ, Id.µ> idTransform() {
    return new FunctionK<DSL.µ, Id.µ>() {
      @Override
      public <T> Higher1<Id.µ, T> apply(Higher1<DSL.µ, T> from) {
        DSL<?> dsl = from.fix1(DSL::narrowK);
        if (dsl instanceof ReadInt) {
          return (Higher1<Id.µ, T>) Id.of(1).kind1();
        }
        if (dsl instanceof ReadString) {
          return (Higher1<Id.µ, T>) Id.of("string").kind1();
        }
        throw new IllegalStateException();
      }
    };
  }

}

@HigherKind
interface DSL<A> {

  static FreeAp<DSL.µ, Integer> readInt() {
    return FreeAp.lift(new ReadInt().kind1());
  }

  static FreeAp<DSL.µ, String> readString() {
    return FreeAp.lift(new ReadString().kind1());
  }
}

final class ReadInt implements DSL<Integer> {

}

final class ReadString implements DSL<String> {

}
