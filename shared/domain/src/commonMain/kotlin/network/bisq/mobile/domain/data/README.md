# Overview data package

## Replicated package

### Naming conventions

The `network.bisq.mobile.domain.data.replicated` package contains classes and enums which are replicas from the Bisq 2 sides data transfer
objects.
In Bisq 2 those are post-fixed with `Dto` and reflect the same package structure as the domain models and enums. Enums are also post-fixed
with `Dto`.
Those are pure value objects and replicate the domain models without any mutable fields or domain methods.
On the Bisq 2 side those are only used for data transfer.

In Bisq mobile we use those objects also as value objects in the domain, thus we post-fix them with `VO` for classes and `Enum` (due lack of
a better generic postfix) for enums.
As we use the Bisq 2 domain classes and enums in the node mode we have to distinguish the names to avoid conflicts and confusion (
distinguishing by package name would pollute the code with very long fully qualified names).

For mapping between the domain objects and the value objects or enums we use the `Mappings` class with its specific mappings for all
replicated value objects.

In case we use extension methods or properties we use a Kotlin file post-fixed with `Extensions` (e.g. `DirectionExtensions`) to keep the
value objects clean.

### Base 64 encoding for byte arrays

If byte arrays are in the source domain object we encode it with Base 64 encoding and post-fix the field with `Encoded`.

### Immutable data

#### Value objects

All value objects consist of immutable fields only and have no domain methods. They can though contains initial values for mutual fields.
The updates for those fields are handled by models and webservice events.

##### Keep the value objects clean

To avoid to pollute the value objects we use `Extensions` postfix for objects containing extensions to a value object and `Factory` for util
methods to create a value object. `Utils` for other utility methods.

## Presentation data

In the `network.bisq.mobile.domain.data.presentation` we maintain value objects and models which concern the presentation aspect.
Those would be ideally in the presentation module itself, but as we do not have the code base ported to provide those values and it would
be considerable effort to do that, we provide those data from the client/node side and by that they need to be hosted in the domain module
due dependency restrictions.

Some of the fields might be removed over time when more code is ported in a KMP compatible way. So maybe that 'misfit' will get removed over
time.

### Mutable data

#### Models

For mutable data we use the `Model` postfix and pass the value object into it (e.g. `OfferItemPresentationModel` gets the
`OfferItemPresentationDto`).
We do not expose the value object but provide delegate fields to its fields. The mutual/observable fields are provided as `StateFlow`.
The initial value for those fields are set from value object and later updated by the relevant services.
In case of the node we observe the domain observable fields and apply the changes.
For the client we get updates via websocket events.

Model classes can contain also domain methods or util fields.


