package de.ccc.events.postixdroid.net.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.net.ssl.SSLException;

import de.ccc.events.postixdroid.AppConfig;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostixApi {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private String url;
    private String key;

    public PostixApi(String url, String key) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        this.url = url;
        this.key = key;
    }

    public static PostixApi fromConfig(AppConfig config) {
        return new PostixApi(config.getApiUrl(), config.getApiKey());
    }

    public JSONObject pong(String identifier) throws ApiException {
        JSONObject data = new JSONObject();
        try {
            data.put("pong", identifier);
        } catch (JSONException e) {
            throw new ApiException("Error while creating the request.");
        }

        Request request = new Request.Builder()
                .addHeader("Authorization", "Token " + key)
                .url(url + "/api/cashdesk/pong/")
                .post(RequestBody.create(JSON, data.toString()))
                .build();
        return apiCall(request);
    }

    public JSONObject listEntries(int listId, String query) throws ApiException {
        Request request = new Request.Builder()
                .addHeader("Authorization", "Token " + key)
                .url(url + "/api/listconstraintentries/?listid=" + listId + "&search=" + query)
                .get()
                .build();
        return apiCall(request);
    }

    public JSONObject requestResupply() throws ApiException {
        JSONObject data = new JSONObject();
        Request request = new Request.Builder()
                .addHeader("Authorization", "Token " + key)
                .url(url + "/api/cashdesk/request-resupply/")
                .post(RequestBody.create(JSON, data.toString()))
                .build();
        return apiCall(request);
    }

    public JSONObject supply(String identifier) throws ApiException {
        JSONObject data = new JSONObject();
        try {
            data.put("identifier", identifier);
        } catch (JSONException e) {
            throw new ApiException("Error while creating the request.");
        }

        Request request = new Request.Builder()
                .addHeader("Authorization", "Token " + key)
                .url(url + "/api/cashdesk/supply/")
                .post(RequestBody.create(JSON, data.toString()))
                .build();
        return apiCall(request);
    }

    public JSONObject redeem(String secret, JSONObject options) throws ApiException {
        JSONObject data = new JSONObject();
        JSONArray positions = new JSONArray();
        JSONObject position = options;
        try {
            position.put("type", "redeem");
            position.put("secret", secret);
            positions.put(position);
            data.put("positions", positions);
        } catch (JSONException e) {
            throw new ApiException("Error while creating the request.");
        }

        Request request = new Request.Builder()
                .addHeader("Authorization", "Token " + key)
                .url(url + "/api/transactions/")
                .post(RequestBody.create(JSON, data.toString()))
                .build();
        return apiCall(request);
    }

    private JSONObject apiCall(Request request) throws ApiException {
        OkHttpClient client = new OkHttpClient();
        Response response;
        try {
            response = client.newCall(request).execute();
        } catch (SSLException e) {
            e.printStackTrace();
            throw new ApiException("Error while creating a secure connection.", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApiException("Connection error.", e);
        }
        if (response.code() >= 500) {
            throw new ApiException("Server error.");
        } else if (response.code() == 404) {
            throw new ApiException("Invalid configuration, please reset and reconfigure.");
        } else if (response.code() == 403) {
            throw new ApiException("Permission error, please try again or reset and reconfigure.");
        }
        try {
            return new JSONObject(response.body().string());
        } catch (JSONException e) {
            e.printStackTrace();
            throw new ApiException("Invalid JSON received.", e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApiException("Connection error.", e);
        }
    }

}
