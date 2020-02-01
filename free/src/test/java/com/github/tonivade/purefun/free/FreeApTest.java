package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Tuple2;
import com.github.tonivade.purefun.typeclasses.Applicative;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FreeApTest {

  @Test
  public void test() {
    Applicative<Higher1<FreeAp.µ, DSL.µ>> applicative = FreeAp.applicativeF();

    Higher1<Higher1<FreeAp.µ, DSL.µ>, Tuple2<Integer, String>> tuple =
        applicative.tuple(DSL.readInt().kind1(), DSL.readString().kind1());

    System.out.println(tuple);
  }

}

@HigherKind
interface DSL<A> {

  static FreeAp<DSL.µ, Integer> readInt() {
    return FreeAp.liftF(new ReadInt().kind1());
  }

  static FreeAp<DSL.µ, String> readString() {
    return FreeAp.liftF(new ReadString().kind1());
  }
}

final class ReadInt implements DSL<Integer> {

}

final class ReadString implements DSL<String> {

}
