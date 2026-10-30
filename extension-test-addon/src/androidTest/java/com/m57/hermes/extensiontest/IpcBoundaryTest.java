package com.m57.hermes.extensiontest;

import android.content.*;
import android.os.*;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.m57.hermes.extension.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import java.util.concurrent.*;

@RunWith(AndroidJUnit4.class)
public class IpcBoundaryTest {
    @Test public void actualBinderNegotiationAndUnissuedGrantAreFailClosed() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CompletableFuture<IExtensionBroker> ready = new CompletableFuture<>();
        ServiceConnection connection = new ServiceConnection() {
            public void onServiceConnected(ComponentName name, IBinder binder) { ready.complete(IExtensionBroker.Stub.asInterface(binder)); }
            public void onServiceDisconnected(ComponentName name) {}
        };
        assertTrue(context.bindService(new Intent().setComponent(new ComponentName("com.m57.hermescontrol.dev",
            "com.m57.hermescontrol.extension.ExtensionService")), connection, Context.BIND_AUTO_CREATE));
        try {
            IExtensionBroker broker = ready.get(10, TimeUnit.SECONDS);
            Bundle negotiated = broker.negotiate(1, 0, 255);
            assertEquals("OK", negotiated.getString("status"));
            assertEquals(3, negotiated.getInt("capabilities"));
            assertEquals(5 * 1024 * 1024, negotiated.getInt("maxJpegBytes"));
            assertEquals("UNSUPPORTED_VERSION", broker.negotiate(2, 0, 3).getString("status"));
            assertEquals("REVOKED", broker.stageInput("invented", "epoch", "op", "text", null).getString("status"));
            assertEquals("REVOKED", broker.observe("invented", "epoch").getString("status"));
            assertEquals("REVOKED", broker.getOperationStatus("invented", "epoch", "op").getString("status"));
            assertEquals("LIMIT_EXCEEDED", broker.stageInput("invented", "epoch", "op",
                new String(new char[40000]).replace('\0', 'x'), null).getString("status"));
            assertFalse(negotiated.containsKey("token"));
            assertFalse(negotiated.containsKey("session_id"));
            assertFalse(SignerTrust.matches(context.getPackageManager(), android.os.Process.myUid(),
                context.getPackageName(), "0000000000000000000000000000000000000000000000000000000000000000"));
        } finally { context.unbindService(connection); }
    }
}
