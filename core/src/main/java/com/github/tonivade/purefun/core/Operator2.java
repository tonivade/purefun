/*
 * Copyright (c) 2018-2026, Antonio Gabriel Muñoz Conejo <me at tonivade dot es>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.core;

import java.util.function.BinaryOperator;

@FunctionalInterface
public interface Operator2<T> extends Function2<T, T, T>, BinaryOperator<T> {

}
