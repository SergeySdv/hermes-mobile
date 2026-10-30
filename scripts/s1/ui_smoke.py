#!/usr/bin/env python3
"""Repeatable real UI/Binder smoke on the explicitly selected S1 emulator.
Precondition: install both S1 debug APKs, connect Hermes to the loopback fixture
and enable its existing 'Stay connected in background' preference.
"""
import json
import os
from pathlib import Path
import re
import subprocess
import time
import urllib.request
import xml.etree.ElementTree as ET

ADB = [os.path.expanduser('~/Library/Android/sdk/platform-tools/adb'), '-s', 'emulator-5554']
OUT = Path(os.environ.get('S1_EVIDENCE', '/tmp/hermes-s1-evidence/final-smoke'))
OUT.mkdir(parents=True, exist_ok=True)
CORE = 'com.m57.hermescontrol.dev/com.m57.hermescontrol.MainActivity'
ADDON = 'com.m57.hermes.extensiontest/.MainActivity'
TEXT = 'Describe the synthetic S1 photograph.'
ANSWER = 'S1 TEST BACKEND: a blue rectangle on a grey background. No real model was called.'

def adb(*args):
    return subprocess.check_output(ADB + list(args), stderr=subprocess.STDOUT, timeout=20)

def tree():
    result = adb('shell', 'uiautomator', 'dump', '/sdcard/s1-smoke.xml')
    if b'dumped' not in result: raise RuntimeError(result.decode())
    return ET.fromstring(adb('shell', 'cat', '/sdcard/s1-smoke.xml'))

def tap_node(node):
    b = list(map(int, re.findall(r'\d+', node.attrib['bounds'])))
    adb('shell', 'input', 'tap', str((b[0]+b[2])//2), str((b[1]+b[3])//2))

def nodes_text(root):
    return [n.get('text', '') for n in root.iter('node')]

def wait_text(value, seconds=20):
    end = time.monotonic() + seconds
    while time.monotonic() < end:
        root = tree()
        if value in nodes_text(root): return root
        time.sleep(.3)
    raise AssertionError('Missing UI text: ' + value)

def tap(value):
    root = tree()
    for n in root.iter('node'):
        if value.lower() in (n.get('text','').lower(), n.get('content-desc','').lower()) and n.get('enabled') == 'true':
            tap_node(n); return
    raise AssertionError('Missing enabled control: ' + value)

def launch(component):
    adb('shell', 'am', 'start', '-n', component)
    time.sleep(1)

def capture(name):
    root = tree()
    (OUT / (name+'.xml')).write_bytes(ET.tostring(root))
    (OUT / (name+'.png')).write_bytes(adb('exec-out', 'screencap', '-p'))
    return root

def counts():
    with urllib.request.urlopen('http://127.0.0.1:19119/__s1_counts', timeout=3) as response:
        return json.load(response)

def step(value): print(value, flush=True)

def main():
    step('Prepare a fresh conversation; no data clearing')
    adb('shell', 'am', 'force-stop', 'com.m57.hermes.extensiontest')
    launch(CORE)
    tap('New Chat')
    time.sleep(1)
    before = counts()
    launch(ADDON)
    tap('Connect to Hermes'); wait_text('OK')
    tap('Stage photo / text as draft'); wait_text('REVOKED')
    capture('01-without-consent')
    tap('Request conversation access'); wait_text('AWAITING_USER')
    tap('Open Hermes to approve / send')
    root = wait_text('Allow extension for this conversation?')
    capture('02-consent')
    for node in root.iter('node'):
        if node.get('checkable') == 'true' and node.get('checked') == 'false': tap_node(node)
    tap('Allow')
    launch(ADDON)
    wait_text('OK')
    tap('Use synthetic JPEG')
    tap('Stage photo / text as draft'); wait_text('AWAITING_USER')
    tap('Repeat same operation'); wait_text('AWAITING_USER')
    step('Real Binder staging + duplicate receipt obtained')
    tap('Open Hermes to approve / send')
    root = capture('03-composer-once')
    inputs = [n for n in root.iter('node') if n.get('class') == 'android.widget.EditText']
    assert sum(n.get('text') == TEXT for n in inputs) == 1
    assert sum(n.get('content-desc') == 'Remove attachment' for n in root.iter('node')) == 1
    staged = counts(); assert staged == before, (before, staged)
    tap('Send')
    wait_text(ANSWER)
    capture('04-hermes-answer')
    after = counts()
    assert after['prompt.submit'] == before['prompt.submit'] + 1, (before, after)
    assert after['image.attach_bytes'] == before['image.attach_bytes'] + 1, (before, after)
    launch(ADDON)
    wait_text(ANSWER)
    capture('05-addon-answer')
    step('Confirmed: one upload, one prompt; answer in both applications')
    tap('Test unsupported version'); wait_text('UNSUPPORTED_VERSION')
    capture('06-version-rejected')
    tap('Close access')
    time.sleep(.5)
    tap('Repeat same operation'); wait_text('REVOKED')
    assert ANSWER not in nodes_text(tree())
    capture('07-revoked-and-cleared')
    assert counts() == after
    (OUT/'counts.json').write_text(json.dumps({'before':before,'staged':staged,'sent':after}, indent=2)+'\n')
    step('PASS: consent, draft, dedup, zero pre-send requests, one send, projection, version mismatch and revoke')

if __name__ == "__main__":
    main()
