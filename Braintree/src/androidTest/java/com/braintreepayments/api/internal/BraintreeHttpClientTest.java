package com.braintreepayments.api.internal;

import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.BuildConfig;
import com.braintreepayments.api.exceptions.AuthorizationException;
import com.braintreepayments.api.exceptions.ErrorWithResponse;
import com.braintreepayments.api.exceptions.InvalidArgumentException;
import com.braintreepayments.api.interfaces.HttpResponseCallback;
import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.TokenizationKey;
import com.braintreepayments.api.test.EnvironmentHelper;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.CountDownLatch;

import okhttp3.Request;

import static com.braintreepayments.api.internal.BraintreeHttpClientTestUtils.clientWithExpectedResponse;
import static com.braintreepayments.testutils.FixturesHelper.stringFromFixture;
import static com.braintreepayments.testutils.TestTokenizationKey.TOKENIZATION_KEY;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class BraintreeHttpClientTest {

    private CountDownLatch mCountDownLatch;

    @Before
    public void setup() throws Exception {
        mCountDownLatch = new CountDownLatch(1);
    }

    @Test(timeout = 1000)
    public void getUserAgent_returnsCorrectUserAgent() {
        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME,
                BraintreeHttpClient.getUserAgent());
    }

    @Test(timeout = 1000)
    public void sendsUserAgent() throws IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY));

        Request request = httpClient.init("http://example.com/").build();

        assertEquals("braintree/android/" + BuildConfig.VERSION_NAME,
                request.header("User-Agent"));
    }

    @Test(timeout = 1000)
    public void sendsTokenizationKeyWhenPresent() throws IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY));

        Request request = httpClient.init("http://example.com/").build();

        assertEquals(TOKENIZATION_KEY, request.header("Client-Key"));
    }

    @Test(timeout = 1000)
    public void doesNotSendTokenizationKeyWhenNotPresent()
            throws IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                Authorization.fromString(stringFromFixture("client_token.json")));

        Request request = httpClient.init("http://example.com/").build();

        assertNull(request.header("Client-Key"));
    }

    @Test(timeout = 1000)
    public void get_includesAuthorizationFingerprintWhenPresent()
            throws IOException, InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(Authorization.fromString(stringFromFixture("client_token.json"))) {
            @Override
            protected Request.Builder init(String url) throws IOException {
                assertTrue(url.contains("authorization_fingerprint"));
                mCountDownLatch.countDown();

                return super.init(url);
            }
        };

        httpClient.get("/", null);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void get_doesNotIncludeAuthorizationFingerprintWhenNotPresent()
            throws IOException, InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY)) {
            @Override
            protected Request.Builder init(String url) throws IOException {
                assertFalse(url.contains("authorizationFingerprint"));
                mCountDownLatch.countDown();

                return super.init(url);
            }
        };

        httpClient.get("/", null);

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void postsErrorWhenBaseUrlIsNotSet()
            throws InterruptedException, IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(TOKENIZATION_KEY));
        final CountDownLatch countDownLatch = new CountDownLatch(2);

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(MalformedURLException.class, exception.getClass());
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
                assertEquals(MalformedURLException.class, exception.getClass());
                countDownLatch.countDown();
            }
        });

        countDownLatch.await();
    }

    @Test(timeout = 1000)
    public void postsErrorWhenBaseUrlIsNull()
            throws InterruptedException, InvalidArgumentException, IOException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(TOKENIZATION_KEY));
        httpClient.setBaseUrl(null);

        assertExceptionIsPosted(httpClient, MalformedURLException.class, null);
    }


    @Test(timeout = 1000)
    public void postsErrorWhenBaseUrlIsEmpty()
            throws InterruptedException, IOException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(TOKENIZATION_KEY));
        httpClient.setBaseUrl("");

        assertExceptionIsPosted(httpClient, MalformedURLException.class, null);
    }

    @Test(timeout = 1000)
    public void postsErrorWhenPathIsNull() throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(TokenizationKey.fromString(TOKENIZATION_KEY));
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

    @Test(timeout = 1000)
    public void postsErrorWhenClientTokenIsUsedAndInvalidJsonIsSent()
            throws InvalidArgumentException, InterruptedException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                Authorization.fromString(stringFromFixture("client_token.json")));

        httpClient.post("/", "not json", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertTrue(exception instanceof JSONException);
                assertEquals("Value not of type java.lang.String cannot be converted to JSONObject",
                        exception.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 1000)
    public void throwsAuthorizationExceptionWithCorrectMessageOn403() throws IOException,
            InterruptedException, ErrorWithResponse, InvalidArgumentException {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(403,
                stringFromFixture("error_response.json"));

        assertExceptionIsPosted(httpClient, AuthorizationException.class, "There was an error");
    }

    @Test(timeout = 1000)
    public void throwsErrorWithResponseOn422() throws IOException, InterruptedException, ErrorWithResponse,
            InvalidArgumentException {
        BraintreeHttpClient httpClient = clientWithExpectedResponse(422, stringFromFixture("error_response.json"));

        assertExceptionIsPosted(httpClient, ErrorWithResponse.class, "There was an error");
    }

    @Test(timeout = 5000)
    public void getRequestSslCertificateSuccessfulInSandbox() throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY));
        httpClient.setBaseUrl("https://api.sandbox.braintreegateway.com");

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request failed");
            }

            @Override
            public void failure(Exception exception) {
                assertTrue(exception instanceof AuthorizationException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    public void getRequestSslCertificateSuccessfulInProduction() throws InterruptedException, InvalidArgumentException {
        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY));
        httpClient.setBaseUrl("https://api.braintreegateway.com");

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request failed");
            }

            @Override
            public void failure(Exception exception) {
                assertTrue(exception instanceof AuthorizationException);
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    @Test(timeout = 5000)
    public void getRequestBadCertificateCheck()
            throws InterruptedException, InvalidArgumentException {
        if (!BuildConfig.RUN_ALL_TESTS) {
            return;
        }

        BraintreeHttpClient httpClient = new BraintreeHttpClient(
                TokenizationKey.fromString(TOKENIZATION_KEY));
        httpClient.setBaseUrl("https://" + EnvironmentHelper.getLocalhostIp() + ":9443");

        httpClient.get("/", new HttpResponseCallback() {
            @Override
            public void success(String responseBody) {
                fail("Request was successful");
            }

            @Override
            public void failure(Exception exception) {
                assertEquals(
                        "java.security.cert.CertPathValidatorException: Trust anchor for certification path not found.",
                        exception.getMessage());
                mCountDownLatch.countDown();
            }
        });

        mCountDownLatch.await();
    }

    /* helpers */
    private void assertExceptionIsPosted(BraintreeHttpClient httpClient,
            final Class<? extends Exception> exceptionType, final String exceptionMessage)
            throws IOException, InterruptedException {
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
}
