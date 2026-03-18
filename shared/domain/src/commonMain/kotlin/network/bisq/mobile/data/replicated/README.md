# Overview data package

## Replicated package

### Naming conventions

As we use the Bisq Easy domain classes and enums in the node mode we have to distinguish the names to avoid conflicts and confusion (
distinguishing by package name would pollute the code with very long fully qualified names).

The `network.bisq.mobile.domain.data.replicated` package contains classes and enums which are replicas from the Bisq Easy sides data transfer
objects.
In Bisq Easy those are post-fixed with `Dto` and reflect the same package structure as the domain models and enums. Enums are also post-fixed
with `Dto`.
Those are pure value objects and replicate the domain models without any mutable fields or domain methods.
On the Bisq Easy side those are only used for data transfer.

In case we use those data as value objects, we use `VO` for classes and `Enum` (due lack of a better generic postfix) for enums.
In case those data are used as immutable data source for models, we keep the `Dto` post-fix, as they are only used for data transfer and
not accessed directly from client code (data access via delegation inside the associated model).

For mapping between the domain objects and the value objects or enums we use the `Mappings` class with its specific mappings for all
replicated value objects.
For models we do not provide the `toBisq2Model` method as we don't need that usually. Instead we lookup in the domain service for the
relevant Bisq Easy model.

To avoid to pollute the value objects we use `Extensions` postfix for objects containing extensions to a value object and `Factory` for util
methods to create a value object. `Utils` for other utility methods.

### Immutable data

#### Data transfer objects

In case the data is only used for data transfer we use the `Dto` post-fix.

#### Value objects

Immutable data which is in domain context is using the `VO` post-fix.
All value objects consist of immutable fields only and have no domain methods. They can though contains initial values for mutual fields.
The updates for those fields are handled by models and webservice events.

## Presentation data

In the `network.bisq.mobile.domain.data.replicated.presentation` package we maintain data transfer objects and models which concern the
presentation aspect.
Those would be ideally in the presentation module itself, but as we do not have the code base ported to provide the content for that data,
we provide those data from the client/node side and by that they need to be hosted in the domain module due dependency restrictions.

As those presentation models are usually list items we use `Item` as par of the name (e.g. `OfferItemPresentationModel`).

Some of the fields might be removed over time when more code is ported to the KMP layer. So maybe that 'misfit' will get removed over time.
But that might come with considerable effort and not sure if feasible as some of those data are composed from data sources of multiple
domains,
thus would require specialized backend API to serve the required domain data.

### Mutable data

#### Models

For mutable data we use the `Model` postfix and pass the value object into it (e.g. `OfferItemPresentationModel` gets the
`OfferItemPresentationDto`).
We do not expose the value object but provide delegate fields to its fields. The mutual/observable fields are provided as `StateFlow`.
The initial value for those fields are set from value object and later updated by the relevant services.
In case of the node we observe the domain observable fields and apply the changes.
For the client we get updates via websocket events.

Model classes can contain also domain methods or util fields.

#### Data types

#### Optional/nullable data

Optional data is provided as nullable type in Kotlin. On the Bisq Easy side Optional is used in the domain layer instead of nullable annotated
fields.
In case the dto is not reflecting a domain model, we use nullable annotation.
For back-end provided nullable/optional fields we can use `@JsonInclude(JsonInclude.Include.NON_EMPTY)` to send those data at all.
The values will be considered null in such cases on the client side.

#### Base 64 encoding for byte arrays

If byte arrays are in the source domain object we encode it with Base 64 encoding and post-fix the field with `Encoded`.
