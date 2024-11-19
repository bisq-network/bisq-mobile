/**
 * The model graph in that package reflect exactly the Bisq 2 model graph and should be used only
 * for such models.
 * They come with some adjustments to make it KMP compatible and JSON serializable.
 * They contain only fields which are not annotated with @JsonIgnore in Bisq 2.
 *
 * We do only implement those models which are required in the supported use cases.
 * In future we might move it to an independent library project to make them re-usable for other
 * Kotlin based projects using the Bisq 2 REST API.
 */
package network.bisq.mobile.client.replicated_model

