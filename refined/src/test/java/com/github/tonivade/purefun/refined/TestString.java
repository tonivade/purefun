package com.github.tonivade.purefun.refined;

import java.io.Serializable;

@RefinedString(minSize = 12, maxSize = 12)
public interface TestString extends Comparable<TestString>, Serializable, CharSequence {

}
