"""Pins the QM7 (75QM751G) device note and the casting bullet it exists for.

The note is one owner's same-day report (#453), not a list: it has to keep sending people to the
QM6K list, name the seven packages that owner turned off, and stay honest about how little was
tested. Whitespace is flattened before matching because both files wrap their prose.
"""
import re
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / "docs" / "devices" / "tcl-75qm751g-qm7.md"
TROUBLE = ROOT / "docs" / "TROUBLESHOOTING.md"
EXAMPLES = ROOT / "examples" / "README.md"
LINK = "../docs/devices/tcl-75qm751g-qm7.md"


def flat(text):
    return re.sub(r"\s+", " ", text)


def bullets(path):
    """Top-level `- **Title** ...` bullets of a markdown file, as (title, flattened text)."""
    blocks, current = [], None
    for line in path.read_text(encoding="utf-8").split("\n"):
        if line.startswith("- **"):
            if current:
                blocks.append(current)
            current = [line]
        elif current is not None and (not line.strip() or line.startswith("  ")):
            current.append(line)
        elif current is not None:
            blocks.append(current)
            current = None
    if current:
        blocks.append(current)
    out = []
    for block in blocks:
        text = flat(" ".join(block))
        out.append((text.split("**")[1], text))
    return out


def links(text, base=None):
    targets = re.findall(r"\]\(([^)\s]+)\)", text)
    if base is None:
        return targets
    return [str((base / target).resolve()) for target in targets if not target.startswith("http")]



class Qm751gNoteTest(unittest.TestCase):
    def setUp(self):
        self.page = PAGE.read_text(encoding="utf-8")
        self.flat = flat(self.page)

    def test_names_the_firmware_not_the_retail_name(self):
        self.assertIn("75QM751G", self.flat)
        self.assertIn("`G08`", self.flat)
        self.assertIn("tcl9618-user 14 UTT2.250416.001 AU14 release-keys", self.flat)
        self.assertIn("Smart TV Pro", self.flat, "the generic model string belongs in the table")
        self.assertIn("shared across TCL ranges", self.flat)

    def test_sends_the_reader_to_the_qm6k_list(self):
        self.assertIn("not a list of its own", self.flat)
        target = (ROOT / "examples" / "tcl-qm6k-google-tv" / "debloat.txt").resolve()
        self.assertIn(str(target), links(self.page, PAGE.parent),
                      "the note must link the list it defers to")
        self.assertTrue(target.is_file())

    def test_records_the_seven_extra_packages(self):
        for package in (
            "com.netflix.ninja",
            "com.netflix.tokenmanager",
            "com.google.android.katniss",
            "com.google.android.tv.remote.service",
            "com.tcl.airplay2",
            "com.mediatek.AirplayAPK",
            "com.mediatek.airplaydaemon",
        ):
            with self.subTest(package=package):
                self.assertIn(package, self.flat)
        self.assertIn("com.tcl.repairguide", self.flat)
        self.assertIn("Unknown package", self.flat, "repairguide is absent here, not disabled")

    def test_keeps_the_scope_honest(self):
        self.assertIn("same day", self.flat)
        self.assertIn("lived with", self.flat)
        self.assertIn("One set, one firmware, one session", self.flat)
        self.assertIn("## Test limits", self.page)

    def test_setupwraith_guard_and_wifi_owner(self):
        self.assertIn("dumpsys wifi", self.flat)
        self.assertIn("cname=", self.flat)
        self.assertIn("FORCE_SETUPWRAITH=1", self.flat)
        self.assertIn("Ethernet", self.flat, "the guard looks at the saved network, not the interface")


class CastingBulletTest(unittest.TestCase):
    def test_hung_receiver_bullet_exists(self):
        found = [text for title, text in bullets(TROUBLE) if title.startswith("Casting disappears")]
        self.assertEqual(len(found), 1, "expected exactly one 'Casting disappears' bullet")
        bullet = found[0]
        for needle in (
            "force-stop",
            "com.google.android.apps.mediashell",
            "Done announcing",
            "WargService",
            "CLOSE_WAIT",
            "not the setupwraith case",
        ):
            with self.subTest(needle=needle):
                self.assertIn(needle, bullet)
        self.assertIn("devices/tcl-75qm751g-qm7.md", bullet)

    def test_credentials_case_kept_apart(self):
        titles = [title for title, _ in bullets(TROUBLE)]
        self.assertTrue(any(t.startswith("Casting from a phone fails") for t in titles),
                        "the setupwraith/CastInfoProvider bullet must stay")
        credentials = [t for title, t in bullets(TROUBLE) if title.startswith("Casting from a phone fails")][0]
        self.assertIn("CastInfoProvider", credentials)


class IndexTest(unittest.TestCase):
    def test_examples_readme_points_here(self):
        text = EXAMPLES.read_text(encoding="utf-8")
        self.assertIn(LINK, links(text), "the docs/devices/ index has to list the page")
        index = [l for l in text.split("\n") if l.startswith("- [TCL `75QM751G`]")]
        self.assertEqual(len(index), 1, "exactly one index bullet for this model")
        self.assertIn("QM7", index[0])
        self.assertIn(LINK, index[0])
        rows = [l for l in text.split("\n") if l.startswith("| `tcl-qm6k-google-tv/`")]
        self.assertEqual(len(rows), 1)
        self.assertIn("QM7 (2024)", rows[0], "the QM6K row has to mention the QM7 run")


if __name__ == "__main__":
    unittest.main()
