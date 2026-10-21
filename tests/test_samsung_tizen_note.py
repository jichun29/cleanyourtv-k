"""A Samsung Tizen set is documented as out of reach for this repo's tools, not as a supported TV.

#441 offered one owner's reconnaissance of an AU8000, and the answer to "can this repo clean my
Samsung?" is no: there is no ADB on retail firmware, so there is no `pm disable-user`, no shell, no
screenshot, and no way to take Home. The ports are pinned open or closed here because that split is
the whole argument, and because a later edit that moves 5555 into the open column would turn an
honest "not supported" into a wrong one. The README has to keep sending Tizen readers to the page;
otherwise the only answer they get is "different system" with nowhere to go.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import re
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
PAGE = ROOT / "docs" / "devices" / "samsung-tizen.md"
README = ROOT / "README.md"


def page():
    return PAGE.read_text(encoding="utf-8")


def ports():
    """The port table: port number -> state, from the three-column rows."""
    found = {}
    for line in page().split("\n"):
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if len(cells) == 3 and cells[0].isdigit():
            found[cells[0]] = cells[1].lower()
    return found


class SamsungTizenPageTest(unittest.TestCase):
    def test_page_names_the_set_it_describes(self):
        text = page()
        self.assertIn("UN50AU8000FXZA", text, "a Tizen reader has to be able to tell whether it is their set")
        self.assertIn("Tizen", text)
        self.assertIn("#441", text, "the report the page is transcribed from")

    def test_no_adb_and_no_sdb(self):
        table = ports()
        self.assertEqual(table.get("5555"), "closed", f"5555 is not the ADB port here: {table}")
        self.assertEqual(table.get("26101"), "closed", f"26101 is closed with Developer Mode off: {table}")

    def test_the_remote_ports_are_the_open_ones(self):
        table = ports()
        for port in ("8001", "8002"):
            self.assertEqual(table.get(port), "open", f"{port} carries the remote: {table}")

    def test_the_page_says_why_the_lists_cannot_be_applied(self):
        text = page()
        self.assertRegex(text, r"(?i)no\s+`pm disable-user`", "the page has to say there is no disable path")
        self.assertRegex(text, r"(?i)no screenshot|no capture api", "the page has to say a script cannot see the screen")
        self.assertRegex(text, r"(?i)no home takeover|no third-party app can take the home",
                         "the page has to say the launcher half does not carry over")

    def test_the_page_keeps_its_limits(self):
        self.assertRegex(page(), r"(?i)one set, one firmware",
                         "one owner's report must not read like a platform-wide claim")

    def test_readme_sends_tizen_readers_to_the_page(self):
        text = README.read_text(encoding="utf-8")
        self.assertIn("docs/devices/samsung-tizen.md", text,
                      "the Supported TVs paragraph is where a Samsung owner looks")
        self.assertRegex(text, r"(?i)samsung \(tizen\)")


class PageLinksTest(unittest.TestCase):
    def test_readme_links_resolve(self):
        text = README.read_text(encoding="utf-8")
        for target in sorted(set(re.findall(r"\]\((docs/devices/[^)#]+)\)", text))):
            with self.subTest(target=target):
                self.assertTrue((ROOT / target).is_file(), f"README points at a missing file: {target}")


if __name__ == "__main__":
    unittest.main()
