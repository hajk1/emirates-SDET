package me.hajk1.domain.component;

import static org.assertj.core.api.Assertions.assertThat;

import io.vertx.core.json.JsonObject;
import io.vertx.junit5.VertxTestContext;
import java.math.BigDecimal;
import me.hajk1.domain.model.CabinClass;
import me.hajk1.domain.model.CustomerTier;
import me.hajk1.domain.model.PointsQuoteRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Edge Cases")
class EdgeCaseComponentTest extends ComponentTestBase {

  @Test
  @DisplayName("Should handle request without promo code")
  void shouldHandleRequestWithoutPromoCode(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode(null) // Explicitly null
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
                          assertThat(body.getJsonArray("warnings")).isEmpty();

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should handle empty promo code")
  void shouldHandleEmptyPromoCode(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("") // Empty string
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
  @DisplayName("Should handle blank promo code")
  void shouldHandleBlankPromoCode(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("   ") // Blank string
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
  @DisplayName("Should handle promo with zero bonus percentage")
  void shouldHandleZeroBonusPromo(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);
    stubPromoCode("ZERO", 0, false);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("ZERO")
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
  @DisplayName("Should handle exactly at 50k cap without promo")
  void shouldHandleExactlyAt50kCap(VertxTestContext testContext) {
    stubFxRate("USD", 10.0);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(5000.00))
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
                          var body = response.bodyAsJsonObject();
                          assertThat(body.getInteger("totalPoints")).isEqualTo(50000);

                          testContext.completeNow();
                        })));
  }
}
