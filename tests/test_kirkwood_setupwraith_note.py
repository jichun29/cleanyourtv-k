"""The setupwraith notes must carry the second box that reproduces them (#500).

docs/TROUBLESHOOTING.md already documented the casting break and the `.RecoveryActivity` crash loop
from a Chromecast with Google TV HD and a Xiaomi MiTV-MZTU0. A Google TV Streamer (`kirkwood`,
`UTTK.260317.003`) reproduces both and adds two facts the doc was missing: the cast credentials live
only in the receiver's process, so a force-stop loses them just as a reboot does, and the HOME
resolver / role holder can both name the new launcher while the key still opens setupwraith.

The tests read the bullets as bullets, not as grep hits: a sentence that drifts into a neighbouring
bullet would leave the doc just as useless. Run from the repository root:
python3 -m unittest discover -s tests -v
"""
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
DOC = (ROOT / "docs" / "TROUBLESHOOTING.md").read_text(encoding="utf-8")
README = (ROOT / "README.md").read_text(encoding="utf-8")
PROMPT = (ROOT / "PROMPT.md").read_text(encoding="utf-8")
KIRKWOOD = "UTTK.260317.003"


def bullets(text):
    """Split a markdown doc into its `- **Title**` bullets, wrapping lines included."""
    found, current = [], None
    for line in text.splitlines():
        if line.startswith("- **"):
            if current is not None:
                found.append(current)
            current = line
        elif current is not None and (line.startswith("  ") or not line.strip()):
            current += "\n" + line
        else:
            if current is not None:
                found.append(current)
            current = None
    if current is not None:
        found.append(current)
    return found


def bullet_containing(needle):
    matches = [b for b in bullets(DOC) if needle in b]
    assert len(matches) == 1, f"expected one bullet with {needle!r}, found {len(matches)}"
    return re.sub(r"\s+", " ", matches[0])


class KirkwoodNoteTest(unittest.TestCase):
    def setUp(self):
        self.casting = bullet_containing("Casting from a phone fails")
        self.crash = bullet_containing("crash-looped")
        self.home = bullet_containing("still doesn't own Home")

    def test_the_casting_break_is_not_scoped_to_googles_own_hardware(self):
        self.assertIn("Google TV Streamer", self.casting)
        self.assertIn(KIRKWOOD, self.casting)
        self.assertNotIn("hardware so far", self.casting,
                         "the 'so far' wording is what the report corrects")

    def test_the_casting_bullet_names_where_the_provider_lives_on_kirkwood(self):
        # The reporter checked `dumpsys package providers`: there is no separate tvsetup package,
        # the CastInfoProvider classes ship inside the setupwraith APK. That is why disabling the
        # wizard takes casting with it on a box Google never shipped a wizard for.
        self.assertIn("CastInfoProvider", self.casting)
        self.assertIn("setupwraith", self.casting)

    def test_a_receiver_restart_loses_the_credentials_too(self):
        self.assertIn("force-stop", self.casting)
        self.assertIn("mediashell", self.casting)
        self.assertIn("Retry loading factory credentials", self.casting)
        self.assertIn("next reboot", self.casting,
                      "the reboot case is still the common one and must stay")

    def test_the_casting_workaround_still_says_what_to_do(self):
        # The correction must not cost the instruction: enable, wait for the signer, disable again.
        self.assertIn("ClientAuthSigner", self.casting)
        self.assertIn("re-enable setupwraith after each boot", self.casting)
        self.assertIn("cannot change component state", self.casting)

    def test_the_home_bullet_says_resolver_and_role_holder_can_both_lie(self):
        self.assertIn("mFocusedApp", self.home)
        self.assertIn("resolve-activity", self.home)
        self.assertIn("get-role-holders", self.home)
        self.assertIn("RecoveryActivity", self.home)

    def test_the_home_bullet_keeps_the_package_level_way_out(self):
        # Flipping to the component would be wrong: the shell cannot change component state.
        self.assertIn("pm disable-user", self.home)
        self.assertIn("SecurityException", self.home)

    def test_the_crash_loop_bullet_carries_the_kirkwood_trace(self):
        self.assertIn("positionChildAt", self.crash)
        self.assertIn("Task.moveToBack", self.crash)
        self.assertIn("DeadSystemException", self.crash)
        self.assertIn(KIRKWOOD, self.crash)

    def test_the_crash_loop_bullet_keeps_the_way_out(self):
        self.assertIn("pm enable --user 0 com.google.android.apps.tv.launcherx", self.crash)
        self.assertIn("FATAL EXCEPTION IN SYSTEM PROCESS", self.crash)

    def test_the_readme_clause_covers_both_boxes(self):
        clause = next(line for line in README.splitlines() if "costs phone casting" in line)
        self.assertIn("Google TV Streamer", clause)
        self.assertIn("Chromecast", clause)
        self.assertIn("docs/TROUBLESHOOTING.md", clause)

    def test_the_prompt_asks_for_the_focused_app_before_claiming_success(self):
        self.assertIn("mFocusedApp", PROMPT)
        self.assertIn("set-home-activity", PROMPT)
        self.assertIn("query-activities", PROMPT)


if __name__ == "__main__":
    unittest.main()
