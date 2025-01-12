# Overview about the replicated package

## Naming conventions

The `network.bisq.mobile.domain.replicated` package contains classes and enums which are replicas from the Bisq 2 sides data transfer
objects.
In Bisq 2 those are post-fixed with `Dto` and reflect the same package structure as the domain models and enums. Enums are also post-fixed
with `Dto`.
Those are pure value object and replicate the domain models without any mutable fields or domain methods.
On the Bisq 2 side those are only used for the data transfer purpose.
In Bisq mobile we use those objects also as value objects in the domain, thus we post-fix them with `VO` for classes and `Enum` (due lack of
a better generic postfix) for enums.
As we use the Bisq 2 domain classes and enums in the node mode we have to distinguish the names to avoid conflicts and confusion (
distinguishing by package name would cause very long fully qualified names).

For mapping between the domain objects and the value objects or enums we use the Mappings class with its specific Mappings for all objects.

In case we use extension methods or properties we use an Kotlin file post-fixed with `Extensions` (e.g. `DirectionExtensions`) to keep the
value objects clean.

## Base 64 encoding for byte arrays

If byte arrays are in the source domain object we encode it with Base 64 encoding and post-fix the field with `Encoded`.

## Immutable data

### Value objects

All value objects consist of immutable fields only and have no domain methods.

## List items

For value objects designed to be displayed in lists, we post-fix them with `ListItem`.
As we provide those list items from the backend in case of the client mode and they have a mirrored dto version on the http-api module in
Bisq 2.
Those usually are rather view models but as they get delivered by the backend we consider them part of the domain layer (sort of enriched
value objects).

## Mutable data

### Models

If mutable data are needed we use `Model` postfix and pass the value object into it (e.g. `BisqEasyTradeModel`).
Model classes can contain also domain methods and delegate or util fields.

As those classes have no mirrored version on the backend we do not keep them in the `replicated` package but in the `model` package.

For observable data we use `FlowState`.

## Keep the value objects clean

To avoid to pollute the value objects we use `Extensions` postfix for objects containing extensions to a value object and `Factory` for util
methods to create a value object. `Utils` for other utility methods. 

