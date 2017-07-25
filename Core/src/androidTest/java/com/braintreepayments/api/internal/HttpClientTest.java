package com.braintreepayments.api.internal;

import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.core.BuildConfig;
import com.braintreepayments.api.interfaces.HttpResponseCallback;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import okhttp3.Request;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class HttpClientTest {

    @Test(timeout = 1000)
    public void sendsDefaultUserAgentWhenNoUserAgentIsSet() throws IOException {
        HttpClient httpClient = new HttpClient();

        Request request = httpClient.init("http://example.com/").build();

        assertEquals("braintree/core/" + BuildConfig.VERSION_NAME, request.header("User-Agent"));
    }

    @Test(timeout = 1000)
    public void sendsUserAgent() throws IOException {
        HttpClient httpClient = new HttpClient()
                .setUserAgent("test/user/agent");

        Request request = httpClient.init("http://example.com/").build();

        assertEquals("test/user/agent", request.header("User-Agent"));
    }

//    @Test(timeout = 1000)
//    public void postsErrorForHttpsRequestsWhenSSLSocketFactoryIsNull()
//            throws IOException, InterruptedException {
//        HttpClient httpClient = new HttpClient()
//                .setBaseUrl("https://example.com")
//                .setSSLSocketFactory(null);
//
//        assertExceptionIsPosted(httpClient, SSLException.class,
//                "SSLSocketFactory was not set or failed to initialize");
//    }

//    @Test(timeout = 1000)
//    public void usesSSLSocketFactory() throws IOException {
//        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
//        HttpClient httpClient = new HttpClient()
//                .setSSLSocketFactory(sslSocketFactory);
//
//        HttpURLConnection connection = httpClient.init("https://example.com/");
//
//        assertEquals(sslSocketFactory, ((HttpsURLConnection) connection).getSSLSocketFactory());
//    }
//
//    @Test(timeout = 1000)
//    public void doesNotUseSSLSocketFactoryForNonHttpsRequests() throws IOException {
//        HttpClient httpClient = new HttpClient();
//
//        HttpURLConnection connection = httpClient.init("http://example.com/");
//
//        assertFalse(connection instanceof HttpsURLConnection);
//    }

    @Test(timeout = 1000)
    public void sendsAcceptLanguageHeader() throws IOException {
        HttpClient httpClient = new HttpClient();

        Request request = httpClient.init("http://example.com/").build();

        assertEquals(Locale.getDefault().getLanguage(), request.header("Accept-Language"));
    }

    @Test(timeout = 1000)
    public void setsDefaultConnectTimeoutWhenNoTimeoutIsSet() throws IOException {
        HttpClient httpClient = new HttpClient();

        assertEquals(30000, httpClient.getOkHttpClient().connectTimeoutMillis());
    }

    @Test(timeout = 1000)
    public void setsConnectTimeout() throws IOException {
        HttpClient httpClient = new HttpClient()
                .setConnectTimeout(1000);

        assertEquals(1000, httpClient.getOkHttpClient().connectTimeoutMillis());
    }

    @Test(timeout = 1000)
    public void setsDefaultReadTimeoutWhenNoTimeoutIsSet() throws IOException {
        HttpClient httpClient = new HttpClient();

        assertEquals(30000, httpClient.getOkHttpClient().readTimeoutMillis());
    }

    @Test(timeout = 1000)
    public void setsReadTimeout() throws IOException {
        HttpClient httpClient = new HttpClient()
                .setReadTimeout(1000);

        assertEquals(1000, httpClient.getOkHttpClient().readTimeoutMillis());
    }

//    @Test(timeout = 1000)
//    public void postsErrorWhenBaseUrlIsNotSet() throws InterruptedException, IOException {
//        HttpClient httpClient = new HttpClient();
//
//        assertExceptionIsPosted(httpClient, MalformedURLException.class, null);
//    }
//
//    @Test(timeout = 1000)
//    public void postsErrorWhenBaseUrlIsNull() throws InterruptedException, IOException {
//        HttpClient httpClient = new HttpClient();
//        httpClient.setBaseUrl(null);
//
//        assertExceptionIsPosted(httpClient, MalformedURLException.class, null);
//    }
//
//    @Test(timeout = 1000)
//    public void postsErrorWhenBaseUrlIsEmpty() throws InterruptedException, IOException {
//        HttpClient httpClient = new HttpClient();
//        httpClient.setBaseUrl("");
//
//        assertExceptionIsPosted(httpClient, MalformedURLException.class, null);
//    }

    @Test(timeout = 1000)
    public void postsErrorWhenPathIsNull() throws InterruptedException {
        HttpClient httpClient = new HttpClient();
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get(null, new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(IllegalArgumentException.class, exception.getClass());
                assertEquals("Path cannot be null", exception.getMessage());
                countDownLatch.countDown();
            }
        });

        httpClient.post(null, "{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(IllegalArgumentException.class, exception.getClass());
                assertEquals("Path cannot be null", exception.getMessage());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

//    @Test(timeout = 1000)
//    public void successCallbacksHappenOnMainThread() throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(200, "");
//        final CountDownLatch countDownLatch = new CountDownLatch(2);
//
//        httpClient.get("/", new HttpResponseCallback() {
//            @Override
//            public void success(String responseBody) {
//                assertEquals(Looper.getMainLooper(), Looper.myLooper());
//                countDownLatch.countDown();
//            }
//
//            @Override
//            public void failure(Exception exception) {
//                fail("Request failed");
//            }
//        });
//
//        httpClient.post("/", "{}", new HttpResponseCallback() {
//            @Override
//            public void success(String responseBody) {
//                assertEquals(Looper.getMainLooper(), Looper.myLooper());
//                countDownLatch.countDown();
//            }
//
//            @Override
//            public void failure(Exception exception) {
//                fail("Request failed");
//            }
//        });
//
//        countDownLatch.await();
//    }

//    @Test(timeout = 1000)
//    public void failureCallbacksHappenOnMainThread() throws Exception {
//        HttpClient httpClient = spy(new HttpClient());
//        doThrow(new IOException()).when(httpClient).parseResponse(any(Response.class));
//        final CountDownLatch countDownLatch = new CountDownLatch(2);
//
//        httpClient.get("/", new HttpResponseCallback() {
//            @Override
//            public void success(String responseBody) {
//                fail("Request was successful");
//            }
//
//            @Override
//            public void failure(Exception exception) {
//                assertEquals(Looper.getMainLooper(), Looper.myLooper());
//                countDownLatch.countDown();
//            }
//        });
//
//        httpClient.post("/", "{}", new HttpResponseCallback() {
//            @Override
//            public void success(String responseBody) {
//                fail("Request was successful");
//            }
//
//            @Override
//            public void failure(Exception exception) {
//                assertEquals(Looper.getMainLooper(), Looper.myLooper());
//                countDownLatch.countDown();
//            }
//        });
//
//        countDownLatch.await();
//    }

//    @Test(timeout = 1000)
//    public void postsIOExceptionWhenHttpRequestBlowsUp() throws Exception {
//        HttpClient httpClient = spy(new HttpClient());
//        HttpURLConnection httpURLConnection = mock(HttpURLConnection.class);
//        when(httpURLConnection.getOutputStream()).thenReturn(mock(OutputStream.class));
//        doReturn(httpURLConnection).when(httpClient).init(anyString());
//        doThrow(new IOException()).when(httpClient).parseResponse(any(Response.class));
//
//        assertExceptionIsPosted(httpClient, IOException.class, null);
//    }

//    @Test(timeout = 1000)
//    public void postsServerErrorWhenServerReturns500() throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(500, "");
//
//        assertExceptionIsPosted(httpClient, ServerException.class, null);
//    }
//
//    @Test(timeout = 1000)
//    public void postsDownForMaintenanceWhenServerIsDown() throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(503, "");
//
//        assertExceptionIsPosted(httpClient, DownForMaintenanceException.class, null);
//    }
//
//    @Test(timeout = 1000)
//    public void throwsUpgradeRequiredExceptionOn426()
//            throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(426, "");
//
//        assertExceptionIsPosted(httpClient, UpgradeRequiredException.class, null);
//    }
//
//    @Test(timeout = 1000)
//    public void postsAuthenticationExceptionOn401() throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(401, "");
//
//        assertExceptionIsPosted(httpClient, AuthenticationException.class, null);
//    }
//
//    @Test(timeout = 1000)
//    public void postsAuthorizationExceptionOn403() throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(403, "There was an error");
//
//        assertExceptionIsPosted(httpClient, AuthorizationException.class, "There was an error");
//    }
//
//    @Test(timeout = 1000)
//    public void postsUnprocessableEntityExceptionOn422() throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(422, "There was an error");
//
//        assertExceptionIsPosted(httpClient, UnprocessableEntityException.class, "There was an error");
//    }
//
//    @Test(timeout = 1000)
//    public void postsRateLimitExceptionOn429() throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(429, "");
//
//        assertExceptionIsPosted(httpClient, RateLimitException.class, "You are being rate-limited. Please try again in a few minutes.");
//    }
//
//    @Test(timeout = 1000)
//    public void postsUnknownExceptionOnUnrecognizedStatusCode()
//            throws IOException, InterruptedException {
//        HttpClient httpClient = clientWithExpectedResponse(418, "");
//
//        assertExceptionIsPosted(httpClient, UnexpectedException.class, null);
//    }

    /* helpers */
    private void assertExceptionIsPosted(HttpClient httpClient, final Class<? extends Exception> exceptionType,
            final String exceptionMessage) throws IOException, InterruptedException {
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(exceptionType, exception.getClass());
                if (exceptionMessage != null) {
                    assertEquals(exceptionMessage, exception.getMessage());
                }
                countDownLatch.countDown();
            }
        });

        httpClient.post("/", "{}", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(exceptionType, exception.getClass());
                if (exceptionMessage != null) {
                    assertEquals(exceptionMessage, exception.getMessage());
                }
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

//    private HttpClient clientWithExpectedResponse(int responseCode, String response)
//            throws IOException {
//        HttpURLConnection connection = mock(HttpURLConnection.class);
//        when(connection.getOutputStream()).thenReturn(mock(OutputStream.class));
//        when(connection.getResponseCode()).thenReturn(responseCode);
//        when(connection.getInputStream()).thenReturn(streamFromString(response))
//                .thenReturn(streamFromString(response));
//        when(connection.getErrorStream()).thenReturn(streamFromString(response))
//                .thenReturn(streamFromString(response));
//
//        HttpClient httpClient = spy(new HttpClient());
//        doReturn(connection).when(httpClient).init(anyString());
//
//        return httpClient;
//    }
//
//    private InputStream streamFromString(String string) throws UnsupportedEncodingException {
//        return new ByteArrayInputStream(string.getBytes("UTF-8"));
//    }
}
