package de.ccc.events.postixdroid.ui;

import static androidx.core.content.ContextCompat.getExternalFilesDirs;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import de.ccc.events.postixdroid.R;

public class DataWedgeHelper {
    private Context ctx;
    private int dwprofileVersion = 1;

    public DataWedgeHelper(Context ctx) {
        this.ctx = ctx;
    }

    public boolean isInstalled() {
        try {
            PackageManager pm = ctx.getPackageManager();
            pm.getPackageInfo("com.symbol.datawedge", 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private File getStagingDirectory() {
        File[] externalStorageDirectory = getExternalFilesDirs(ctx, null);
        File stagingDirectory = new File(externalStorageDirectory[0].getPath(), "/datawedge_import");
        if (!stagingDirectory.exists()) {
            stagingDirectory.mkdirs();
        }
        return stagingDirectory;
    }

    private void copyAllStagedFiles() throws IOException {
        File stagingDirectory = getStagingDirectory();
        File[] filesToStage = stagingDirectory.listFiles();
        File outputDirectory = new File ("/enterprise/device/settings/datawedge/autoimport");
        if (!outputDirectory.exists())
            outputDirectory.mkdirs();
        if (filesToStage.length == 0)
            return;
        for (int i = 0; i < filesToStage.length; i++)
        {
            //  Write the file as .tmp to the autoimport directory
            try {
                InputStream in = new FileInputStream(filesToStage[i]);
                File outputFile = new File(outputDirectory, filesToStage[i].getName() + ".tmp");
                OutputStream out = new FileOutputStream(outputFile);

                copyFile(in, out);

                //  Rename the temp file
                String outputFileName = outputFile.getAbsolutePath();
                outputFileName = outputFileName.substring(0, outputFileName.length() - 4);
                File fileToImport = new File(outputFileName);
                outputFile.renameTo(fileToImport);
                //set permission to the file to read, write and exec.
                fileToImport.setExecutable(true, false);
                fileToImport.setReadable(true, false);
                fileToImport.setWritable(true, false);
                Log.i("DataWedge", "DataWedge profile written successfully.");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
        out.flush();
        in.close();
        out.close();
    }

    public void install() throws IOException {
        install(false);
    }

    public void install(boolean force) throws IOException {
        File stgfile = new File(getStagingDirectory(), "dwprofile_postix.db");
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        if (!force && stgfile.exists() && prefs.getInt("__dwprofile_installed_version", 0) >= dwprofileVersion) {
            return;
        }
        FileOutputStream stgout = new FileOutputStream(stgfile);

        InputStream rawin = ctx.getResources().openRawResource(R.raw.dwprofile);
        copyFile(rawin, stgout);

        // Legacy DataWedge Profile import
        copyAllStagedFiles();

        // New DataWedge Profile Import (available since DataWedge 6.7)
        Intent importIntent = new Intent();
        Bundle importBundle = new Bundle();
        importBundle.putString("FOLDER_PATH", getStagingDirectory().toString());
        importIntent.setAction("com.symbol.datawedge.api.ACTION");
        importIntent.putExtra("com.symbol.datawedge.api.IMPORT_CONFIG", importBundle);
        ctx.sendBroadcast(importIntent);

        prefs.edit().putInt("__dwprofile_installed_version", dwprofileVersion).apply();
    }
}