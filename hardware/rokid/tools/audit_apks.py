#!/usr/bin/env python3
"""Fail closed if assembled S3 packages retain logging sinks or unexpected permissions."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SDK = Path(os.environ['ANDROID_HOME']) / 'build-tools' / '37.0.0'
result = {}
for module, package in [('addon', 'com.m57.hermes.rokid.addon.debug'),
                        ('companion', 'com.m57.hermes.rokid.glasses.debug')]:
    apk = ROOT / module / 'build/outputs/apk/debug' / f'{module}-debug.apk'
    permissions = subprocess.check_output([str(SDK / 'aapt'), 'dump', 'permissions', str(apk)], text=True)
    assert f"package: {package}" in permissions, permissions
    assert 'uses-permission:' not in permissions, permissions
    certificates = subprocess.check_output([str(SDK / 'apksigner'), 'verify', '--print-certs', str(apk)], text=True)
    signer = re.search(r'certificate SHA-256 digest: (\w+)', certificates).group(1)
    with zipfile.ZipFile(apk) as z, tempfile.TemporaryDirectory() as tmp:
        abis = sorted({name.split('/')[1] for name in z.namelist() if name.startswith('lib/')})
        dex_count = 0
        for name in z.namelist():
            if re.fullmatch(r'classes\d*\.dex', name):
                dex_count += 1
                dex = Path(tmp) / name
                dex.write_bytes(z.read(name))
                code = subprocess.check_output([str(SDK / 'dexdump'), '-d', str(dex)], text=True)
                assert not re.search(r'invoke[^\n]*Landroid/util/Log;\.(?:v|d|i|w|e|wtf|println):', code), 'Android logging retained'
                assert not re.search(r'invoke[^\n]*(?:PrintStream;|printStackTrace:)', code), 'stdio logging retained'
                assert 'connect by token:' not in code, 'SDK token logging string retained'
        if module == 'addon':
            companion = ROOT / 'companion/build/outputs/apk/debug/companion-debug.apk'
            assert z.read('assets/companion.apk') == companion.read_bytes(), 'Bundled companion differs'
    result[module] = dict(package=package, version='0.1-s3-probe', version_code=1,
                          sha256=hashlib.sha256(apk.read_bytes()).hexdigest(),
                          signer_sha256=signer, bytes=apk.stat().st_size, abis=abis,
                          dex_files=dex_count, android_permissions=[], logging_sink_audit='PASS')
print(json.dumps(result, indent=2))
