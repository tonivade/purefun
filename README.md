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

## Data types

All this data types implements this methods: `get`, `map`, `flatMap`, `filter`, `fold` and `flatten`.

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

This type represents two different states, valid or invalid, an also it allows to combine several
validations using `map2` to `map5` methods.

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

Is the equivalent to java `Collection` interface. It defines all the common methods.

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

### IO Monad

This is a experimental implementation of IO Monad in java. Inspired in this [work](https://gist.github.com/joergrathlev/f17092d3470dcf732be6).

```java
  IO<Nothing> echo = print("write your name")
      .andThen(read())
      .flatMap(name -> print("Hello " + name))
      .andThen(print("end"));
      
  echo.unsafeRunSync();
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
