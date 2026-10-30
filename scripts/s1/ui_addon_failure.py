#!/usr/bin/env python3
"""Stop, disable and uninstall only the synthetic harness on emulator-5554.
Leaves the harness uninstalled; reinstall the known APK before IPC tests.
"""
import json
import time
import ui_smoke as ui

# Establish a real live grant before killing the client process.
ui.launch(ui.CORE)
ui.tap('New Chat')
ui.launch(ui.ADDON)
ui.tap('Connect to Hermes'); ui.wait_text('OK')
ui.tap('Request conversation access'); ui.wait_text('AWAITING_USER')
ui.tap('Open Hermes to approve / send')
root = ui.wait_text('Allow extension for this conversation?')
for node in root.iter('node'):
    if node.get('checkable') == 'true' and node.get('checked') == 'false': ui.tap_node(node)
ui.tap('Allow')
ui.launch(ui.ADDON); ui.wait_text('OK')

for state, command in [
    ('stopped', ('shell', 'am', 'force-stop', 'com.m57.hermes.extensiontest')),
    ('disabled', ('shell', 'pm', 'disable-user', '--user', '0', 'com.m57.hermes.extensiontest')),
    ('uninstalled', ('uninstall', 'com.m57.hermes.extensiontest')),
]:
    print(ui.adb(*command).decode(), flush=True)
    ui.launch(ui.CORE)
    assert 'Revoke extension' not in ui.nodes_text(ui.tree())
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
