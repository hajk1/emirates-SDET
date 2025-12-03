package me.hajk1.domain.component;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import java.util.concurrent.TimeUnit;
import me.hajk1.Application;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public abstract class ComponentTestBase {

  protected WireMockServer fxServiceMock;
  protected WireMockServer promoServiceMock;
  protected WebClient client;
  protected int serverPort = 8888; // Fixed port for tests

  @BeforeEach
  void setUp(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
    // Start WireMock servers on random ports
    // Note: Initially tried port 0 for the app server too, but had issues
    // getting the actual port back. Fixed port (8888) is simpler for tests.
    fxServiceMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    promoServiceMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    fxServiceMock.start();
    promoServiceMock.start();

    // Create web client
    client = WebClient.create(vertx);

    // Deploy application with fixed test port
    JsonObject config =
        new JsonObject()
            .put("http.port", serverPort)
            .put("fx.service.url", "http://localhost:" + fxServiceMock.port())
            .put("promo.service.url", "http://localhost:" + promoServiceMock.port())
            .put("fx.retry.maxAttempts", 3) // 3 total attempts
            .put("promo.timeout.ms", 2000);

    vertx
        .deployVerticle(new Application(), new io.vertx.core.DeploymentOptions().setConfig(config))
        .onComplete(testContext.succeeding(id -> testContext.completeNow()));

    // Wait for deployment to complete
    testContext.awaitCompletion(5, TimeUnit.SECONDS);
  }

  @AfterEach
  void tearDown(Vertx vertx, VertxTestContext testContext) {
    if (client != null) {
      client.close();
    }
    if (fxServiceMock != null) {
      fxServiceMock.stop();
    }
    if (promoServiceMock != null) {
      promoServiceMock.stop();
    }

    vertx.close(testContext.succeedingThenComplete());
  }

  protected void stubFxRate(String currency, double rate) {
    fxServiceMock.stubFor(
        get(urlPathEqualTo("/fx/rates"))
            .withQueryParam("from", equalTo(currency))
            .withQueryParam("to", equalTo("AED"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        String.format(
                            """
                        {
                            "rate": %.2f,
                            "timestamp": "2025-11-25T10:00:00Z"
                        }
                        """,
                            rate))));
  }

  protected void stubPromoCode(String code, int bonusPercentage, boolean expiresSoon) {
    promoServiceMock.stubFor(
        get(urlPathEqualTo("/promo/" + code))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        String.format(
                            """
                        {
                            "code": "%s",
                            "bonusPercentage": %d,
                            "expiresInDays": %d
                        }
                        """,
                            code, bonusPercentage, expiresSoon ? 3 : 30))));
  }
}
