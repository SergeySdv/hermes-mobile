#!/usr/bin/env python3
"""Stop, disable and uninstall only the synthetic harness on emulator-5554.
Leaves the harness uninstalled; reinstall the known APK before IPC tests.
"""
import json
import time
import ui_smoke as ui

for state, command in [
    ('stopped', ('shell', 'am', 'force-stop', 'com.m57.hermes.extensiontest')),
    ('disabled', ('shell', 'pm', 'disable-user', '--user', '0', 'com.m57.hermes.extensiontest')),
    ('uninstalled', ('uninstall', 'com.m57.hermes.extensiontest')),
]:
    print(ui.adb(*command).decode(), flush=True)
    ui.launch(ui.CORE)
    ui.tap('New Chat')
    time.sleep(1)
    before = ui.counts()
    root = ui.tree()
    editor = next(n for n in root.iter('node') if n.get('class') == 'android.widget.EditText')
    ui.tap_node(editor)
    ui.adb('shell', 'input', 'text', 'S1%sharness%s' + state)
    ui.tap('Send')
    ui.wait_text(ui.ANSWER)
    ui.adb('shell', 'input', 'keyevent', 'KEYCODE_BACK')
    ui.capture(state)
    after = ui.counts()
    assert after['prompt.submit'] == before['prompt.submit'] + 1, (before, after)
    assert after['image.attach_bytes'] == before['image.attach_bytes'], (before, after)
    (ui.OUT / (state + '-counts.json')).write_text(json.dumps({'before': before, 'after': after}, indent=2)+'\n')
    print('PASS: ordinary Hermes text send with harness ' + state, flush=True)
