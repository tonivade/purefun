# Purefun

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/38422db161da48f09cd192c7e7caa7dd)](https://www.codacy.com/app/zeromock/purefun?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=tonivade/purefun&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/38422db161da48f09cd192c7e7caa7dd)](https://www.codacy.com/app/zeromock/purefun?utm_source=github.com&utm_medium=referral&utm_content=tonivade/purefun&utm_campaign=Badge_Coverage)
[![Build Status](https://travis-ci.org/tonivade/purefun.svg?branch=master)](https://travis-ci.org/tonivade/purefun)

This module was developed as the core of the zeromock project. It defines all the basic classes and interfaces 
used in the rest of the project.

Initially the module only holds a few basic interfaces and it has grown to become an entire
functional programming library (well, a humble one), and now is an independent library.

Working in this library helps me to learn and understand some important concepts
of functional programming.

Finally, I have to say thanks to vavr library author, this library is largely inspired in his work,
and also to Scala standard library authors. Their awesome work help me a lot.

## Disclaimer

This project is not ready to be used in production, I use it to learn functional programming concepts by my self, but, if you want to you it, use it at your own risk. Anyway if you think is useful for you, go ahead, also any feedback and PR are wellcome.

## Base Interfaces

### Functor

It represent the arrow between two categories, encapsulates a data type that can be mapped to other data type. 

```java
interface Functor<W extends Witness, T> extends Higher<W, T> {
  <R> Functor<W, R> map(Function1<T, R> map);
}
```

### Monad

It is difficult to explain what a monad is, many people have tried and this is my humble attempt. It is something that allows to combine operations, in a functional way, but simulating the imperative style. For example, `State`, `Reader`, `Writer` and `IO` monads are ways to combine operations.

```java
interface Monad<W extends Witness, T> extends Higher<W, T>, Functor<W, T> {
  <R> Monad<W, R> flatMap(Function1<T, ? extends Higher<W, R>> map);
}
```

### Holder

It represent a type that can hold a value. It defines these methods: `get` and `flatten`.

### Filterable

It represent a type that can be filtered. It defines one method: `filter`.

### Foldable

It represent a type that can be folded. It defined these methods: `reduce`, `fold`, `foldLeft` and `foldRight`

## Data types

All these data types implement `Monad` base interface and implement these methods: `get`, `map`, `flatMap`, `filter`, `fold` and `flatten`.

### Option

Is an alternative to `Optional` of Java standard library. It can contains two values, a `some` or a `none`

```java
Option<String> some = Option.some("Hello world");

Option<String> none = Option.none();
```

### Try

Is an implementation of scala `Try` in Java. It can contains two values, a `success` or a `failure`.

```java
Try<String> success = Try.success("Hello world");

Try<String> failure = Try.failure(new RuntimeException("Error"));
```

### Either

Is an implementation of scala `Either` in Java.

```java
Either<Integer, String> right = Either.right("Hello world");

Either<Integer, String> left = Either.left(100);
```

### Validation

This type represents two different states, valid or invalid, an also it allows to combine several validations using `map2` to `map5` methods.

```java
Validation<String, String> name = Validation.valid("John Smith");
Validation<String, String> email = Validation.valid("john.smith@example.net");

// Person has a constructor with two String parameters, name and email.
Valdation<Sequence<String>, Person> person = Validation.map2(name, email, Person::new); 
```

## Tuples

These classes allow to hold some values together, as tuples. There are tuples from 1 to 5.

```java
Tuple1<String> tuple1 = Tuple.of("Hello world");

Tuple2<String, Integer> tuple2 = Tuple.of("John Smith", 100);
```

## Data structures

Java doesn't define immutable collections, so I have implemented some of them.

### Sequence

Is the equivalent to java `Collection` interface. It defines all the common methods. Also implements `Monad` interface.

### ImmutableList

It represents a linked list. It has a head and a tail.

### ImmutableSet

It represents a set of elements. This elements cannot be duplicated.

### ImmutableArray

It represents an array. You can access to the elements by its position in the array.

### ImmutableMap

This class represents a hash map.

### ImmutableTree

TODO: not implemented yet

This class represents a binary tree.

## Monads

Also I have implemented some Monads that allows to combine some operations.

### State Monad

Is the traditional State Modad from FP languages, like Haskel or Scala. It allows to combine 
operations over a state. The state should be a immutable class. It recives an state and generates
a tuple with the new state and an intermediate result.

```java
State<ImmutableList<String>, Option<String>> read = State.state(list -> Tuple.of(list.tail(), list.head()));
  
Tuple<ImmutableList<String>, Option<String>> result = read.run(ImmutableList.of("a", "b", "c"));
    
assertEquals(Tuple.of(ImmutableList.of("b", "c"), Option.some("a")), result);
```

### Reader Monad

This is an implementation of Reader Monad. It allows to combine operations over a common input.
It can be used to inject dependencies.

```java
Reader<ImmutableList<String>, String> read2 = Reader.reader(list -> list.tail().head().orElse(""));

String result = read2.eval(ImmutableList.of("a", "b", "c"));

assertEqual("b", result);
```

### Writer Monad

It allow to combine operations over a common output.

```java
Writer<ImmutableList<String>, Integer> writer = Writer.<String, Integer>listPure(5)
    .flatMap(value -> listWriter("add 5", value + 5))
    .flatMap(value -> listWriter("plus 2", value * 2));

assertAll(() -> assertEquals(Integer.valueOf(20), writer.getValue()),
          () -> assertEquals(listOf("add 5", "plus 2"), writer.getLog()));
```

### IO Monad

This is a experimental implementation of IO Monad in java. Inspired in this [work](https://gist.github.com/joergrathlev/f17092d3470dcf732be6).

```java
  IO<Nothing> echo = Console.print("write your name")
      .andThen(Console.read())
      .flatMap(name -> Console.print("Hello " + name))
      .andThen(Console.print("end"));
      
  echo.unsafeRunSync();
```

### Free Monad

Finally, after hours of hard coding, I managed to implement a Free monad. This is a highly inestable implementation and I have implemented because it can be implemented. Inspired in this [work](https://github.com/xuwei-k/free-monad-java).

```java 
  Free<IOProgram.µ, Nothing> echo =
      IOProgram.write("what's your name?")
        .andThen(IOProgram.read())
        .flatMap(text -> IOProgram.write("Hello " + text))
        .andThen(IOProgram.write("end"));

  Higher<IO.µ, Nothing> foldMap = echo.foldMap(new IOMonad(),
                                                   new IOProgramFunctor(),
                                                   new IOProgramInterperter());

  IO.narrowK(foldMap).unsafeRunSync();
```

### Kleisli

Also I implemented the Kleisli composition for functions that returns monadic values like `Option`, `Try` or `Either`.

```java
  Kleisli<Try.µ, String, Integer> toInt = Kleisli.lift(Try.monad(), Integer::parseInt);
  Kleisli<Try.µ, Integer, Double> half = Kleisli.lift(Try.monad(), i -> i / 2.);

  Higher1<Try.µ, Double> result = toInt.compose(half).run("123");

  assertEquals(Try.success(61.5), result);
```

## Algebra

Some algebraic data types

### Semigroup

It represents a binary operation over a type.

```java
T combine(T t1, T t2);
```

There are instances for lists, strings and integers.

### Monoid

Extends `Semigroup` adding a zero operation that represent an identity.

```java
T zero();
T combine(T t1, T t2);
```

There are instances for strings and integers.

### SemigroupK

It represents a `Semigroup` but defined for a kind, like a List, so it extends a regular `Semigroup`.

### MonoidK

The same like `SemigroupK` but for a `Monoid`.

### Functor

With higher kinded types simulation, we can represent a `Functor` in Java.

```java
public interface Functor<F extends Witness> {
  <T, R> Higher<F, R> map(Higher<F, T> value, Function1<T, R> map);
}
```

### Applicative

Also an `Applicative`

```java
public interface Applicative<F extends Witness> extends Functor<F> {

  <T> Higher<F, T> pure(T value);

  <T, R> Higher1<F, R> ap(Higher1<F, T> value, Higher1<F, Function1<T, R>> apply);
  
  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return ap(value, pure(map));
  }
}
```

### Monad

Also a `Monad`

```java
public interface Monad<F extends Witness> extends Applicative<F> {

  <T, R> Higher1<F, R> flatMap(Higher1<F, T> value, Function1<T, ? extends Higher1<F, R>> map);

  @Override
  default <T, R> Higher1<F, R> map(Higher1<F, T> value, Function1<T, R> map) {
    return flatMap(value, map.andThen(this::pure));
  }
  
  @Override
  default <T, R> Higher1<F, R> ap(Higher1<F, T> value, Higher1<F, Function1<T, R>> apply) {
    return flatMap(apply, map -> map(value, map));
  }
}
```

### Transformer

It represents a natural transformation between two different kinds.

```java
public interface Transformer<F extends Witness, T extends Witness> {
  <X> Higher<T, X> apply(Higher<F, X> from);
}
```

## Equal

This class helps to create readable `equals` methods. An example:

```java
  @Override
  public boolean equals(Object obj) {
    return Equal.of(this)
        .append(comparing(Data::getId))
        .append(comparing(Data::getValue))
        .applyTo(obj);
  }
```

## License

purefun is released under MIT license
