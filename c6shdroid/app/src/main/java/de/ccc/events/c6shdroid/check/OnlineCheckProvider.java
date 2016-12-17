package de.ccc.events.c6shdroid.check;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import de.ccc.events.c6shdroid.AppConfig;
import de.ccc.events.c6shdroid.net.api.ApiException;
import de.ccc.events.c6shdroid.net.api.C6shApi;

public class OnlineCheckProvider implements TicketCheckProvider {
    private Context ctx;
    private C6shApi api;
    private AppConfig config;

    public OnlineCheckProvider(Context ctx) {
        this.ctx = ctx;

        this.config = new AppConfig(ctx);
        this.api = C6shApi.fromConfig(config);
    }

    @Override
    public CheckResult check(String ticketid, JSONObject options) {
        try {
            CheckResult res = new CheckResult(CheckResult.Type.ERROR);
            JSONObject response = api.redeem(ticketid, options);
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
                } else if ("confirmation".equals(type)) {
                    res.setType(CheckResult.Type.CONFIRMATION);
                } else {
                    res.setType(CheckResult.Type.ERROR);
                }
            }
            return res;
        } catch (JSONException e) {
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
