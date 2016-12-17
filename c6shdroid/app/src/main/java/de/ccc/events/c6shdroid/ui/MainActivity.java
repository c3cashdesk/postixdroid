package de.ccc.events.c6shdroid.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.RawRes;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
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
import java.util.List;

import de.ccc.events.c6shdroid.AppConfig;
import de.ccc.events.c6shdroid.R;
import de.ccc.events.c6shdroid.check.OnlineCheckProvider;
import de.ccc.events.c6shdroid.check.TicketCheckProvider;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler, MediaPlayer.OnCompletionListener {
    public enum State {
        SCANNING, LOADING, RESULT
    }

    public static final int PERMISSIONS_REQUEST_CAMERA = 10001;

    private ZXingScannerView qrView = null;
    private long lastScanTime;
    private String lastScanCode;
    private State state = State.SCANNING;
    private Handler timeoutHandler;
    private MediaPlayer mediaPlayer;
    private TicketCheckProvider checkProvider;
    private AppConfig config;

    private JSONObject options;
    private String last_secret;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkProvider = new OnlineCheckProvider(this);
        config = new AppConfig(this);

        setContentView(R.layout.activity_main);

        qrView = (ZXingScannerView) findViewById(R.id.qrdecoderview);
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
        mediaPlayer = buildMediaPlayer(this);

        timeoutHandler = new Handler();

        resetView();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    qrView.startCamera();
                } else {
                    Toast.makeText(this, R.string.permission_required, Toast.LENGTH_LONG).show();
                    finish();
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        qrView.setResultHandler(this);
        qrView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        qrView.stopCamera();
    }

    @Override
    public void handleResult(Result rawResult) {
        qrView.resumeCameraPreview(this);
        String s = rawResult.getText();
        if (s.equals(lastScanCode) && System.currentTimeMillis() - lastScanTime < 5000) {
            return;
        }
        lastScanTime = System.currentTimeMillis();
        lastScanCode = s;

        if (config.getSoundEnabled()) mediaPlayer.start();
        resetView();

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
        findViewById(R.id.pbScan).setVisibility(View.VISIBLE);
        new CheckTask().execute(s);
    }

    private void resetView() {
        TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);
        timeoutHandler.removeCallbacksAndMessages(null);
        tvScanResult.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvScanResult)).setText("");
        findViewById(R.id.rlScanStatus).setBackgroundColor(
                ContextCompat.getColor(this, R.color.scan_result_unknown));

        if (config.isConfigured()) {
            tvScanResult.setText(R.string.hint_scan);
        } else {
            tvScanResult.setText(R.string.hint_config);
        }
    }

    public class CheckTask extends AsyncTask<String, Integer, TicketCheckProvider.CheckResult> {

        @Override
        protected TicketCheckProvider.CheckResult doInBackground(String... params) {
            if (params[0].matches("[0-9A-Za-z-]+")) {
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

    private void displayScanResult(TicketCheckProvider.CheckResult checkResult) {

        TextView tvScanResult = (TextView) findViewById(R.id.tvScanResult);

        state = State.RESULT;
        findViewById(R.id.pbScan).setVisibility(View.INVISIBLE);
        tvScanResult.setVisibility(View.VISIBLE);

        int col = R.color.scan_result_unknown;
        int default_string = R.string.err_unknown;

        switch (checkResult.getType()) {
            case ERROR:
                col = R.color.scan_result_err;
                default_string = R.string.err_unknown;
                break;
            case INPUT:
                col = R.color.scan_result_err;
                default_string = R.string.err_unknown;
                askForInput(checkResult);
                break;
            case CONFIRMATION:
                askForConfirmation(checkResult);
                break;
            case VALID:
                col = R.color.scan_result_ok;
                default_string = R.string.scan_result_valid;
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

    private MediaPlayer buildMediaPlayer(Context activity) {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnCompletionListener(this);
        // mediaPlayer.setOnErrorListener(this);
        try {
            AssetFileDescriptor file = activity.getResources()
                    .openRawResourceFd(R.raw.beep);
            try {
                mediaPlayer.setDataSource(file.getFileDescriptor(),
                        file.getStartOffset(), file.getLength());
            } finally {
                file.close();
            }
            mediaPlayer.setVolume(0.10f, 0.10f);
            mediaPlayer.prepare();
            return mediaPlayer;
        } catch (IOException ioe) {
            mediaPlayer.release();
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        MenuItem checkable = menu.findItem(R.id.action_flashlight);
        checkable.setChecked(config.getFlashlight());

        checkable = menu.findItem(R.id.action_autofocus);
        checkable.setChecked(config.getAutofocus());

        checkable = menu.findItem(R.id.action_play_sound);
        checkable.setChecked(config.getSoundEnabled());

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear_config:
                config.resetSessionConfig();
                resetView();
                return true;
            case R.id.action_autofocus:
                config.setAutofocus(!item.isChecked());
                qrView.setAutoFocus(!item.isChecked());
                item.setChecked(!item.isChecked());
                return true;
            case R.id.action_flashlight:
                config.setFlashlight(!item.isChecked());
                qrView.setFlash(!item.isChecked());
                item.setChecked(!item.isChecked());
                return true;
            case R.id.action_play_sound:
                config.setSoundEnabled(!item.isChecked());
                item.setChecked(!item.isChecked());
                return true;
            /*case R.id.action_search:
                if (config.isConfigured()) {
                    Intent intent = new Intent(this, SearchActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, R.string.not_configured, Toast.LENGTH_SHORT).show();
                }
                return true;*/
            case R.id.action_about:
                asset_dialog(R.raw.about, R.string.about);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
        final AlertDialog dialog = new AlertDialog.Builder(this)
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
        final EditText etInput = (EditText) view.findViewById(R.id.input_value);
        final AlertDialog dialog = new AlertDialog.Builder(this)
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
}
