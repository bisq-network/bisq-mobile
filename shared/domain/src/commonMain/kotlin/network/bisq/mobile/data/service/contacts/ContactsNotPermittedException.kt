package network.bisq.mobile.data.service.contacts

/**
 * The paired trusted node runs a version that has the contacts API, but this pairing was not
 * granted the CONTACTS permission — only possible for a legacy explicit (non-grantAll) grant
 * that predates the permission; re-pairing fixes it.
 *
 * Its own type rather than a status code, so presenters in `:shared:presentation` can tell this
 * apart from a dropped connection without depending on the client app's HTTP types. Only the
 * Bisq Connect flavour can produce it — the node flavour has no permission layer.
 */
class ContactsNotPermittedException : Exception("The paired trusted node did not grant permission for contacts")
