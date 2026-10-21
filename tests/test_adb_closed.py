"""tools/adb-closed.py tells an open port from a closed one, on 127.0.0.1.

Run from the repository root: python3 -m unittest discover -s tests -v
"""
from pathlib import Path
import socket
import subprocess
import sys
import unittest


ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / "tools" / "adb-closed.py"


def check(*args):
    return subprocess.run([sys.executable, "-I", str(SCRIPT), *args],
                          capture_output=True, text=True, timeout=30)


class AdbClosedTest(unittest.TestCase):
    def test_open_port(self):
        with socket.socket() as server:
            server.bind(("127.0.0.1", 0))
            server.listen(1)
            result = check("127.0.0.1", str(server.getsockname()[1]))
        self.assertEqual(result.returncode, 1, result.stderr)
        self.assertIn("still open", result.stderr)

    def test_closed_port(self):
        # Take a free port and let it go again: nothing listens there, so the connection is refused.
        with socket.socket() as probe:
            probe.bind(("127.0.0.1", 0))
            port = probe.getsockname()[1]
        result = check("127.0.0.1", str(port))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("is closed: the TV refused", result.stdout)

    def test_bad_arguments(self):
        self.assertEqual(check("127.0.0.1", "not-a-port").returncode, 2)
        self.assertEqual(check("a", "b", "c").returncode, 2)


if __name__ == "__main__":
    unittest.main()
