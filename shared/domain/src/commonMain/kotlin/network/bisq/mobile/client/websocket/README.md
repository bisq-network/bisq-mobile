# WebSocket overview

We use websockets for getting data pushed from the back-end.

## Rest Api proxy

As in case of Tor we want to avoid establishing unnecessary new connections due the delay and risk that a connection attempt fails, we
use the existing websocket connection to tunnel the Rest API requests to the backend server which unpacks it and does a local Rest Api
request and returns the result in the websocket connection.
The request blocks until the response is provided.

Note: For POST and PATCH there is a unresolved bug on the backend, so we use the Rest Api endpoints directly for now.

## Topics

Topics are used to subscribe for certain types of events.

## Subscription

To subscribe to some data stream we use a subscription ID and topic. Optional a parameter can be added.
The response to the `SubscriptionRequest` will contain an optional payload and optional errorMessage.
The payload is a json string and will be deserialized by the actual subscriber.
The type of the payload is defined by the Subscription as well as provided in the Topic as a `kotlin.reflect.KType`.

## Events

When a data update on the back-end is propagated to the client, we receive a `WebSocketEvent`.
This contains the payload, the `ModificationType` and a sequence number. The sequence number is to ensure order of events.
`ModificationType` gives information if data was added, removed or replaced.
We use a list for the payloads to support multiple changes being batched in one event.






