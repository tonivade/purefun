/*
 * Copyright (c) 2018, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun;

public interface Higher3<F extends Kind, T, V, U> extends Higher2<Higher1<F, T>, V, U>, Higher1<Higher1<Higher1<F, T>, V>, U> {

}
