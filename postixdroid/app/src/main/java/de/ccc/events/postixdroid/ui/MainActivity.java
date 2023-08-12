package de.ccc.events.postixdroid.ui;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.text.Html;
import android.text.format.Formatter;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.ccc.events.postixdroid.AppConfig;
import de.ccc.events.postixdroid.R;
import de.ccc.events.postixdroid.check.OnlineCheckProvider;
import de.ccc.events.postixdroid.check.TicketCheckProvider;

public class MainActivity extends AppCompatActivity implements CustomizedScannerView.ResultHandler, MediaPlayer.OnCompletionListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    public static final int PERMISSIONS_REQUEST_CAMERA = 10001;
    public static final int PERMISSIONS_REQUEST_WRITE_STORAGE = 10002;

    private CustomizedScannerView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private State state = State.SCANNING;
    private Handler timeoutHandler;
    private Handler timerHandler;
    private long timerStart;
    private Map<Integer, MediaPlayer> mediaPlayers = new HashMap<>();
    private TicketCheckProvider checkProvider;
    private AppConfig config;
    private DataWedgeHelper dataWedgeHelper;
    private AlertDialog dialog;

    Runnable timerUpdate = new Runnable() {
        @Override
        public void run() {
            updateTimer(true);
        }
    };

    private JSONObject options;
    private String last_secret;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkProvider = new OnlineCheckProvider(this);
        config = new AppConfig(this);

        setContentView(R.layout.activity_main);

        qrView = (CustomizedScannerView) findViewById(R.id.qrdecoderview);
        qrView.setResultHandler(this);
        qrView.setAutoFocus(config.getAutofocus());
        qrView.setFlash(config.getFlashlight());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSIONS_REQUEST_CAMERA);
        }

        List<BarcodeFormat> formats = new ArrayList<>();
        formats.add(BarcodeFormat.QR_CODE);

        qrView.setFormats(formats);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        buildMediaPlayer(this);

        timeoutHandler = new Handler();
        timerHandler = new Handler();

        resetView();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_logo);
        getSupportActionBar().setDisplayUseLogoEnabled(true);

        dataWedgeHelper = new DataWedgeHelper(this);
        if (dataWedgeHelper.isInstalled()) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_REQUEST_WRITE_STORAGE);
            } else {
                try {
                    dataWedgeHelper.install();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        timerHandler.postDelayed(timerUpdate, 100);
    }


    private BroadcastReceiver scanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("com.symbol.datawedge.data_string")) {
                // Zebra DataWedge
                handleScan(intent.getStringExtra("com.symbol.datawedge.data_string"));
            } else if (intent.hasExtra("SCAN_BARCODE1")) {
                // NewLand
                handleScan(intent.getStringExtra("SCAN_BARCODE1").trim());
            } else if (intent.hasExtra("EXTRA_BARCODE_DECODING_DATA")) {
                // Bluebird
                handleScan(new String(intent.getByteArrayExtra("EXTRA_BARCODE_DECODING_DATA")).trim());
            } else if (intent.hasExtra("decode_rslt")) {
                // Honeywell
                handleScan(intent.getStringExtra("decode_rslt").trim());
            } else if (intent.hasExtra("data")) {
                // Sunmi
                handleScan(intent.getStringExtra("data").trim());
            } else if (intent.hasExtra("scannerdata")) {
                // SEUIC AUTOID
                handleScan(intent.getStringExtra("scannerdata").trim());
            } else if (intent.hasExtra("barocode")) {
                // Intent receiver for LECOM-manufactured hardware scanners
                byte[] barcode = intent.getByteArrayExtra("barocode"); // sic!
                int barocodelen = intent.getIntExtra("length", 0);
                String barcodeStr = new String(barcode, 0, barocodelen);
                handleScan(barcodeStr);
            }
        }

    };

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (config.getCamera()) {
                        qrView.startCamera();
                    }
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
            case PERMISSIONS_REQUEST_WRITE_STORAGE: {
                try {
                    dataWedgeHelper.install();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (config.getCamera()) {
            qrView.setResultHandler(this);
            qrView.startCamera();
            qrView.setAutoFocus(config.getAutofocus());
            resetView();
        } else {
            IntentFilter filter = new IntentFilter();

            // LECOM
            // Active by default
            filter.addAction("scan.rcv.message");

            // Zebra DataWedge
            // Needs manual configuration in DataWedge
            filter.addAction("eu.pretix.SCAN");
            filter.addAction("de.ccc.events.postixdroid.SCAN");

            // Bluebird
            // Active by default
            filter.addAction("kr.co.bluebird.android.bbapi.action.BARCODE_CALLBACK_DECODING_DATA");

            // NewLand
            // Configure broadcast in Quick Setting > Scan Setting > Output Mode > Output via API
            filter.addAction("nlscan.action.SCANNER_RESULT");

            // Honeywell
            // Configure via Settings > Scan Settings > Internal Scanner > Default Profile > Data
            // Processing Settings > Scan to Intent
            filter.addAction("com.honeywell.intent.action.SCAN_RESULT");

            // SEUIC AUTOID, also known as Concept FuturePAD
            // Configure via Scan Tool > Settings > Barcode Send Model > Broadcast
            filter.addAction("com.android.server.scannerservice.broadcast");

            // Sunmi, e.g. L2s
            // Active by default
            // Configure via Settings > System > Scanner Setting > Data Output Mode > Output via Broadcast
            filter.addAction("com.android.scanner.ACTION_DATA_CODE_RECEIVED");

            registerReceiver(scanReceiver, filter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (config.getCamera()) {
            qrView.stopCamera();
        } else {
            unregisterReceiver(scanReceiver);
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        if (config.getCamera()) {
            qrView.resumeCameraPreview(this);
        }
        String s = rawResult.getText();

        handleScan(s);
    }

    public void handleScan(String s) {
        if (dialog != null && dialog.isShowing()) {
            return;
        }

        if (s.equals(lastScanCode) && System.currentTimeMillis() - lastScanTime < 5000) {
            Toast.makeText(this, R.string.doublescan, Toast.LENGTH_SHORT).show();
            return;
        }
        playSound(R.raw.beep);
        resetView();

        lastScanTime = System.currentTimeMillis();
        lastScanCode = s;

        if (config.isConfigured()) {
            handleTicketScanned(s);
        } else {
            handleConfigScanned(s);
        }
    }

    private void handleConfigScanned(String s) {
        try {
            JSONObject jsonObject = new JSONObject(s);
            config.setSessionConfig(jsonObject.getString("url"), jsonObject.getString("key"));
            checkProvider = new OnlineCheckProvider(this);
            displayScanResult(new TicketCheckProvider.CheckResult(
                    TicketCheckProvider.CheckResult.Type.VALID,
                    getString(R.string.config_done)));
        } catch (JSONException e) {
            displayScanResult(new TicketCheckProvider.CheckResult(
                    TicketCheckProvider.CheckResult.Type.ERROR,
                    getString(R.string.err_qr_invalid)));
        }
    }

    private void handleTicketScanned(String s) {
        last_secret = s;
        options = new JSONObject();
        state = State.LOADING;
        findViewById(R.id.tvScanResult).setVisibility(View.GONE);
        findViewById(R.id.tvTimer).setVisibility(View.GONE);
        findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
        new CheckTask().execute(s);
    }

    private void resetView() {
        TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);
        timeoutHandler.removeCallbacksAndMessages(null);
        tvScanResult.setVisibility(View.VISIBLE);
        findViewById(R.id.tvTimer).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.tvScanResult)).setText("");
        findViewById(R.id.rlScanStatus).setBackgroundColor(
                ContextCompat.getColor(this, R.color.scan_result_unknown));

        if (!config.getCamera()) {
            qrView.setVisibility(View.GONE);
        } else {
            qrView.setVisibility(View.VISIBLE);
        }

        if (config.isConfigured()) {
            tvScanResult.setText(R.string.hint_scan);
        } else {
            tvScanResult.setText(R.string.hint_config);
        }
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(String... params) {
            if (params[0].matches("[0-9A-Za-z-]+") || params[0].matches("/(ping|supply|resupply).*")) {
                return checkProvider.check(params[0], options);
            } else {
                return new TicketCheckProvider.CheckResult(TicketCheckProvider.CheckResult.Type.ERROR, getString(R.string.scan_result_invalid));
            }
        }

        @Override
        protected void onPostExecute(TicketCheckProvider.CheckResult checkResult) {
            displayScanResult(checkResult);
        }
    }

    private void updateTimer(boolean repeat) {
        ((TextView) findViewById(R.id.tvTimer)).setText(String.valueOf(Math.round((System.currentTimeMillis() - timerStart) / 1000)));
        if (repeat) {
            timerHandler.postDelayed(timerUpdate, 500);
        }
    }

    private void displayScanResult(TicketCheckProvider.CheckResult checkResult) {
        TextView tvScanResult = findViewById(R.id.tvScanResult);
        timerStart = System.currentTimeMillis();
        updateTimer(false);
        findViewById(R.id.tvTimer).setVisibility(View.VISIBLE);


        state = State.RESULT;
        findViewById(R.id.pbScan).setVisibility(View.INVISIBLE);
        tvScanResult.setVisibility(View.VISIBLE);

        int col = R.color.scan_result_unknown;
        int default_string = R.string.err_unknown;

        switch (checkResult.getType()) {
            case ERROR:
                col = R.color.scan_result_err;
                default_string = R.string.err_unknown;
                playSound(R.raw.error);
                break;
            case INPUT:
                col = R.color.scan_result_err;
                default_string = R.string.err_unknown;
                playSound(R.raw.attention);
                askForInput(checkResult);
                break;
            case CONFIRMATION:
                playSound(R.raw.attention);
                askForConfirmation(checkResult);
                break;
            case VALID:
                col = R.color.scan_result_ok;
                default_string = R.string.scan_result_valid;
                playSound(R.raw.enter);
                break;
        }

        if (checkResult.getMessage() != null) {
            tvScanResult.setText(checkResult.getMessage());
        } else {
            tvScanResult.setText(getString(default_string));
        }
        findViewById(R.id.rlScanStatus).setBackgroundColor(ContextCompat.getColor(this, col));

        timeoutHandler.postDelayed(new Runnable() {
            public void run() {
                resetView();
            }
        }, 10000);
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        // When the beep has finished playing, rewind to queue up another one.
        mp.seekTo(0);
    }

    private void buildMediaPlayer(Context activity) {
        ArrayList<Integer> resourceIds = new ArrayList<>(Arrays.asList(R.raw.enter, R.raw.exit, R.raw.error, R.raw.beep, R.raw.attention, R.raw.dhl));

        for (int r = 0; r < resourceIds.size(); r++) {
            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mediaPlayer.setOnCompletionListener(this);
            // mediaPlayer.setOnErrorListener(this);
            try {
                AssetFileDescriptor file = activity.getResources()
                        .openRawResourceFd(resourceIds.get(r));
                try {
                    mediaPlayer.setDataSource(file.getFileDescriptor(),
                            file.getStartOffset(), file.getLength());
                } finally {
                    file.close();
                }
                mediaPlayer.setVolume(0.10f, 0.10f);
                mediaPlayer.prepare();
                mediaPlayers.put(resourceIds.get(r), mediaPlayer);
            } catch (IOException ioe) {
                mediaPlayer.release();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem checkable = menu.findItem(R.id.action_flashlight);
        checkable.setChecked(config.getFlashlight());

        checkable = menu.findItem(R.id.action_camera);
        checkable.setChecked(config.getCamera());

        checkable = menu.findItem(R.id.action_autofocus);
        checkable.setChecked(config.getAutofocus());

        checkable = menu.findItem(R.id.action_play_sound);
        checkable.setChecked(config.getSoundEnabled());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_clear_config) {
            final AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage(R.string.confirm_clear)
                    .setCancelable(true)
                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            config.resetSessionConfig();
                            resetView();
                            dialogInterface.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
            dialog.show();
            return true;
        } else if (itemId == R.id.action_autofocus) {
            config.setAutofocus(!item.isChecked());
            qrView.setAutoFocus(!item.isChecked());
            item.setChecked(!item.isChecked());
            return true;
        } else if (itemId == R.id.action_request_resupply) {
            handleTicketScanned("/resupply");
            return true;
        } else if (itemId == R.id.action_flashlight) {
            config.setFlashlight(!item.isChecked());
            if (config.getCamera()) {
                qrView.setFlash(!item.isChecked());
            }
            item.setChecked(!item.isChecked());
            return true;
        } else if (itemId == R.id.action_camera) {
            if (config.getCamera()) {
                qrView.stopCamera();
                IntentFilter filter = new IntentFilter();
                // LECOM
                // Active by default
                filter.addAction("scan.rcv.message");

                // Zebra DataWedge
                // Needs manual configuration in DataWedge
                filter.addAction("eu.pretix.SCAN");

                // Bluebird
                // Active by default
                filter.addAction("kr.co.bluebird.android.bbapi.action.BARCODE_CALLBACK_DECODING_DATA");

                // NewLand
                // Configure broadcast in Quick Setting > Scan Setting > Output Mode > Output via API
                filter.addAction("nlscan.action.SCANNER_RESULT");

                // Honeywell
                // Configure via Settings > Scan Settings > Internal Scanner > Default Profile > Data
                // Processing Settings > Scan to Intent
                filter.addAction("com.honeywell.intent.action.SCAN_RESULT");

                // SEUIC AUTOID, also known as Concept FuturePAD
                // Configure via Scan Tool > Settings > Barcode Send Model > Broadcast
                filter.addAction("com.android.server.scannerservice.broadcast");

                // Sunmi, e.g. L2s
                // Active by default
                // Configure via Settings > System > Scanner Setting > Data Output Mode > Output via Broadcast
                filter.addAction("com.android.scanner.ACTION_DATA_CODE_RECEIVED");
                filter.addAction("com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED");

                registerReceiver(scanReceiver, filter);
            } else {
                unregisterReceiver(scanReceiver);
                qrView.setResultHandler(this);
                qrView.startCamera();
                qrView.setAutoFocus(config.getAutofocus());
            }

            config.setCamera(!item.isChecked());
            item.setChecked(!item.isChecked());
            resetView();
            return true;
        } else if (itemId == R.id.action_play_sound) {
            config.setSoundEnabled(!item.isChecked());
            item.setChecked(!item.isChecked());
            return true;
        } else if (itemId == R.id.action_dhl) {
            config.setDHLEnabled(!item.isChecked());
            item.setChecked(!item.isChecked());
            return true;
        } else if (itemId == R.id.action_networkinfo) {
            show_network_info();
            return true;
            /*case R.id.action_search:
                if (config.isConfigured()) {
                    Intent intent = new Intent(this, SearchActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.not_configured, Toast.LENGTH_SHORT).show();
                }
                return true;*/
        } else if (itemId == R.id.action_about) {
            asset_dialog(R.raw.about, R.string.about);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void show_network_info() {
        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        String mac = wm.getConnectionInfo().getMacAddress();

        StringBuilder sb = new StringBuilder();
        sb.append("WiFi IP address: ");
        sb.append(ip);
        sb.append("\nWiFi MAC address: ");
        sb.append(mac);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.networkinfo)
                .setMessage(sb.toString())
                .setPositiveButton(R.string.dismiss, null)
                .create();
        dialog.show();
    }

    private void asset_dialog(@RawRes int htmlRes, @StringRes int title) {
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_about, null, false);
        final AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setPositiveButton(R.string.dismiss, null)
                .create();

        TextView textView = (TextView) view.findViewById(R.id.aboutText);

        String text = "";

        StringBuilder builder = new StringBuilder();
        InputStream fis;
        try {
            fis = getResources().openRawResource(htmlRes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "utf-8"));
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }

            text = builder.toString();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        textView.setText(Html.fromHtml(text));
        textView.setMovementMethod(LinkMovementMethod.getInstance());

        dialog.show();
    }

    protected void askForConfirmation(final TicketCheckProvider.CheckResult result) {
        dialog = new AlertDialog.Builder(this)
                .setMessage(result.getMessage())
                .setCancelable(true)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            options.put(result.getMissingField(), true);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        state = State.LOADING;
                        findViewById(R.id.tvScanResult).setVisibility(View.GONE);
                        findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
                        new CheckTask().execute(last_secret);
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resetView();
                        dialogInterface.dismiss();
                    }
                })
                .create();
        dialog.show();
    }

    protected void askForInput(final TicketCheckProvider.CheckResult result) {
        final View view = LayoutInflater.from(this).inflate(R.layout.dialog_input, null, false);
        final AutoCompleteTextView etInput = (AutoCompleteTextView) view.findViewById(R.id.input_value);

        if (result.getMissingField().startsWith("list_")) {
            try {
                AutoCompleteAdapter adapter = new AutoCompleteAdapter(this, Integer.parseInt(result.getMissingField().substring(5)));
                etInput.setAdapter(adapter);
            } catch (NumberFormatException ignored) {
                ignored.printStackTrace();
            }
        }

        dialog = new AlertDialog.Builder(this)
                .setTitle(result.getMessage())
                .setView(view)
                .setCancelable(true)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        try {
                            options.put(result.getMissingField(), etInput.getText().toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        state = State.LOADING;
                        findViewById(R.id.tvScanResult).setVisibility(View.GONE);
                        findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
                        new CheckTask().execute(last_secret);
                        dialogInterface.dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        resetView();
                        dialogInterface.dismiss();
                    }
                })
                .create();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
        etInput.requestFocus();
    }

    private void playSound(Integer resourceId) {
        // DHL Mode will disable scan-beep and replace successful scan with DHL-beep
        if (config.getDHLEnabled())
            if (resourceId == R.raw.enter) {
                resourceId = R.raw.dhl;
            } else if (resourceId == R.raw.beep) {
                return;
        }

        if (config.getSoundEnabled() && mediaPlayers.containsKey(resourceId)) {
            mediaPlayers.get(resourceId).start();
        }
    }
}
