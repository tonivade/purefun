/*
 * Copyright (c) 2018-2021, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.control;

import static com.github.tonivade.purefun.Precondition.checkNonNull;
import static com.github.tonivade.purefun.Unit.unit;

import com.github.tonivade.purefun.Operator1;
import com.github.tonivade.purefun.Unit;
import com.github.tonivade.purefun.data.ImmutableMap;

class StateMarker implements Marker.State<ImmutableMap<StateMarker.Field<?>, Object>> {

  private ImmutableMap<Field<?>, Object> data = ImmutableMap.empty();

  @Override
  public ImmutableMap<Field<?>, Object> backup() {
    return data;
  }

  @Override
  public void restore(ImmutableMap<Field<?>, Object> value) {
    this.data = checkNonNull(value);
  }

  public <T> Field<T> field(T value) {
    Field<T> field = new Field<>();
    data = data.put(field, value);
    return field;
  }

  final class Field<T> {

    @SuppressWarnings("unchecked")
    public Control<T> get() {
      return Control.later(() -> (T) data.get(this).get());
    }

    public Control<Unit> set(T value) {
      return Control.later(() -> {
        data = data.put(this, value);
        return unit();
      });
    }

    public Control<Unit> update(Operator1<T> mapper) {
      return get().flatMap(x -> set(mapper.apply(x)));
    }
  }
}
