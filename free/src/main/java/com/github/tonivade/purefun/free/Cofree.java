/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.free;

import com.github.tonivade.purefun.Function1;
import com.github.tonivade.purefun.Function2;
import com.github.tonivade.purefun.Higher1;
import com.github.tonivade.purefun.HigherKind;
import com.github.tonivade.purefun.Kind;
import com.github.tonivade.purefun.type.Eval;
import com.github.tonivade.purefun.typeclasses.Applicative;
import com.github.tonivade.purefun.typeclasses.Functor;
import com.github.tonivade.purefun.typeclasses.Traverse;

import static java.util.Objects.requireNonNull;

@HigherKind
public final class Cofree<F extends Kind, A> {

  private final Functor<F> functor;
  private final A head;
  private final Eval<Higher1<F, Cofree<F, A>>> tail;

  private Cofree(Functor<F> functor, A head, Eval<Higher1<F, Cofree<F, A>>> tail) {
    this.functor = requireNonNull(functor);
    this.head = requireNonNull(head);
    this.tail = requireNonNull(tail);
  }

  public A head() {
    return head;
  }

  public Higher1<F, Cofree<F, A>> tail() {
    return tail.value();
  }

  public <B> Cofree<F, B> map(Function1<A, B> mapper) {
    return transform(mapper.compose(Cofree::head), c -> c.map(mapper));
  }

  public <B> Cofree<F, B> coflatMap(Function1<Cofree<F, A>, B> mapper) {
    return transform(mapper, c -> c.coflatMap(mapper));
  }

  public <B> Eval<B> fold(Applicative<Eval.µ> applicative, Traverse<F> traverse, Function2<A, Higher1<F, B>, Eval<B>> mapper) {
    Eval<Higher1<F, B>> eval =
        traverse.traverse(applicative, tail(), c -> c.fold(applicative, traverse, mapper).kind1())
            .fix1(Eval::narrowK);
    return eval.flatMap(fb -> mapper.apply(head(), fb));
  }

  private <B> Cofree<F, B> transform(Function1<Cofree<F, A>, B> headMap, Function1<Cofree<F, A>, Cofree<F, B>> tailMap) {
    return of(functor, headMap.apply(this), tail.map(t -> functor.map(t, tailMap)));
  }

  public static <F extends Kind, A> Cofree<F, A> unfold(Functor<F> functor, A head, Function1<A, Higher1<F, A>> unfold) {
    return of(functor, head, Eval.later(() -> functor.map(unfold.apply(head), a -> unfold(functor, a, unfold))));
  }

  public static <F extends Kind, A> Cofree<F, A> of(Functor<F> functor, A head, Eval<Higher1<F, Cofree<F, A>>> tail) {
    return new Cofree<>(functor, head, tail);
  }
}
