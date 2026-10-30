package com.m57.hermes.extensiontest;

import android.app.Activity;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.widget.*;
import com.m57.hermes.extension.*;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.Executors;

/** Deliberately no INTERNET permission, chat database, auth or server configuration. */
public class MainActivity extends Activity {
    private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private IExtensionBroker broker;
    private String grant = "", epoch = "", operation = "";
    private long expiry;
    private boolean bound;
    private TextView status, answer;
    private EditText text;
    private File photo;
    private final String core = "com.m57.hermescontrol.dev";
    private final IExtensionCallback callback = new IExtensionCallback.Stub() {
        @Override public void onEvent(Bundle event) {
            main.post(() -> {
                String state = event.getString("status", "UNKNOWN");
                status.setText(state);
                if ("GRANTED".equals(state)) {
                    grant = event.getString("grant", ""); epoch = event.getString("epoch", "");
                    expiry = event.getLong("expiresAtElapsedMs");
                    call(() -> broker.observe(grant, epoch));
                } else if ("DISPLAY".equals(state) && grant.equals(event.getString("grant")) && SystemClock.elapsedRealtime() < expiry) {
                    answer.setText(event.getString("text", ""));
                } else if ("REVOKED".equals(state)) clear();
            });
        }
    };
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            broker = IExtensionBroker.Stub.asInterface(service);
            call(() -> broker.negotiate(1, 0, 3));
        }
        @Override public void onServiceDisconnected(ComponentName name) { broker = null; clear(); status.setText("DISCONNECTED"); }
        @Override public void onBindingDied(ComponentName name) { broker = null; clear(); status.setText("BINDING_DIED"); }
    };
    private interface Request { Bundle run() throws Exception; }
    private void call(Request request) {
        worker.execute(() -> {
            try {
                if (broker == null) throw new IllegalStateException("Connect first");
                Bundle result = request.run();
                main.post(() -> status.setText(result.getString("status", "UNKNOWN")));
            } catch (Exception failure) {
                main.post(() -> status.setText("REFUSED: " + failure.getClass().getSimpleName()));
            }
        });
    }
    private void clear() { grant = ""; epoch = ""; expiry = 0; answer.setText(""); }
    private void button(LinearLayout layout, String title, Runnable action) {
        Button button = new Button(this); button.setText(title); button.setOnClickListener(v -> action.run()); layout.addView(button);
    }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setPadding(24, 64, 24, 48);
        scroll.addView(layout); setContentView(scroll);
        TextView title = new TextView(this); title.setText("S1 · Extension Test Harness\nSynthetic data only; no Rokid hardware"); layout.addView(title);
        status = new TextView(this); status.setText("DISCONNECTED"); layout.addView(status);
        text = new EditText(this); text.setText("Describe the synthetic S1 photograph."); layout.addView(text);
        button(layout, "Connect to Hermes", this::connect);
        button(layout, "Request conversation access", () -> call(() -> broker.openSession(1, 3, callback)));
        button(layout, "Open Hermes to approve / send", () -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage(core); if (launch != null) startActivity(launch);
        });
        button(layout, "Use synthetic JPEG", () -> {
            try {
                photo = new File(getCacheDir(), "synthetic.jpg");
                Bitmap bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap); canvas.drawColor(Color.LTGRAY);
                Paint paint = new Paint(); paint.setColor(Color.BLUE); canvas.drawRect(60, 40, 260, 200, paint);
                try (FileOutputStream out = new FileOutputStream(photo)) { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out); }
                bitmap.recycle(); status.setText("JPEG selected: 320×240, " + photo.length() + " bytes");
            } catch (IOException failure) { status.setText("Fixture failed"); }
        });
        button(layout, "Stage photo / text as draft", () -> { operation = UUID.randomUUID().toString(); stage(); });
        button(layout, "Repeat same operation", this::stage);
        button(layout, "Query operation status", () -> call(() -> broker.getOperationStatus(grant, epoch, operation)));
        button(layout, "Test unsupported version", () -> call(() -> broker.negotiate(99, 0, 3)));
        button(layout, "Close access", () -> call(() -> { broker.closeSession(grant, epoch); return new Bundle(); }));
        answer = new TextView(this); answer.setTextIsSelectable(true); layout.addView(answer);
        main.postDelayed(new Runnable() { public void run() {
            if (expiry > 0 && SystemClock.elapsedRealtime() >= expiry) { clear(); status.setText("EXPIRED"); }
            main.postDelayed(this, 250);
        }}, 250);
    }
    private void stage() {
        String capturedText = text.getText().toString(), capturedGrant = grant, capturedEpoch = epoch, capturedOp = operation;
        File selected = photo;
        call(() -> {
            try (ParcelFileDescriptor fd = selected == null ? null : ParcelFileDescriptor.open(selected, ParcelFileDescriptor.MODE_READ_ONLY)) {
                return broker.stageInput(capturedGrant, capturedEpoch, capturedOp, capturedText, fd);
            }
        });
    }
    private void connect() {
        try {
            int uid = getPackageManager().getApplicationInfo(core, 0).uid;
            if (!SignerTrust.matches(getPackageManager(), uid, core, BuildConfig.CORE_SIGNER)) throw new SecurityException();
            if (!bound) bound = bindService(new Intent().setComponent(new ComponentName(core, "com.m57.hermescontrol.extension.ExtensionService")), connection, BIND_AUTO_CREATE);
            status.setText(bound ? "CONNECTING" : "UNAVAILABLE");
        } catch (Exception failure) { status.setText("UNTRUSTED_OR_ABSENT_CORE"); }
    }
    @Override public void onDestroy() {
        if (bound) unbindService(connection);
        clear(); main.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy();
    }
}
