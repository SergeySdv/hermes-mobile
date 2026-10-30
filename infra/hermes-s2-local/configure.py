"""Local-only secret entry; no values are printed or passed as command arguments."""
import getpass
import os
from pathlib import Path
import secrets

os.umask(0o077)
root = Path(__file__).resolve().parent / ".local"
root.mkdir(mode=0o700, exist_ok=True)
os.chmod(root, 0o700)
key_path = root / "openai_api_key"
if key_path.exists():
    print("OpenAI key already configured; leaving it unchanged.")
else:
    key = getpass.getpass("OpenAI API key (hidden): ").strip()
    if not key:
        raise SystemExit("Empty key: no configuration written.")
    with key_path.open("x") as f:
        os.chmod(key_path, 0o600)
        f.write(key + "\n")
for name in ("dashboard_password", "dashboard_secret"):
    path = root / name
    if not path.exists():
        with path.open("x") as f:
            os.chmod(path, 0o600)
            f.write(secrets.token_urlsafe(32) + "\n")
# Compose file secrets are bind mounts; their uid override is not portable.
# Parent 0700 protects them on the host; read-only files are readable by UID 10000.
for name in ("openai_api_key", "dashboard_password", "dashboard_secret"):
    os.chmod(root / name, 0o444)
print("Secrets saved locally. Dashboard username: s2. No secret values printed.")
