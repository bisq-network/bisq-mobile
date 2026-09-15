#!/usr/bin/env python3
"""Fails if a build resolves a Google dependency nobody has reviewed.

This is the gate that `verify-fdroid-apk.py` cannot be. That one reads package roots out of a
dex, and a dex descriptor carries no package/class boundary, so it can only match the first
segment after `com.google` and has to approve broad roots like `api` and `apps`. A proprietary
artifact published under one of those would pass it.

An artifact coordinate is the unit a licence actually attaches to, so that is what is checked
here: every `com.google*` coordinate on the shipped runtime classpath must be listed below with
the licence it was reviewed under. A new dependency fails until someone looks at it, whatever
package names it uses. The APK check stays as the second net, for anything that arrives without
a declared coordinate.

Licences are as declared by each artifact's own POM where it declares one, and by the project
otherwise. Both Apache-2.0 and BSD-3-Clause are free software and fine for F-Droid; the point of
recording which is that the next person does not have to re-derive it.

Usage:  gradlew :apps:x:dependencies --configuration <cfg> | verify-fdroid-dependencies.py -
        verify-fdroid-dependencies.py <dependency-report.txt>
"""

import re
import sys

COORDINATE = re.compile(r"\b(com\.google(?:code)?(?:\.[a-z0-9_]+)*):([A-Za-z0-9_.\-]+)")

# group:artifact -> licence it was reviewed under.
ALLOWED = {
    "com.google.guava:guava": "Apache-2.0",
    "com.google.guava:failureaccess": "Apache-2.0",
    "com.google.guava:listenablefuture": "Apache-2.0",
    "com.google.protobuf:protobuf-java": "BSD-3-Clause",
    "com.google.protobuf:protobuf-java-util": "BSD-3-Clause",
    # Build-time protoc compiler; never packaged into an APK.
    "com.google.protobuf:protoc": "BSD-3-Clause",
    # Every com.google.{api,apps,cloud,geo,logging,longrunning,rpc,shopping,type} package in the
    # node APK comes from this one artifact, which is why those roots look so broad in the APK
    # check and why reviewing them there is hopeless.
    "com.google.api.grpc:proto-google-common-protos": "Apache-2.0",
    "com.google.code.gson:gson": "Apache-2.0",
    "com.google.code.findbugs:jsr305": "Apache-2.0",
    "com.google.errorprone:error_prone_annotations": "Apache-2.0",
    "com.google.auto.value:auto-value-annotations": "Apache-2.0",
    # Guava drags this one in. Its group id contains a digit, which is exactly the sort of thing
    # an ad-hoc grep misses and a real check does not.
    "com.google.j2objc:j2objc-annotations": "Apache-2.0",
    "com.google.accompanist:accompanist-drawablepainter": "Apache-2.0",
    # grpc's annotations artifact. Unrelated to com.google.android.gms despite the group id.
    "com.google.android:annotations": "Apache-2.0",
    "com.googlecode.libphonenumber:libphonenumber": "Apache-2.0",
}


def unreviewed(report: str) -> list[str]:
    found = {f"{m.group(1)}:{m.group(2)}" for m in COORDINATE.finditer(report)}
    return sorted(c for c in found if c not in ALLOWED)


def main(argv: list[str]) -> int:
    if len(argv) != 2:
        print(__doc__, file=sys.stderr)
        return 2

    report = sys.stdin.read() if argv[1] == "-" else open(argv[1], encoding="utf-8").read()
    if not report.strip():
        print("::error::empty dependency report, nothing was checked", file=sys.stderr)
        return 2

    problems = unreviewed(report)
    for coordinate in problems:
        print(f"::error::{coordinate} is not in the reviewed dependency allowlist")

    if problems:
        print(
            "\nA Google dependency reached the shipped classpath without review. Check its "
            "licence: if it is free software, add it to ALLOWED in this script with that "
            "licence recorded; if it is not, it cannot ship on F-Droid and the dependency that "
            "pulled it in has to change.",
            file=sys.stderr,
        )
        return 1

    found = {f"{m.group(1)}:{m.group(2)}" for m in COORDINATE.finditer(report)}
    print(f"{len(found)} Google coordinates on this classpath, all reviewed:")
    for coordinate in sorted(found):
        print(f"  {coordinate}  ({ALLOWED[coordinate]})")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
