#!/usr/bin/env python3
"""Install a separately signed synthetic harness, assert rejection, restore correct APK.
Arguments: wrong-signer.apk trusted-harness.apk. Only emulator-5554 is targeted.
"""
import hashlib
import json
from pathlib import Path
import sys
import ui_smoke as ui

wrong, trusted = map(Path, sys.argv[1:])
for path in (wrong, trusted):
    if not path.is_file(): raise FileNotFoundError(path)
try:
    ui.adb('uninstall', 'com.m57.hermes.extensiontest')
    ui.adb('install', str(wrong))
    before = ui.counts()
    ui.launch(ui.ADDON)
    ui.tap('Connect to Hermes')
    ui.wait_text('REFUSED: SecurityException')
    ui.capture('wrong-signer')
    assert ui.counts() == before
    (ui.OUT/'signer-test.json').write_text(json.dumps({
        'wrong_apk_sha256': hashlib.sha256(wrong.read_bytes()).hexdigest(),
        'trusted_apk_sha256': hashlib.sha256(trusted.read_bytes()).hexdigest(),
        'result': 'REFUSED: SecurityException', 'backend_counts_unchanged': True,
    }, indent=2)+'\n')
    print('PASS: actual wrong-signer APK refused by Hermes Binder; no backend side effects')
finally:
    ui.adb('uninstall', 'com.m57.hermes.extensiontest')
    ui.adb('install', str(trusted))
