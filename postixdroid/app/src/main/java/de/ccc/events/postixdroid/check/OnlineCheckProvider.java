package de.ccc.events.postixdroid.check;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.ccc.events.postixdroid.AppConfig;
import de.ccc.events.postixdroid.net.api.ApiException;
import de.ccc.events.postixdroid.net.api.PostixApi;

public class OnlineCheckProvider implements TicketCheckProvider {
    private Context ctx;
    private PostixApi api;
    private AppConfig config;

    public OnlineCheckProvider(Context ctx) {
        this.ctx = ctx;

        this.config = new AppConfig(ctx);
        this.api = PostixApi.fromConfig(config);
    }

    @Override
    public CheckResult check(String ticketid, JSONObject options) {
        try {
            CheckResult res = new CheckResult(CheckResult.Type.ERROR);
            if (ticketid.startsWith("/supply")) {
                JSONObject response = api.supply(ticketid);
                if (response.optBoolean("success", false)) {
                    return new CheckResult(CheckResult.Type.VALID, "Supply registered.");
                } else if (response.has("message")) {
                    res.setMessage(response.getString("message"));
                }
            } else if (ticketid.startsWith("/resupply")) {
                JSONObject response = api.requestResupply();
                if (response.optBoolean("success", false)) {
                    return new CheckResult(CheckResult.Type.VALID, "OK, Troubleshooter gets a notification!");
                } else if (response.has("message")) {
                    res.setMessage(response.getString("message"));
                }
            } else if (ticketid.startsWith("/ping")) {
                JSONObject response = api.pong(ticketid);
                if (response.optBoolean("success", false)) {
                    return new CheckResult(CheckResult.Type.VALID, "Pong! Thanks :-)");
                } else if (response.has("message")) {
                    res.setMessage(response.getString("message"));
                }
            } else {
                JSONObject response = api.redeem(ticketid, options);
                if (!response.has("success") && response.has("detail")) {
                    return new CheckResult(CheckResult.Type.ERROR, response.getString("detail"));
                }
                boolean status = response.getBoolean("success");
                if (status) {
                    res.setType(CheckResult.Type.VALID);
                } else {
                    JSONArray positions = response.getJSONArray("positions");
                    JSONObject position = positions.getJSONObject(0);

                    res.setMessage(position.optString("message"));
                    String type = position.optString("type");
                    if ("input".equals(type)) {
                        res.setType(CheckResult.Type.INPUT);
                        res.setMissingField(position.optString("missing_field"));
                    } else if ("confirmation".equals(type)) {
                        res.setType(CheckResult.Type.CONFIRMATION);
                        res.setMissingField(position.optString("missing_field"));
                    } else {
                        res.setType(CheckResult.Type.ERROR);
                    }
                }
            }
            return res;
        } catch (JSONException e) {
            e.printStackTrace();
            if (e.getCause() != null) {
                return new CheckResult(CheckResult.Type.ERROR, "Invalid server response: " + e.getCause().getMessage());
            } else {
                return new CheckResult(CheckResult.Type.ERROR, "Invalid server response");
            }
        } catch (ApiException e) {
            if (e.getCause() != null) {
                return new CheckResult(CheckResult.Type.ERROR, e.getMessage() + ": " + e.getCause().getMessage());
            } else {
                return new CheckResult(CheckResult.Type.ERROR, e.getMessage());
            }
        }
    }

    @Override
    public List<SearchResult> search(String query) throws CheckException {
        return null;
    }
}
