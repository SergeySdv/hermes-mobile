#!/usr/bin/env python3
"""Loopback-only S1 fixture, NOT a Hermes server. Existing APK HTTP/WS clients connect here.
Counts side effects without logging payloads, credentials or images. Synthetic data only.
"""
import base64
import hashlib
import http.server
import json
import struct
import threading
import uuid

COUNTS = {"image.attach_bytes": 0, "prompt.submit": 0}
LOCK = threading.Lock()
ANSWER = "S1 TEST BACKEND: a blue rectangle on a grey background. No real model was called."

class Handler(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"
    def log_message(self, *_):
        pass
    def respond(self, value, mime="application/json", code=200):
        data = value.encode() if isinstance(value, str) else json.dumps(value).encode()
        self.send_response(code)
        self.send_header("Content-Type", mime)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)
    def do_GET(self):
        if self.headers.get("Upgrade", "").lower() == "websocket":
            return self.websocket()
        path = self.path.split("?")[0]
        if path == "/":
            return self.respond('<script>window.__HERMES_SESSION_TOKEN__="s1-synthetic-token";</script>', "text/html")
        if path == "/__s1_counts":
            with LOCK: return self.respond(dict(COUNTS))
        if path == "/api/status":
            return self.respond({"status":"ok", "auth_required":False, "version":"S1 MOCK", "agent_running":True})
        if path == "/api/profiles": return self.respond({"profiles":[], "active":"default"})
        if path == "/api/profiles/active": return self.respond({"name":"default"})
        if path.endswith("/messages") or path.endswith("/timeline"):
            return self.respond({"messages":[], "total":0, "has_more":False})
        if path == "/api/sessions": return self.respond({"sessions":[], "total":0})
        return self.respond({})
    def do_POST(self):
        self.rfile.read(int(self.headers.get("Content-Length", "0")))
        return self.respond({})
    def frame(self, value):
        data = json.dumps(value).encode()
        header = bytes([129, len(data)]) if len(data) < 126 else bytes([129,126]) + struct.pack("!H",len(data))
        self.wfile.write(header + data)
        self.wfile.flush()
    def event(self, kind, session, payload):
        self.frame({"jsonrpc":"2.0", "method":"event", "params":{"type":kind,"session_id":session,"payload":payload}})
    def websocket(self):
        key = self.headers["Sec-WebSocket-Key"]
        accept = base64.b64encode(hashlib.sha1((key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").encode()).digest()).decode()
        self.send_response(101)
        self.send_header("Upgrade","websocket"); self.send_header("Connection","Upgrade")
        self.send_header("Sec-WebSocket-Accept",accept); self.end_headers()
        self.event("gateway.ready", None, {})
        try:
            while True:
                header = self.rfile.read(2)
                if len(header) != 2 or header[0] & 15 == 8: return
                size = header[1] & 127
                if size == 126: size = struct.unpack("!H", self.rfile.read(2))[0]
                if size == 127: size = struct.unpack("!Q", self.rfile.read(8))[0]
                if size > 8*1024*1024: return
                mask = self.rfile.read(4) if header[1] & 128 else None
                data = self.rfile.read(size)
                if mask: data = bytes(x ^ mask[i % 4] for i,x in enumerate(data))
                if header[0] & 15 == 9:
                    self.wfile.write(bytes([138,len(data)]) + data)
                    self.wfile.flush()
                    continue
                if header[0] & 15 != 1: continue
                request = json.loads(data)
                method = request.get("method", ""); params = request.get("params", {})
                session = params.get("session_id") or "s1-" + str(uuid.uuid4())
                result = {}
                if method in ("session.create", "session.resume"):
                    result = {"session_id":session,"stored_session_id":session,"info":{"model":"S1 MOCK","provider":"test"}}
                elif method == "session.list": result = {"sessions":[]}
                elif method == "commands.list": result = {"commands":[]}
                elif method == "models.list": result = {"models":[]}
                elif method == "session.events.since": result = {"events":[]}
                elif method == "image.attach_bytes":
                    with LOCK: COUNTS[method] += 1
                    result = {"attached":True,"path":"/synthetic/s1.jpg"}
                elif method == "prompt.submit":
                    with LOCK: COUNTS[method] += 1
                    result = {"status":"streaming"}
                if "id" in request: self.frame({"jsonrpc":"2.0","id":request["id"],"result":result})
                if method == "prompt.submit":
                    self.event("message.start",session,{})
                    self.event("message.delta",session,{"text":ANSWER})
                    self.event("message.complete",session,{"text":ANSWER,"completion_id":str(uuid.uuid4()),"finish_reason":"stop"})
        except (OSError, ValueError, KeyError):
            return

if __name__ == "__main__":
    print("S1 mock gateway: 127.0.0.1:19119; no model calls", flush=True)
    http.server.ThreadingHTTPServer(("127.0.0.1", 19119), Handler).serve_forever()
