"""Start the unmodified pinned Hermes gateway with isolated test configuration."""
import json
import os
from pathlib import Path
import sys

os.umask(0o077)
def secret(name):
    value = (Path("/run/secrets") / name).read_text().strip()
    if not value:
        raise SystemExit("Missing nonempty secret: " + name)
    return value

os.environ["OPENAI_API_KEY"] = secret("openai_api_key")
home = Path(os.environ["HERMES_HOME"])
home.mkdir(parents=True, exist_ok=True)
config = {
    "model": {"default": "gpt-4.1-mini", "provider": "openai-api"},
    "dashboard": {"basic_auth": {
        "username": "s2", "password": secret("dashboard_password"),
        "secret": secret("dashboard_secret"),
    }},
    "terminal": {"backend": "local", "cwd": "/workspace"},
}
# JSON is valid YAML; secrets remain exclusively in the private data volume.
(home / "config.yaml").write_text(json.dumps(config))
os.chdir("/opt/hermes")
os.execv(sys.executable, [sys.executable, "-m", "hermes_cli.main", "serve",
                         "--host", "0.0.0.0", "--port", "9119", "--isolated"])
