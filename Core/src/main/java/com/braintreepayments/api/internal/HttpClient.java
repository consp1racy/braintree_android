package com.braintreepayments.api.internal;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.braintreepayments.api.core.BuildConfig;
import com.braintreepayments.api.exceptions.AuthenticationException;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.DownForMaintenanceException;
import com.braintreepayments.api.exceptions.RateLimitException;
import com.braintreepayments.api.exceptions.ServerException;
import com.braintreepayments.api.exceptions.UnexpectedException;
import com.braintreepayments.api.exceptions.UnprocessableEntityException;
import com.braintreepayments.api.exceptions.UpgradeRequiredException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLException;

import okhttp3.Call;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.TlsVersion;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_CREATED;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.net.HttpURLConnection.HTTP_UNAVAILABLE;

public class HttpClient<T extends HttpClient> {

    private static final MediaType MEDIA_TYPE_APPLICATION_JSON = MediaType.parse("application/json");

    private static OkHttpClient sOkHttpClient = buildDefaultOkHttpClient();

    private final Handler mMainThreadHandler;

    @VisibleForTesting
    protected final ExecutorService mThreadPool;

    private String mUserAgent;

    protected String mBaseUrl;

    private OkHttpClient mOkHttpClient;

    private static OkHttpClient buildDefaultOkHttpClient() {
        final ConnectionSpec spec = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                .tlsVersions(TlsVersion.TLS_1_2)
                .build();

        final List<ConnectionSpec> connectionSpecs = new ArrayList<>();
        connectionSpecs.add(ConnectionSpec.CLEARTEXT);
        connectionSpecs.add(spec);

        final Builder builder = new Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .connectionSpecs(connectionSpecs);

        try {
            final TLSSocketFactory tlsSocketFactory = new TLSSocketFactory();
            builder.sslSocketFactory(tlsSocketFactory, tlsSocketFactory.getTrustManager());
        } catch (SSLException e) {
            /* No-op. */
        }

        return builder.build();
    }

    public HttpClient() {
        mThreadPool = Executors.newCachedThreadPool();
        mMainThreadHandler = new Handler(Looper.getMainLooper());
        mUserAgent = "braintree/core/" + BuildConfig.VERSION_NAME;
        mOkHttpClient = sOkHttpClient;
    }

    @VisibleForTesting
    public OkHttpClient getOkHttpClient() {
        return mOkHttpClient;
    }

    /**
     * @param userAgent the user agent to be sent with all http requests.
     * @return {@link HttpClient} for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setUserAgent(String userAgent) {
        mUserAgent = userAgent;
        return (T) this;
    }

    /**
     * @param sslSocketFactory the {@link javax.net.ssl.SSLSocketFactory} to use for all https requests.
     * @return {@link HttpClient} for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setSSLSocketFactory(@NonNull final TLSSocketFactory sslSocketFactory) {
        //noinspection deprecation
        mOkHttpClient = mOkHttpClient.newBuilder().sslSocketFactory(sslSocketFactory, sslSocketFactory.getTrustManager()).build();
        return (T) this;
    }

    /**
     * @param baseUrl the base url to use when only a path is supplied to {@link #get(String, HttpResponseCallback)} or
     * {@link #post(String, String, HttpResponseCallback)}
     * @return {@link HttpClient} for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setBaseUrl(String baseUrl) {
        mBaseUrl = (baseUrl == null) ? "" : baseUrl;
        return (T) this;
    }

    /**
     * @param timeout the time in milliseconds to wait for a connection before timing out.
     * @return {@link HttpClient} for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setConnectTimeout(int timeout) {
        mOkHttpClient = mOkHttpClient.newBuilder().connectTimeout(timeout, TimeUnit.MILLISECONDS).build();
        return (T) this;
    }

    /**
     * @param timeout the time in milliseconds to read a response from the server before timing out.
     * @return {@link HttpClient} for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setReadTimeout(int timeout) {
        mOkHttpClient = mOkHttpClient.newBuilder().readTimeout(timeout, TimeUnit.MILLISECONDS).build();
        return (T) this;
    }

    /**
     * Make a HTTP GET request to using the base url and path provided. If the path is a full url,
     * it will be used instead of the previously provided base url.
     *
     * @param path The path or url to request from the server via GET
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    public void get(final String path, final HttpResponseCallback callback) {
        if (path == null) {
            postCallbackOnMainThread(callback, new IllegalArgumentException("Path cannot be null"));
            return;
        }

        final String url;
        if (path.startsWith("http")) {
            url = path;
        } else {
            url = mBaseUrl + path;
        }

        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    final Request request = init(url).get().build();
                    final Call call = mOkHttpClient.newCall(request);
                    final Response response = call.execute();
                    postCallbackOnMainThread(callback, parseResponse(response));
                } catch (Exception e) {
                    postCallbackOnMainThread(callback, e);
                }
            }
        });
    }

    /**
     * Make a HTTP POST request using the base url and path provided. If the path is a full url,
     * it will be used instead of the previously provided url.
     *
     * @param path The path or url to request from the server via HTTP POST
     * @param data The body of the POST request
     * @param callback The {@link HttpResponseCallback} to receive the response or error.
     */
    public void post(final String path, final String data, final HttpResponseCallback callback) {
        if (path == null) {
            postCallbackOnMainThread(callback, new IllegalArgumentException("Path cannot be null"));
            return;
        }

        mThreadPool.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    postCallbackOnMainThread(callback, post(path, data));
                } catch (Exception e) {
                    postCallbackOnMainThread(callback, e);
                }
            }
        });
    }

    /**
     * Performs a synchronous post request.
     *
     * @param path the path or url to request from the server via HTTP POST
     * @param data the body of the post request
     * @return The HTTP body the of the response
     * @throws Exception
     * @see HttpClient#post(String, String, HttpResponseCallback)
     */
    public String post(String path, String data) throws Exception {
        final String url;
        if (path.startsWith("http")) {
            url = path;
        } else {
            url = mBaseUrl + path;
        }

        final RequestBody requestBody = RequestBody.create(MEDIA_TYPE_APPLICATION_JSON, data);

        final Request request = init(url).post(requestBody).build();
        final Call call = mOkHttpClient.newCall(request);
        final Response response = call.execute();
        return parseResponse(response);
    }

    protected Request.Builder init(String url) throws IOException {
        if (url.startsWith("https") && mOkHttpClient.sslSocketFactory() == null) {
            throw new SSLException("SSLSocketFactory was not set or failed to initialize");
        }

        return new Request.Builder()
                .url(url)
                .addHeader("User-Agent", mUserAgent)
                .addHeader("Accept-Language", Locale.getDefault().getLanguage());
    }

    protected String parseResponse(Response response) throws Exception {
        final int responseCode = response.code();
        final String responseBody = response.body().string();
        switch (responseCode) {
            case HTTP_OK: case HTTP_CREATED: case HTTP_ACCEPTED:
                return responseBody;
            case HTTP_UNAUTHORIZED:
                throw new AuthenticationException(responseBody);
            case HTTP_FORBIDDEN:
                throw new AuthorizationException(responseBody);
            case 422: // HTTP_UNPROCESSABLE_ENTITY
                throw new UnprocessableEntityException(responseBody);
            case 426: // HTTP_UPGRADE_REQUIRED
                throw new UpgradeRequiredException(responseBody);
            case 429: // HTTP_TOO_MANY_REQUESTS
                throw new RateLimitException("You are being rate-limited. Please try again in a few minutes.");
            case HTTP_INTERNAL_ERROR:
                throw new ServerException(responseBody);
            case HTTP_UNAVAILABLE:
                throw new DownForMaintenanceException(responseBody);
            default:
                throw new UnexpectedException(responseBody);
        }
    }

    void postCallbackOnMainThread(final HttpResponseCallback callback, final String response) {
        if (callback == null) {
            return;
        }

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.success(response);
            }
        });
    }

    void postCallbackOnMainThread(final HttpResponseCallback callback, final Exception exception) {
        if (callback == null) {
            return;
        }

        mMainThreadHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.failure(exception);
            }
        });
    }
}
