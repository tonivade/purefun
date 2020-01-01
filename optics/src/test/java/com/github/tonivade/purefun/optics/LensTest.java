/*
 * Copyright (c) 2018-2020, Antonio Gabriel Muñoz Conejo <antoniogmc at gmail dot com>
 * Distributed under the terms of the MIT License
 */
package com.github.tonivade.purefun.optics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Objects;

import org.junit.jupiter.api.Test;

import com.github.tonivade.purefun.Equal;

public class LensTest {

  private final Lens<Employee, String> nameLens = Lens.of(Employee::getName, Employee::withName);
  private final Lens<Employee, Address> addressLens = Lens.of(Employee::getAddress, Employee::withAddress);
  private final Lens<Address, String> cityLens = Lens.of(Address::getCity, Address::withCity);
  private final Lens<Employee, String> cityAddressLens = addressLens.compose(cityLens);

  private final Address madrid = new Address("Madrid");
  private final Address alicante = new Address("Alicante");
  private final Employee pepe = new Employee("pepe", madrid);
  private final Employee paco = pepe.withName("paco");

  @Test
  public void lensTest() {
    assertAll(
      () -> assertEquals(pepe.getName(), nameLens.get(pepe)),
      () -> assertEquals(paco, nameLens.set(pepe, paco.getName())),
      () -> assertEquals(madrid.getCity(), cityAddressLens.get(pepe)),
      () -> assertEquals(pepe.withAddress(alicante),
                         cityAddressLens.set(pepe, alicante.getCity()))
    );
  }

  @Test
  public void lensLaws() {
    verifyLaws(nameLens, pepe, paco.getName());
    verifyLaws(cityLens, madrid, alicante.getCity());
    verifyLaws(cityAddressLens, pepe, alicante.getCity());
  }

  private <S, A> void verifyLaws(Lens<S, A> lens, S target, A value) {
    assertAll(
      () -> assertEquals(target, lens.set(target, lens.get(target))),
      () -> assertEquals(value, lens.get(lens.set(target, value)))
    );
  }
}

class Employee {
  private final String name;
  private final Address address;

  public Employee(String name, Address address) {
    this.name = name;
    this.address = address;
  }

  public String getName() {
    return name;
  }

  public Employee withName(String newName) {
    return new Employee(newName, address);
  }

  public Address getAddress() {
    return address;
  }

  public Employee withAddress(Address newAddress) {
    return new Employee(name, newAddress);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, address);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.<Employee>of()
        .comparing(Employee::getName)
        .comparing(Employee::getAddress)
        .applyTo(this, obj);
  }
}

class Address {
  private final String city;

  public Address(String city) {
    this.city = city;
  }

  public String getCity() {
    return city;
  }

  public Address withCity(String newCity) {
    return new Address(newCity);
  }

  @Override
  public int hashCode() {
    return Objects.hash(city);
  }

  @Override
  public boolean equals(Object obj) {
    return Equal.<Address>of()
        .comparing(Address::getCity)
        .applyTo(this, obj);
  }
}
