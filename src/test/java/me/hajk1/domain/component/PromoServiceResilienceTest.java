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

@DisplayName("Promo Service - Resilience")
class PromoServiceResilienceTest extends ComponentTestBase {

  @Test
  @DisplayName("Should continue without promo when promo service fails")
  void shouldContinueWithoutPromoOnFailure(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);

    // Promo service returns 500
    promoServiceMock.stubFor(
        get(urlPathEqualTo("/promo/BROKEN")).willReturn(aResponse().withStatus(500)));

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("BROKEN")
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

                          var body = response.bodyAsJsonObject();
                          assertThat(body.getInteger("basePoints")).isEqualTo(3000);
                          assertThat(body.getInteger("tierBonus")).isEqualTo(450);
                          assertThat(body.getInteger("promoBonus")).isZero();
                          assertThat(body.getInteger("totalPoints")).isEqualTo(3450);

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should handle promo service timeout gracefully")
  void shouldHandlePromoServiceTimeout(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);

    // Promo service times out
    promoServiceMock.stubFor(
        get(urlPathEqualTo("/promo/SLOW"))
            .willReturn(
                aResponse()
                    .withStatus(200)
                    .withFixedDelay(3000)
                    .withBody(
                        """
                        {
                            "code": "SLOW",
                            "bonusPercentage": 50,
                            "expiresInDays": 30
                        }
                        """)));

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("SLOW")
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

                          var body = response.bodyAsJsonObject();
                          assertThat(body.getInteger("promoBonus")).isZero();

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should handle invalid promo code (404)")
  void shouldHandleInvalidPromoCode(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);

    promoServiceMock.stubFor(
        get(urlPathEqualTo("/promo/INVALID")).willReturn(aResponse().withStatus(404)));

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("INVALID")
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

                          var body = response.bodyAsJsonObject();
                          assertThat(body.getInteger("promoBonus")).isZero();

                          testContext.completeNow();
                        })));
  }
}
