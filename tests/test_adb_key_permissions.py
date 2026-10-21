"""tools/tv.py keeps its ADB private key readable only by the account that made it.

The key is a credential: a TV that has accepted it gives a shell to anyone holding it, with no
"Allow USB debugging?" prompt. adb_shell's keygen writes the file with a plain open(), so without
help it lands at 0666 & ~umask (0644 under the usual umask) and every other account on the machine
can read it. Google's own adb keeps ~/.android/adbkey at 0600.

keygen is stubbed here so the test needs no adb_shell and no TV: the stub writes the key exactly the
way the real one does (plain open(), no chmod), which is the behaviour tv.py has to make up for.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
import os
from pathlib import Path
import shutil
import stat
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]

# Enough of adb_shell for `import tv` to succeed. keygen writes the key the way the real one does.
STUB_DEVICE = "class AdbDeviceTcp:\n    def __init__(self, *a, **k):\n        pass\n"
STUB_SIGNER = "class PythonRSASigner:\n    def __init__(self, *a, **k):\n        pass\n"
STUB_KEYGEN = (
    "def keygen(filepath):\n"
    "    # adb_shell/auth/keygen.py: a plain open(), no chmod.\n"
    "    with open(filepath, 'wb') as f:\n"
    "        f.write(b'-----BEGIN PRIVATE KEY-----\\nstub\\n-----END PRIVATE KEY-----\\n')\n"
    "    with open(filepath + '.pub', 'wb') as f:\n"
    "        f.write(b'stub pub\\n')\n"
)


@unittest.skipIf(os.name == "nt", "POSIX permission bits; the chmod is a no-op on Windows")
class AdbKeyPermissionsTest(unittest.TestCase):
    def make_key(self):
        """Run tv.get_signer() against a copy of tools/tv.py in a throwaway tree; return the key path."""
        tmp = Path(tempfile.mkdtemp())
        self.addCleanup(shutil.rmtree, tmp, ignore_errors=True)

        tools = tmp / "tools"
        tools.mkdir(parents=True)
        shutil.copy2(ROOT / "tools" / "tv.py", tools / "tv.py")

        stub = tmp / "stub" / "adb_shell"
        (stub / "auth").mkdir(parents=True)
        (stub / "__init__.py").write_text("")
        (stub / "adb_device.py").write_text(STUB_DEVICE)
        (stub / "auth" / "__init__.py").write_text("")
        (stub / "auth" / "keygen.py").write_text(STUB_KEYGEN)
        (stub / "auth" / "sign_pythonrsa.py").write_text(STUB_SIGNER)

        # HOME points at the empty tree, so the stock ~/.android/adbkey branch cannot be taken and the
        # user's real adb key is never read or touched by this test.
        env = dict(os.environ, TV_HOST="127.0.0.1", HOME=str(tmp),
                   PYTHONPATH=os.pathsep.join([str(tmp / "stub"), str(tools)]))
        result = subprocess.run([sys.executable, "-c", "import tv; tv.get_signer()"],
                                cwd=tmp, env=env, capture_output=True, text=True, timeout=60)
        self.assertEqual(result.returncode, 0, result.stderr)
        return tools / "adbkey" / "adbkey"

    def test_private_key_is_not_readable_by_group_or_others(self):
        key = self.make_key()
        self.assertTrue(key.exists(), "tv.py generated no key")
        mode = stat.S_IMODE(key.stat().st_mode)
        self.assertEqual(mode & 0o077, 0,
                         f"the ADB private key is {oct(mode)}: any other account on this machine can "
                         f"read it and take a shell on every TV that has accepted it")

    def test_key_directory_is_private(self):
        directory = self.make_key().parent
        mode = stat.S_IMODE(directory.stat().st_mode)
        self.assertEqual(mode & 0o077, 0, f"{directory.name}/ is {oct(mode)}, expected owner only")


if __name__ == "__main__":
    unittest.main()
