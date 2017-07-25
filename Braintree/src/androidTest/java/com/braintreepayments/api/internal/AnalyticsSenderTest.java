package com.braintreepayments.api.internal;

import android.support.test.runner.AndroidJUnit4;

import com.braintreepayments.api.models.Authorization;
import com.braintreepayments.api.models.Configuration;
import com.braintreepayments.api.test.TestClientTokenBuilder;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

import okhttp3.Response;

import static android.support.test.InstrumentationRegistry.getTargetContext;
import static junit.framework.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class AnalyticsSenderTest {

    @Test(timeout = 10000)
    public void sendsCorrectlyFormattedAnalyticsRequest() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        String authorization = new TestClientTokenBuilder().withAnalytics().build();
        final Configuration configuration = Configuration.fromJson(authorization);

        AnalyticsEvent event = new AnalyticsEvent(getTargetContext(), "sessionId", "custom", "event.started");
        AnalyticsDatabase.getInstance(getTargetContext()).addEvent(event);

        BraintreeHttpClient httpClient = new BraintreeHttpClient(Authorization.fromString(authorization)) {
            @Override
            protected String parseResponse(Response response) throws Exception {
                if (response.request().url().toString().equals(configuration.getAnalytics().getUrl())) {
                    assertEquals(200, response.code());
                    latch.countDown();
                }
                return "";
            }
        };

        AnalyticsSender.send(getTargetContext(), Authorization.fromString(authorization), httpClient,
                configuration.getAnalytics().getUrl(), true);

        latch.await();
    }
}
