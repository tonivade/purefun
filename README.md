# Purefun

```
 ____                  __             
|  _ \ _   _ _ __ ___ / _|_   _ _ __  
| |_) | | | | '__/ _ \ |_| | | | '_ \ 
|  __/| |_| | | |  __/  _| |_| | | | |
|_|    \__,_|_|  \___|_|  \__,_|_| |_|
```

![Build Status](https://github.com/tonivade/purefun/workflows/Java%20CI%20with%20Gradle/badge.svg)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/38422db161da48f09cd192c7e7caa7dd)](https://www.codacy.com/app/zeromock/purefun?utm_source=github.com&utm_medium=referral&utm_content=tonivade/purefun&utm_campaign=Badge_Coverage)
[![Join the chat at https://gitter.im/tonivade/purefun](https://badges.gitter.im/tonivade/purefun.svg)](https://gitter.im/tonivade/purefun?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

This module was developed as the core of the zeromock project. It defines all the basic classes and interfaces
used in the rest of the project.

Initially the module only held a few basic interfaces and it has grown to become an entire
functional programming library (well, a humble one), and now is an independent library.

Working in this library helped me to learn and understand some important concepts
of functional programming, and over time I implemented higher kinded types and type classes
in Java. I don't know if this will be helpful for somebody, but the work is here to everyone want to use it.

Finally, I have to say thanks to [vavr](https://www.vavr.io/) library author, this library is largely inspired in his work,
and also to [Scala](https://www.scala-lang.org/) standard library authors. I don't want to forget some of the projects I've used as 
reference: [Arrow](https://arrow-kt.io/) and [cats](https://typelevel.org/cats/) for type classes implementation, 
[fs2](https://fs2.io/) for stream processing, and [ZIO](https://zio.dev/) to implement my own version in Java. 
Their awesome work help me a lot.

## Disclaimer

**This project is not ready to be used in production**, I use it to learn functional programming concepts by my self, but,
if you want to use it, **use it at your own risk**. Anyway if you think is useful for you, go ahead, also any
feedback and PR are very welcome.

## Higher Kinded Types

In this project I have implemented some patterns of functional programming that need Higher Kinded Types. In Java
there are not such thing, but it can be simulated using a especial codification of types.

In Scala we can define a higher kinded typed just like this `Monad[F[_]]` but in Java it can be codified
like this `Monad<F>`. Then we can define a type using a special codification like this:

```java
interface SomeType<T> extends SomeTypeOf<T> { }

// Boilerplate
interface SomeTypeOf<T> implements Kind<SomeType<?>, T> {

  // this is a safe cast
  static SomeType<T> toSomeType(Kind<SomeType<?>, ? extends T> hkt) {
    return (SomeType<T>) hkt;
  }
}
```

It can be triky but, in the end is easy to work with. By the way, I tried to hide this details to the user of the library.
Except with type classes because is the only way to implement them correctly.

So, there are interfaces to encode kinds of 1, 2 and 3 types. It can be defined types for 4, 5 or more types, but it wasn't
necessary to implement the library.

## Annotation Processor

In order to simplify working with higher kinded types, in the last version I've included an annotation processor to generate
all this boilerplate code:

```java
@HigherKind
interface SomeType<T> extends SomeTypeOf<T> { }
```

With this annotation, all the above code, is generated automatically.

## Data types

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
validations using `mapN` methods.

```java
Validation<String, String> name = Validation.valid("John Smith");
Validation<String, String> email = Validation.valid("john.smith@example.net");

// Person has a constructor with two String parameters, name and email.
Valdation<Sequence<String>, Person> person = Validation.map2(name, email, Person::new);
```

### Const

A object with a phantom parameter:

```java
Const<String, Integer> constInt = Const.of("Hello world!");

Const<String, Float> constFloat = constInt.retag();

assertEquals("Hello world!", constFloat.value());
```

### Future

This is an experimental implementation of Future. Computations are executed in another thread inmediatelly (since version 5.0 de default executor is a virtual thread per task executor).

```java
Future<String> future = Future.success("Hello world!");

Future<String> result = future.flatMap(string -> Future.run(string::toUpperCase));

assertEquals(Try.success("HELLO WORLD!"), result.await());
```

### Trampoline

Implements recursion using an iteration and is stack safe.

```java
private Trampoline<Integer> fibLoop(Integer n) {
  if (n < 2) {
    return Trampoline.done(n);
  }
  return Trampoline.more(() -> fibLoop(n - 1)).flatMap(x -> fibLoop(n - 2).map(y -> x + y));
}
```

### Tuples

These classes allow to hold some values together, as tuples. There are tuples from 1 to 5.

```java
Tuple1<String> tuple1 = Tuple.of("Hello world");

Tuple2<String, Integer> tuple2 = Tuple.of("John Smith", 100);
```

## Data structures

Java doesn't define immutable collections, so I have implemented some of them.

### Sequence

Is the equivalent to java `Collection` interface. It defines all the common methods. The default implementation use persistent collections based on [pcollections](https://github.com/hrldcpr/pcollections/) library.

### ImmutableList

It represents a linked list. It has a head and a tail. Based on `ConsPStack`.

### ImmutableSet

It represents a set of elements. This elements cannot be duplicated. Based on `HashTreePSet`.

### ImmutableArray

It represents an array. You can access to the elements by its position in the array. Based on `TreePVector`.

### ImmutableMap

This class represents a hash map. Based on `HashTreePMap`.

### ImmutableTree

This class represents a binary tree. Based on `TreePSet`

### ImmutableTreeMap

This class represents a binary tree map. Based on `TreePMap`.

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
IO<Unit> echo = Console.print("write your name")
  .andThen(Console.read())
  .flatMap(name -> Console.print("Hello " + name))
  .andThen(Console.print("end"));

echo.unsafeRunSync();
```

## Free

### Free Monad

Finally, after hours of hard coding, I managed to implement a Free monad. This is a highly
unstable implementation and I have implemented because it can be implemented. Inspired
in this [work](https://github.com/xuwei-k/free-monad-java).

```java
Free<IOProgram_, Unit> echo =
  IOProgram.write("what's your name?")
    .andThen(IOProgram.read())
    .flatMap(text -> IOProgram.write("Hello " + text))
    .andThen(IOProgram.write("end"));

Kind<IO_, Unit> foldMap = echo.foldMap(IOInstances.monad(), new IOProgramInterperter());

foldMap.fix(toIO()).unsafeRunSync();
```

### Free Applicative

Similar to Free monad, but allows static analysis without to run the program.

```java
FreeAp<DSL_, Tuple5<Integer, Boolean, Double, String, Unit>> tuple =
    applicative.map5(
        DSL.readInt(2),
        DSL.readBoolean(false),
        DSL.readDouble(2.1),
        DSL.readString("hola mundo"),
        DSL.readUnit(),
        Tuple::of
    ).fix(toFreeAp());

Kind<Id_, Tuple5<Integer, Boolean, Double, String, Unit>> map =
    tuple.foldMap(idTransform(), IdInstances.applicative());

assertEquals(Id.of(Tuple.of(2, false, 2.1, "hola mundo", unit())), map.fix(toId()));
```

## Monad Transformers

### OptionT

Monad Transformer for `Option` type

```java
OptionT<IO_, String> some = OptionT.some(IO.monad(), "abc");

OptionT<IO_, String> map = some.flatMap(value -> OptionT.some(IOInstances.monad(), value.toUpperCase()));

assertEquals("ABC", map.get().fix(toIO()).unsafeRunSync());
```

### EitherT

Monad Transformer for `Either` type

```java
EitherT<IO_, Nothing, String> right = EitherT.right(IO.monad(), "abc");

EitherT<IO_, Nothing, String> map = right.flatMap(value -> EitherT.right(IOInstances.monad(), value.toUpperCase()));

assertEquals("ABC", map.get().fix(toIO()).unsafeRunSync());
```

### StateT

Monad Transformer for `State` type

```java
StateT<IO_, ImmutableList<String>, Unit> state =
  pure("a").flatMap(append("b")).flatMap(append("c")).flatMap(end());

IO<Tuple2<ImmutableList<String>, Unit>> result = state.run(ImmutableList.empty()).fix(toIO());

assertEquals(Tuple.of(listOf("a", "b", "c"), unit()), result.unsafeRunSync());
```

### WriterT

Monad Transformer for `Writer` type

```java
WriterT<Id_, Sequence<String>, Integer> writer =
    WriterT.<Id_, Sequence<String>, Integer>pure(monoid, monad, 5)
    .flatMap(value -> lift(monoid, monad, Tuple.of(listOf("add 5"), value + 5)))
    .flatMap(value -> lift(monoid, monad, Tuple.of(listOf("plus 2"), value * 2)));

assertAll(() -> assertEquals(Id.of(Integer.valueOf(20)), writer.getValue()),
          () -> assertEquals(Id.of(listOf("add 5", "plus 2")), writer.getLog()));
```

### Kleisli

Also I implemented the Kleisli composition for functions that returns monadic values like `Option`, `Try` or `Either`.

```java
Kleisli<Try_, String, Integer> toInt = Kleisli.lift(Try.monad(), Integer::parseInt);
Kleisli<Try_, Integer, Double> half = Kleisli.lift(Try.monad(), i -> i / 2.);

Kind<Try_, Double> result = toInt.compose(half).run("123");

assertEquals(Try.success(61.5), result);
```

## Stream

An experimental version of a `Stream` like scala fs2 project.

```java
StreamOf<IO<?>> streamOfIO = Stream.ofIO();

IO<String> readFile = streamOfIO.eval(IO.of(() -> reader(file)))
  .flatMap(reader -> streamOfIO.iterate(() -> Option.of(() -> readLine(reader))))
  .takeWhile(Option::isPresent)
  .map(Option::get)
  .foldLeft("", (a, b) -> a + "\n" + b)
  .fix(IOOf::toIO)
  .recoverWith(UncheckedIOException.class, cons("--- file not found ---"));

String content = readFile.unsafeRunSync();
```

## Effects

An experimental version of `PureIO` similar to ZIO.

```java
PureIO<Console, Throwable, Unit> echoProgram =
  Console.println("what's your name?")
      .andThen(Console.readln())
      .flatMap(name -> Console.println("Hello " + name));

interface Console {

  <R extends Console> Console.Service<R> console();

  static PureIO<Console, Throwable, String> readln() {
    return PureIO.accessM(env -> env.console().readln());
  }

  static PureIO<Console, Throwable, Unit> println(String text) {
    return PureIO.accessM(env -> env.console().println(text));
  }

  interface Service<R extends Console> {
    PureIO<R, Throwable, String> readln();

    PureIO<R, Throwable, Unit> println(String text);
  }
}
```

Additionally, there are aliases for some PureIO special cases:

```
UIO<T>      =>  PureIO<Void, Void, T>
EIO<E, T>   =>  PureIO<Void, E, T>
Task<T>     =>  PureIO<Void, Throwable, T>
RIO<R, T>   =>  PureIO<R, Throwable, T>
URIO<T>     =>  PureIO<R, Void, T>
```

## Type Classes

With higher kinded types simulation we can implement typeclases.

```
                       Invariant -- Contravariant
                             \
       SemigroupK           Functor -- Comonad
           |               /       \
         MonoidK   _ Applicative   Traverse -- Foldable
           |      /      |      \
       Alternative    Selective  ApplicativeError
                         |        |
  MonadWriter          Monad      |
        \________________|        |
        /          /      \      / 
  MonadState  MonadReader  MonadError_____
                               \          \
                            MonadThrow  Bracket
                                  \      /
                        Defer -- MonadDefer -- Timer
                                     |
                                   Async
                                     |
                                 Concurrent
```

### Functor

```java
public interface Functor<F extends > extends Invariant<F> {

  <T, R> Kind<F, R> map(Kind<F, T> value, Function1<T, R> map);
}
```

### Applicative

```java
public interface Applicative<F> extends Functor<F> {

  <T> Kind<F, T> pure(T value);

  <T, R> Kind<F, R> ap(Kind<F, T> value, Kind<F, Function1<T, R>> apply);

  @Override
  default <T, R> Kind<F, R> map(Kind<F, T> value, Function1<T, R> map) {
    return ap(value, pure(map));
  }
}
```

### Selective

```java
public interface Selective<F> extends Applicative<F> {

  <A, B> Kind<F, B> select(Kind<F, Either<A, B>> value, Kind<F, Function1<A, B>> apply);

  default <A, B, C> Kind<F, C> branch(Kind<F, Either<A, B>> value,
                                      Kind<F, Function1<A, C>> applyA,
                                      Kind<F, Function1<B, C>> applyB) {
    Kind<F, Either<A, Either<B, C>>> abc = map(value, either -> either.map(Either::left));
    Kind<F, Function1<A, Either<B, C>>> fabc = map(applyA, fb -> fb.andThen(Either::right));
    return select(select(abc, fabc), applyB);
  }
}
```

### Monad

```java
public interface Monad<F> extends Selective<F> {

  <T, R> Kind<F, R> flatMap(Kind<F, T> value, Function1<T, ? extends Kind<F, R>> map);

  @Override
  default <T, R> Kind<F, R> map(Kind<F, T> value, Function1<T, R> map) {
    return flatMap(value, map.andThen(this::pure));
  }

  @Override
  default <T, R> Kind<F, R> ap(Kind<F, T> value, Kind<F, Function1<T, R>> apply) {
    return flatMap(apply, map -> map(value, map));
  }

  @Override
  default <A, B> Kind<F, B> select(Kind<F, Either<A, B>> value, Kind<F, Function1<A, B>> apply) {
    return flatMap(value, either -> either.fold(a -> map(apply, map -> map.apply(a)), this::<B>pure));
  }
}
```

### Semigroup

It represents a binary operation over a type.

```java
@FunctionalInterface
public interface Semigroup<T> {
  T combine(T t1, T t2);
}
```

There are instances for strings and integers.

### Monoid

Extends `Semigroup` adding a `zero` operation that represent an identity.

```java
public interface Monoid<T> extends Semigroup<T> {
  T zero();
}
```

There are instances for strings and integers.

### SemigroupK

It represents a `Semigroup` but defined for a kind, like a List, so it extends a regular `Semigroup`.

### MonoidK

The same like `SemigroupK` but for a `Monoid`.

### Invariant

```java
public interface Invariant<F> {
  <A, B> Kind<F, B> imap(Kind<F, A> value, Function1<A, B> map, Function1<B, A> comap);
}
```

### Contravariant

```java
public interface Contravariant<F> extends Invariant<F> {
  <A, B> Kind<F, B> contramap(Kind<F, A> value, Function1<B, A> map);
}
```

### Applicative Error

```java
public interface ApplicativeError<F, E> extends Applicative<F> {

  <A> Kind<F, A> raiseError(E error);

  <A> Kind<F, A> handleErrorWith(Kind<F, A> value, Function1<E, ? extends Kind<F, A>> handler);
}
```

### Monad Error

```java
public interface MonadError<F, E> extends ApplicativeError<F, E>, Monad<F> {

  default <A> Kind<F, A> ensure(Kind<F, A> value, Producer<E> error, Matcher1<A> matcher) {
    return flatMap(value, a -> matcher.match(a) ? pure(a) : raiseError(error.get()));
  }
}
```

### Monad Throw

```java
public interface MonadThrow<F> extends MonadError<F, Throwable> {

}
```

### MonadReader

```java
public interface MonadReader<F, R> extends Monad<F> {

  Kind<F, R> ask();

  default <A> Kind<F, A> reader(Function1<R, A> mapper) {
    return map(ask(), mapper);
  }
}
```

### MonadState

```java
public interface MonadState<F, S> extends Monad<F> {
  Kind<F, S> get();
  Kind<F, Unit> set(S state);

  default Kind<F, Unit> modify(Operator1<S> mapper) {
    return flatMap(get(), s -> set(mapper.apply(s)));
  }

  default <A> Kind<F, A> inspect(Function1<S, A> mapper) {
    return map(get(), mapper);
  }

  default <A> Kind<F, A> state(Function1<S, Tuple2<S, A>> mapper) {
    return flatMap(get(), s -> mapper.apply(s).applyTo((s1, a) -> map(set(s1), x -> a)));
  }
}
```

### MonadWriter

```java
public interface MonadWriter<F, W> extends Monad<F> {

  <A> Kind<F, A> writer(Tuple2<W, A> value);
  <A> Kind<F, Tuple2<W, A>> listen(Kind<F, A> value);
  <A> Kind<F, A> pass(Kind<F, Tuple2<Operator1<W>, A>> value);

  default Kind<F, Unit> tell(W writer) {
    return writer(Tuple.of(writer, unit()));
  }
}
```

### Comonad

```java
public interface Comonad<F> extends Functor<F> {

  <A, B> Kind<F, B> coflatMap(Kind<F, A> value, Function1<Kind<F, A>, B> map);

  <A> A extract(Kind<F, A> value);

  default <A> Kind<F, Kind<F, A>> coflatten(Kind<F, A> value) {
    return coflatMap(value, identity());
  }
}
```

### Foldable

```java
public interface Foldable<F> {

  <A, B> B foldLeft(Kind<F, A> value, B initial, Function2<B, A, B> mapper);

  <A, B> Eval<B> foldRight(Kind<F, A> value, Eval<B> initial, Function2<A, Eval<B>, Eval<B>> mapper);
}
```

### Traverse

```java
public interface Traverse<F> extends Functor<F>, Foldable<F> {

  <G, T, R> Kind<G, Kind<F, R>> traverse(Applicative<G> applicative, Kind<F, T> value,
      Function1<T, ? extends Kind<G, R>> mapper);
}
```

### Semigroupal

```java
public interface Semigroupal<F> {

  <A, B> Kind<F, Tuple2<A, B>> product(Kind<F, A> fa, Kind<F, B> fb);
}
```

### Defer

```java
public interface Defer<F> {

  <A> Kind<F, A> defer(Producer<Kind<F, A>> defer);
}
```

### Bracket

```java
public interface Bracket<F, E> extends MonadError<F, E> {

  <A, B> Kind<F, B> bracket(Kind<F, A> acquire, Function1<A, ? extends Kind<F, B>> use, Consumer1<A> release);
}
```

### MonadDefer

```java
public interface MonadDefer<F> extends MonadThrow<F>, Bracket<F, Throwable>, Defer<F>, Timer<F> {

  default <A> Kind<F, A> later(Producer<A> later) {
    return defer(() -> Try.of(later::get).fold(this::raiseError, this::pure));
  }
}
```

### Async

```java
public interface Async<F> extends MonadDefer<F> {
  
  <A> Kind<F, A> async(Consumer1<Consumer1<Try<A>>> consumer);

}
```

### Timer

```java
public interface Timer<F> {

  Kind<F, Unit> sleep(Duration duration);
}
```

### FunctionK

It represents a natural transformation between two different kinds.

```java
public interface FunctionK<F, G> {
  <T> Kind<G, T> apply(Kind<F, T> from);
}
```

## Optics

```
         __Iso__
        /       \
    Lens        Prism
        \       /
         Optional
```

### Iso

An `Iso` is an optic which converts elements of some type into elements of other type without loss.
In other words, it's **isomorphic**.

```java
Point point = new Point(1, 2);

Iso<Point, Tuple2<Integer, Integer>> pointToTuple = 
  Iso.of(p -> Tuple.of(p.x, p.y), 
         t -> new Point(t.get1(), t.get2()));

assertEquals(point, pointToTuple.set(pointToTuple.get(point)));
```

### Lens

A `Lens` is an optic used to zoom inside a structure. In other words, it's an abstraction of a setter and a getter 
but with immutable objects.

```java
Lens<Employee, String> nameLens = Lens.of(Employee::getName, Employee::withName);

Employee pepe = new Employee("pepe");

assertEquals("pepe", nameLens.get(pepe));
assertEquals("paco", nameLens.get(nameLens.set(pepe, "paco")));
```

We can compose Lenses to get deeper inside. For example, if we add an attribute of type `Address` into `Employee`.
We can create a lens to access the city name of the address of the employee.

```java
Lens<Employee, Address> addressLens = Lens.of(Employee::getAddress, Employee::withAddress);
Lens<Address, String> cityLens = Lens.of(Address::getCity, Address::withCity);
Lens<Employee, String> cityAddressLens = addressLens.compose(cityLens);

Employee pepe = new Employee("pepe", new Address("Madrid"));

assertEquals("Madrid", cityAddressLens.get(pepe));
```

### Prism

A `Prism` is a lossless invertible optic that can see into a structure and optionally find a value. 

```java
Function1<String, Option<Integer>> parseInt = ...; // is a method that only returns a value when the string can be parsed

Prism<String, Integer> stringToInteger = Prism.of(parseInt, String::valueOf);

assertEquals(Option.some(5), stringToInteger.getOption("5"));
assertEquals(Option.none(), stringToInteger.getOption("a"));
assertEquals("5", stringToInteger.reverseGet(5));
```

### Optional

An `Optional` is an optic that allows to see into a structure and getting, setting like a `Lens` an optional find a value like a `Prism`.

```java
Optional<Employee, Address> addressOptional = Optional.of(
  Employee::withAddress, employee -> Option.of(employee::getAddress)
);

Address madrid = new Address("Madrid");
Employee pepe = new Employee("pepe", null);

assertEquals(Option.none(), addressOptional.getOption(pepe));
assertEquals(Option.some(madrid), addressOptional.getOption(addressOptional.set(pepe, madrid)));
```

### Composition

|  | Optional | Prism | Lens | Iso |
|------|------|-------|-------|-------|
| Optional | Optional | Optional | Optional | Optional |
| Prism | Optional | Prism | Optional | Prism |
| Lens | Optional | Optional | Lens | Lens |
| Iso | Optional | Prism | Lens | Iso |

## Equal

This class helps to create readable `equals` methods. An example:

```java
@Override
public boolean equals(Object obj) {
  return Equal.<Data>of()
    .comparing(Data::getId)
    .comparing(Data::getValue)
    .applyTo(this, obj);
}
```

## Stargazers over time

[![Stargazers over time](https://starchart.cc/tonivade/purefun.svg)](https://starchart.cc/tonivade/purefun)

## License

purefun is released under MIT license
