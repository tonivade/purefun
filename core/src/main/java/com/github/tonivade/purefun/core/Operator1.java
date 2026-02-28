/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import java.util.function.UnaryOperator;

@FunctionalInterface
public interface Operator1<T> extends Function1<T, T>, UnaryOperator<T> {

}
