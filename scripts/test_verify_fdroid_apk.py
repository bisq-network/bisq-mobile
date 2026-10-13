import importlib.util
import io
import tempfile
import unittest
import zipfile
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path

_SCRIPT = Path(__file__).resolve().parent / "verify-fdroid-apk.py"
_spec = importlib.util.spec_from_file_location("verify_fdroid_apk", _SCRIPT)
v = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(v)

MAP_ID = "67082b849a9b6b330d69bf894795992ff0b39c4675bc6c9c4d7cfd7ec71791aa"
OTHER_MAP_ID = "0000000000000000000000000000000000000000000000000000000000000000"


def r8_marker(map_id: str = MAP_ID) -> bytes:
    return (
        b'~~R8{"backend":"dex","compilation-mode":"release","has-checksums":false,"min-api":24,'
        b'"pg-map-id":"' + map_id.encode() + b'","r8-mode":"full","version":"8.13.19"}'
    )


D8_MARKER = b'~~D8{"backend":"dex","compilation-mode":"release","min-api":24,"version":"8.13.19"}'


def mapping_text(*lines: str, map_id: str | None = MAP_ID) -> str:
    header = ["# compiler: R8", "# compiler_version: 8.13.19"]
    if map_id is not None:
        header.append(f"# pg_map_id: {map_id}")
    return "\n".join([*header, "a.Clean -> a.a:", *lines]) + "\n"


class VerifyFdroidApkTests(unittest.TestCase):
    def setUp(self):
        self._dir = tempfile.TemporaryDirectory()
        self.addCleanup(self._dir.cleanup)
        self.dir = Path(self._dir.name)

    def apk(self, *dex: bytes, arsc: bytes = b"", name: str = "app.apk") -> str:
        path = self.dir / name
        with zipfile.ZipFile(path, "w") as z:
            for index, content in enumerate(dex):
                z.writestr("classes.dex" if index == 0 else f"classes{index + 1}.dex", content)
            z.writestr("resources.arsc", arsc)
        return str(path)

    def mapping(self, text: str, name: str = "mapping.txt") -> str:
        path = self.dir / name
        path.write_text(text, encoding="utf-8")
        return str(path)

    def run_main(self, *args: str) -> tuple[int, str]:
        out, err = io.StringIO(), io.StringIO()
        with redirect_stdout(out), redirect_stderr(err):
            code = v.main(["verify-fdroid-apk.py", *args])
        return code, out.getvalue() + err.getvalue()

    # APKs which R8 did not minify

    def test_unminified_clean_apk_passes_without_a_mapping(self):
        code, output = self.run_main(self.apk(b"Lnetwork/bisq/mobile/Main;" + D8_MARKER))
        self.assertEqual(0, code, output)
        self.assertIn("clean: app.apk", output)

    def test_denied_descriptor_in_dex_is_reported(self):
        code, output = self.run_main(self.apk(b"Lcom/google/firebase/FirebaseApp;"))
        self.assertEqual(1, code, output)
        self.assertIn("com.google.firebase (1 class references)", output)

    def test_google_services_resource_is_reported(self):
        code, output = self.run_main(self.apk(b"clean", arsc=b"..google_app_id.."))
        self.assertEqual(1, code, output)
        self.assertIn("google_app_id resource", output)

    def test_permitted_google_package_passes(self):
        code, output = self.run_main(self.apk(b"Lcom/google/protobuf/Message;Lcom/google/common/base/Joiner;"))
        self.assertEqual(0, code, output)

    # APKs which R8 produced

    def test_r8_apk_without_a_mapping_cannot_be_verified(self):
        code, output = self.run_main(self.apk(b"La/a;" + r8_marker()))
        self.assertEqual(2, code, output)
        self.assertIn("cannot verify app.apk", output)
        self.assertIn(MAP_ID, output)
        self.assertNotIn("clean:", output)

    def test_r8_apk_with_the_mapping_of_another_build_cannot_be_verified(self):
        mapping = self.mapping(mapping_text(map_id=OTHER_MAP_ID))
        code, output = self.run_main("--mapping", mapping, self.apk(b"La/a;" + r8_marker()))
        self.assertEqual(2, code, output)
        self.assertNotIn("clean:", output)

    def test_r8_apk_with_clean_matching_mapping_passes(self):
        mapping = self.mapping(mapping_text("network.bisq.mobile.Main -> b.c:"))
        code, output = self.run_main("--mapping", mapping, self.apk(b"La/a;" + r8_marker()))
        self.assertEqual(0, code, output)
        self.assertIn("clean: app.apk", output)

    def test_renamed_denied_class_is_found_in_the_mapping(self):
        mapping = self.mapping(mapping_text("com.google.firebase.FirebaseApp -> a.b:"))
        code, output = self.run_main("--mapping", mapping, self.apk(b"La/b;" + r8_marker()))
        self.assertEqual(1, code, output)
        self.assertIn("com.google.firebase (1 references in the R8 mapping)", output)

    def test_denied_code_inlined_into_another_class_is_found_in_the_mapping(self):
        mapping = self.mapping(
            mapping_text(
                "network.bisq.mobile.Main -> b.c:",
                "    1:3:void com.google.android.gms.common.internal.Preconditions.checkNotNull(java.lang.Object):12:14 -> a",
                "    1:3:void start():40 -> a",
            )
        )
        code, output = self.run_main("--mapping", mapping, self.apk(b"Lb/c;" + r8_marker()))
        self.assertEqual(1, code, output)
        self.assertIn("com.google.android.gms (1 references in the R8 mapping)", output)

    def test_denied_type_in_a_member_signature_is_found_in_the_mapping(self):
        mapping = self.mapping(
            mapping_text(
                "network.bisq.mobile.Main -> b.c:",
                "    com.google.mlkit.vision.barcode.BarcodeScanner scanner -> a",
                "    void onResult(java.lang.String,com.google.android.play.core.review.ReviewInfo) -> b",
            )
        )
        code, output = self.run_main("--mapping", mapping, self.apk(b"Lb/c;" + r8_marker()))
        self.assertEqual(1, code, output)
        self.assertIn("com.google.mlkit (1 references in the R8 mapping)", output)
        self.assertIn("com.google.android.play (1 references in the R8 mapping)", output)

    def test_package_which_only_starts_like_a_denied_one_passes(self):
        mapping = self.mapping(
            mapping_text(
                "com.google.android.playful.Toy -> a.b:",
                "com.google.firebaseless.Thing -> a.c:",
                "org.example.com.google.firebase.Shaded -> a.d:",
                "com.google.protobuf.Message -> a.e:",
            )
        )
        code, output = self.run_main("--mapping", mapping, self.apk(b"La/b;" + r8_marker()))
        self.assertEqual(0, code, output)

    def test_matching_mapping_is_picked_among_several(self):
        dirty = self.mapping(mapping_text("com.google.firebase.FirebaseApp -> a.b:", map_id=OTHER_MAP_ID), "other.txt")
        clean = self.mapping(mapping_text())
        code, output = self.run_main("--mapping", dirty, "--mapping", clean, self.apk(b"La/a;" + r8_marker()))
        self.assertEqual(0, code, output)

    def test_every_dex_of_a_multidex_apk_must_have_its_mapping(self):
        mapping = self.mapping(mapping_text())
        apk = self.apk(b"La/a;" + r8_marker(), b"La/b;" + r8_marker(OTHER_MAP_ID))
        code, output = self.run_main("--mapping", mapping, apk)
        self.assertEqual(2, code, output)
        self.assertIn(OTHER_MAP_ID, output)

    def test_denied_descriptor_kept_by_r8_is_still_reported_from_the_dex(self):
        mapping = self.mapping(mapping_text())
        apk = self.apk(b"Lcom/google/firebase/FirebaseApp;" + r8_marker())
        code, output = self.run_main("--mapping", mapping, apk)
        self.assertEqual(1, code, output)
        self.assertIn("com.google.firebase (1 class references)", output)

    # Inputs which must not be mistaken for a verdict

    def test_mapping_without_an_id_cannot_be_used(self):
        mapping = self.mapping(mapping_text(map_id=None))
        code, output = self.run_main("--mapping", mapping, self.apk(b"La/a;" + r8_marker()))
        self.assertEqual(2, code, output)
        self.assertIn("cannot use the mapping", output)

    def test_id_in_a_late_comment_is_not_accepted_as_the_mapping_id(self):
        filler = [f"a.C{index} -> a.c{index}:" for index in range(v.MAX_MAPPING_HEADER_LINES)]
        mapping = self.mapping(mapping_text(*filler, f"# pg_map_id: {MAP_ID}", map_id=None))
        code, output = self.run_main("--mapping", mapping, self.apk(b"La/a;" + r8_marker()))
        self.assertEqual(2, code, output)

    def test_missing_mapping_file_cannot_be_used(self):
        code, output = self.run_main("--mapping", str(self.dir / "absent.txt"), self.apk(b"La/a;" + r8_marker()))
        self.assertEqual(2, code, output)

    def test_mapping_for_an_apk_without_r8_marker_cannot_be_verified(self):
        mapping = self.mapping(mapping_text())
        code, output = self.run_main("--mapping", mapping, self.apk(b"La/a;" + D8_MARKER))
        self.assertEqual(2, code, output)
        self.assertNotIn("clean:", output)

    def test_unreadable_apk_is_not_reported_as_dirty(self):
        code, output = self.run_main(str(self.dir / "*.apk"))
        self.assertEqual(2, code, output)
        self.assertIn("cannot read", output)

    def test_no_apk_is_a_usage_error(self):
        code, _ = self.run_main()
        self.assertEqual(2, code)


if __name__ == "__main__":
    unittest.main()
