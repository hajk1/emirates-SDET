package me.hajk1.domain.component;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.math.BigDecimal;
import me.hajk1.domain.model.CabinClass;
import me.hajk1.domain.model.CustomerTier;
import me.hajk1.domain.model.PointsQuoteRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("FX Service - Resilience")
class FxServiceResilienceTest extends ComponentTestBase {

  @Test
  @DisplayName("Should retry FX service on temporary failure and succeed")
  void shouldRetryFxServiceAndSucceed(VertxTestContext testContext) {
    // WireMock scenarios let us simulate state changes across requests
    // This was tricky to get right - the key is understanding whenScenarioStateIs
    // First 2 attempts fail (503), 3rd succeeds
    fxServiceMock.stubFor(
        get(urlPathEqualTo("/fx/rates"))
            .withQueryParam("from", equalTo("USD"))
            .withQueryParam("to", equalTo("AED"))
            .inScenario("Retry")
            .whenScenarioStateIs("Started")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("ATTEMPT_2"));

    fxServiceMock.stubFor(
        get(urlPathEqualTo("/fx/rates"))
            .withQueryParam("from", equalTo("USD"))
            .withQueryParam("to", equalTo("AED"))
            .inScenario("Retry")
            .whenScenarioStateIs("ATTEMPT_2")
            .willReturn(aResponse().withStatus(503))
            .willSetStateTo("SUCCESS"));

    fxServiceMock.stubFor(
        get(urlPathEqualTo("/fx/rates"))
            .withQueryParam("from", equalTo("USD"))
            .withQueryParam("to", equalTo("AED"))
            .inScenario("Retry")
            .whenScenarioStateIs("SUCCESS")
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                            "rate": 3.67,
                            "timestamp": "2025-11-25T10:00:00Z"
                        }
                        """)));

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(100))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.NONE)
            .build();

    client
        .post(serverPort, "localhost", "/v1/points/quote")
        .sendJsonObject(JsonObject.mapFrom(request))
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);

                          // Verify FX service was called 3 times
                          fxServiceMock.verify(
                              3,
                              getRequestedFor(urlPathEqualTo("/fx/rates"))
                                  .withQueryParam("from", equalTo("USD")));

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should fail after max FX retries")
  void shouldFailAfterMaxFxRetries(VertxTestContext testContext) {
    // All attempts fail
    fxServiceMock.stubFor(get(urlPathEqualTo("/fx/rates")).willReturn(aResponse().withStatus(503)));

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(100))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.NONE)
            .build();

    client
        .post(serverPort, "localhost", "/v1/points/quote")
        .sendJsonObject(JsonObject.mapFrom(request))
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(500);

                          // Verify FX service was called 3 times (initial + 2 retries)
                          fxServiceMock.verify(3, getRequestedFor(urlPathEqualTo("/fx/rates")));

                          testContext.completeNow();
                        })));
  }
}
