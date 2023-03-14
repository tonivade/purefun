package com.github.tonivade.purefun.refined;

import java.io.Serializable;

@RefinedInteger(min = 0)
public interface TestInt extends Comparable<TestIntImpl>, Serializable {

}
