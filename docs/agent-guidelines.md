# Agent working method

How agents (and humans) should reason about changes in this repo. Architecture and testing conventions live in [architecture.md](architecture.md) and [testing/README.md](testing/README.md); this file covers judgment: how to establish intent, how to fix bugs, and what to check before a change lands.

---

## Intent authority

1. Explicit maintainer decisions (issue, PR discussion, or direct instruction).
2. Written docs in `docs/`, the module READMEs, and the bisq2 desktop behaviour the mobile apps mirror.
3. Current code and tests, as evidence of existing behaviour only.

Tests are not specifications. A test may be AI-generated, outdated, or coupled to an implementation detail. Do not infer a domain requirement because a test asserts it, and do not change production behaviour just to make a test pass without establishing that the tested behaviour is intended. When code, docs, and tests disagree, describe the conflict and propose which one should change instead of resolving it by majority.

## Root cause before fix

For non-trivial bugs, security-sensitive changes, and anything with several edge cases, do not patch the first place the symptom appears. Before changing production code be able to state:

- the incorrect behaviour and the invariant it violates;
- where the invalid state is first introduced, and why that is possible;
- which other behaviours and workarounds are consequences of the same cause;
- what code becomes unnecessary once the cause is fixed;
- whether the proposed fix leaves another path to the same invalid state.

Prefer removing the cause over adding handling. A sound fix often makes code simpler because a former edge case becomes impossible. Remove obsolete workarounds only after confirming they are not protecting an independent trust boundary, persisted legacy data, or backward compatibility with older nodes and peers.

Treat the reported issue, failing test, or review comment as evidence of a problem, not as an accurate description of its cause. Form at least one competing hypothesis and look for evidence that would disprove the leading one. Growing conditional complexity during a fix is a signal that the model of the problem is wrong.

Aim for the smallest sound solution, not the smallest diff. When the sound fix materially exceeds the requested scope (architecture, protocol, persisted data, public API, compatibility), stop and present both the narrow mitigation and the sound fix with their residual risks; scope decisions belong to the maintainer. When a narrow mitigation is chosen, label it as such and state the remaining defect.

In the final report, state the root cause and the causal chain (cause, invalid state, consequences, symptom, fix). Distinguish confirmed causes from hypotheses, and say plainly when the cause is still unknown.

## Architecture ownership

- Put behaviour in the component that owns the rule, data, or lifecycle, not in whichever caller already has the needed services injected.
- Callbacks, listeners, suppliers, and service locators can hide reverse dependencies and cycles. Adding an indirection does not by itself preserve a boundary.
- Avoid duplicate sources of truth. When a projection of state is needed (for example a Model derived from a Dto), make its derivation and validity explicit.
- Do not let an invariant depend accidentally on message arrival order, optional callbacks, or another component's initialization side effects. See [architecture.md](architecture.md) for the layer rules and the MVIP shape.

## Security and compatibility

Every change touching the following gets an explicit compatibility and security pass in the report: trust boundaries between app and trusted node, externally supplied input, serialization and proto changes, persisted data and settings, networking and Tor, pairing and permissions, cryptographic operations, concurrency, and error handling.

- Assume validation can be reached through paths other than the normal UI flow.
- Connect clients talk to older trusted nodes and nodes talk to older peers. Gate new API usage through capability probes (see [Feature availability services](architecture.md#feature-availability-services)), never through version strings, and fail closed.
- Raise security findings found outside the task scope. Do not silently fix them as part of an unrelated change.

## Verification and honesty

- Never claim that tests, builds, or commands ran unless they did. Report verification that could not be performed and why.
- Run the narrowest useful check first (module-scoped Gradle command from [TESTING.md](TESTING.md#commands)), broader checks when scope or risk justify it.
- When verification fails, find the cause before changing production behaviour or weakening the test. Do not delete, disable, or loosen a valid test to get a green build.
- Preserve maintainer work. Never revert, overwrite, stash, or clean up changes you did not make unless asked.
- Commits and pushes are done by the maintainer unless explicitly delegated for the task at hand. Do not add AI-agent attribution or co-author trailers to commits, PRs, or issues.

## Review feedback

When given feedback prefixed with `reviewer claims:`, treat each claim as a hypothesis. Inspect the code, decide whether it is correct, partially correct, obsolete, already fixed, or duplicated. Fix the underlying issue rather than mechanically applying the reviewer's proposed patch, keep one logical change per valid claim, and explain in the reply why an invalid claim does not apply. Stop once all claims are evaluated.

## Internationalization

Add or change strings only in the English base file `mobile.properties`. Never hand-edit translated locale files; translations are produced by an automated process. See [translations.md](translations.md).
