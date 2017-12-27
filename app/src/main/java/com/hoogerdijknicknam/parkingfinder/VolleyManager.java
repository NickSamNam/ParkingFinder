package com.hoogerdijknicknam.parkingfinder;

import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;

/**
 * Created by kevin on 30-11-2017.
 */

public class VolleyManager {
    private static final String TAG = "NetworkManager";
    private static final String prefixURL = "http://some/url/prefix/";
    private static VolleyManager instance = null;
    //for Volley API
    public RequestQueue requestQueue;

    private VolleyManager(Context context) {
        requestQueue = Volley.newRequestQueue(context.getApplicationContext());
    }

    public static synchronized VolleyManager getInstance(Context context) {
        if (null == instance)
            instance = new VolleyManager(context);
        return instance;
    }

    //this is so you don't need to pass context each time
    public static synchronized VolleyManager getInstance() {
        if (null == instance) {
            throw new IllegalStateException(VolleyManager.class.getSimpleName() +
                    " is not initialized, call getInstance(...) first");
        }
        return instance;
    }

    public void JsonObjectRequest(int method, String requestURL, JSONObject body, final ResponseListener listener) {

        JsonObjectRequest request = new JsonObjectRequest(method, requestURL, body,
                response -> {
                    if (null != response.toString())
                        listener.getResult(response);
                },
                error -> {
                    if (null != error.networkResponse) {
                        //Log.d(TAG + ": ", "Error Response code: " + error.networkResponse.statusCode);
                        listener.getResult(false);
                    }
                });

        requestQueue.add(request);
    }

    public void JsonArrayRequest(int method, String requestURL, JSONObject body, final ResponseListener listener) {
        CustomJsonArrayRequest request = new CustomJsonArrayRequest(method, requestURL, body,
                response -> {
                    //Log.d(TAG + ": ", "somePostRequest Response : " + response.toString());
                    if (null != response.toString())
                        listener.getResult(response);
                },
                error -> {
                    if (null != error.networkResponse) {
                        //Log.d(TAG + ": ", "Error Response code: " + error.networkResponse.statusCode);
                        listener.getResult(false);
                    }
                });

        requestQueue.add(request);
    }
}

class CustomJsonArrayRequest extends JsonRequest<JSONArray> {

    /**
     * Creates a new request.
     *
     * @param method        the HTTP method to use
     * @param url           URL to fetch the JSON from
     * @param jsonRequest   A {@link JSONObject} to post with the request. Null is allowed and
     *                      indicates no parameters will be posted along with request.
     * @param listener      Listener to receive the JSON response
     * @param errorListener Error listener, or null to ignore errors.
     */
    public CustomJsonArrayRequest(int method, String url, JSONObject jsonRequest,
                                  Response.Listener<JSONArray> listener, Response.ErrorListener errorListener) {
        super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(), listener,
                errorListener);
    }

    @Override
    protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
        try {
            String jsonString = new String(response.data,
                    HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
            return Response.success(new JSONArray(jsonString),
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException | JSONException e) {
            return Response.error(new ParseError(e));
        }
    }
}
