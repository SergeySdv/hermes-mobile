#!/usr/bin/env python3
"""Inspect compiled release classes after :app:compileReleaseJavaWithJavac.
Structural test only: does not claim release APK signing/R8/device validation.
"""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

root = Path(__file__).resolve().parents[2]
java = root / 'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes'
kotlin = root / 'app/build/intermediates/built_in_kotlinc/release/compileReleaseKotlin/classes'
javap = str(Path(os.environ['JAVA_HOME']) / 'bin/javap')
config = java / 'com/m57/hermescontrol/BuildConfig.class'
broker = kotlin / 'com/m57/hermescontrol/extension/ExtensionBroker.class'
constants = subprocess.check_output([javap, '-constants', str(config)], text=True)
assert 'boolean DEBUG = false;' in constants
assert 'String EXTENSION_TEST_SIGNER = "";' in constants
bytecode = subprocess.check_output([javap, '-c', str(broker)], text=True)
trusted = bytecode.split('boolean trusted(android.content.Context, int);', 1)[1].split('public final void attach', 1)[0]
assert 'iconst_0' in trusted and 'ireturn' in trusted
assert 'SignerTrust' not in trusted and 'getPackageManager' not in trusted
classes = list(java.rglob('*.class')) + list(kotlin.rglob('*.class'))
assert classes
for path in classes:
    assert '/com/m57/hermes/extensiontest/' not in str(path)
    content = path.read_bytes()
    for marker in [b'S1 TEST BACKEND:', b's1-synthetic-token', b'127.0.0.1:19119']:
        assert marker not in content, (str(path), marker)
print(json.dumps({
    'result': 'PASS', 'release_class_count': len(classes),
    'debug': False, 'test_signer': '', 'trust_method': 'constant false',
    'harness_classes_and_mock_markers': 'absent',
    'build_git_sha': re.search(r'GIT_SHA = "([^"]+)"', constants).group(1),
    'build_config_class_sha256': hashlib.sha256(config.read_bytes()).hexdigest(),
    'broker_class_sha256': hashlib.sha256(broker.read_bytes()).hexdigest(),
}, indent=2))
