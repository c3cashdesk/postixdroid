package de.ccc.events.postixdroid.ui;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiNetworkSpecifier;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;


import de.ccc.events.postixdroid.AppConfig;

public class WiFiHelper {
    private Context ctx;
    private AppConfig config;

    public WiFiHelper(Context ctx) {
        this.ctx = ctx;
        this.config = new AppConfig(ctx);
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void conenctWiFi(WifiNetworkSpecifier wifiNetworkSpecifier) {
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(wifiNetworkSpecifier)
                .build();

        ConnectivityManager connectivityManager = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback());
    }

    public void connectCashdeskWiFi() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return;
        }
        
        WifiEnterpriseConfig wifiEnterpriseConfig = new WifiEnterpriseConfig();
        wifiEnterpriseConfig.setEapMethod(WifiEnterpriseConfig.Eap.PEAP);
        wifiEnterpriseConfig.setPhase2Method(WifiEnterpriseConfig.Phase2.MSCHAPV2);
        // ToDo: CA-Cert "Use system certificates"
        wifiEnterpriseConfig.setAltSubjectMatch("radius.c3noc.net");
        wifiEnterpriseConfig.setIdentity(config.getWiFiUser());
        wifiEnterpriseConfig.setAnonymousIdentity("");
        wifiEnterpriseConfig.setPassword(config.getWiFiPass());

        WifiNetworkSpecifier wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
                .setSsid(config.getWiFiSSID())
                .setWpa2EnterpriseConfig(wifiEnterpriseConfig)
                //.setWpa3EnterpriseConfig(wifiEnterpriseConfig)
                .build();

        conenctWiFi(wifiNetworkSpecifier);
    }

    public boolean hasConfig() {
        return !TextUtils.isEmpty(config.getWiFiSSID())
                && !TextUtils.isEmpty(config.getWiFiUser())
                && !TextUtils.isEmpty(config.getWiFiPass());
    }
}
