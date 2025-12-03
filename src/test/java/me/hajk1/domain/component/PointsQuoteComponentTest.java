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

@DisplayName("Points Quote - Happy Path & Business Rules")
class PointsQuoteComponentTest extends ComponentTestBase {

  @Test
  @DisplayName("Should calculate points with SILVER tier and promo code")
  void shouldCalculatePointsWithSilverTierAndPromo(VertxTestContext testContext) {
    // Arrange
    stubFxRate("USD", 3.67);
    stubPromoCode("SUMMER25", 25, false);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1234.50))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("SUMMER25")
            .build();

    // Act & Assert
    client
        .post(serverPort, "localhost", "/v1/points/quote")
        .sendJsonObject(JsonObject.mapFrom(request))
        .onComplete(
            testContext.succeeding(
                response ->
                    testContext.verify(
                        () -> {
                          assertThat(response.statusCode()).isEqualTo(200);
                          assertThat(response.getHeader("Content-Type"))
                              .contains("application/json");

                          var body = response.bodyAsJsonObject();
                          assertThat(body.getInteger("basePoints")).isEqualTo(4531);
                          assertThat(body.getInteger("tierBonus")).isEqualTo(679);
                          assertThat(body.getInteger("promoBonus")).isEqualTo(1302);
                          assertThat(body.getInteger("totalPoints")).isEqualTo(6512);
                          assertThat(body.getDouble("effectiveFxRate")).isEqualTo(3.67);
                          assertThat(body.getJsonArray("warnings")).isEmpty();

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should calculate points for PLATINUM tier without promo")
  void shouldCalculatePointsForPlatinumTier(VertxTestContext testContext) {
    stubFxRate("EUR", 4.05);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(2000.00))
            .currency("EUR")
            .cabinClass(CabinClass.BUSINESS)
            .customerTier(CustomerTier.PLATINUM)
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
                          assertThat(body.getInteger("basePoints")).isEqualTo(8100);
                          assertThat(body.getInteger("tierBonus")).isEqualTo(4050);
                          assertThat(body.getInteger("promoBonus")).isZero();
                          assertThat(body.getInteger("totalPoints")).isEqualTo(12150);

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should cap total points at 50,000")
  void shouldCapTotalPointsAt50k(VertxTestContext testContext) {
    stubFxRate("USD", 5.0);
    stubPromoCode("MEGA100", 100, false);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(20000.00))
            .currency("USD")
            .cabinClass(CabinClass.FIRST)
            .customerTier(CustomerTier.PLATINUM)
            .promoCode("MEGA100")
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
                          assertThat(body.getInteger("totalPoints"))
                              .isEqualTo(50000)
                              .describedAs("Total points should be capped at 50,000");

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should warn when promo expires soon")
  void shouldWarnWhenPromoExpiresSoon(VertxTestContext testContext) {
    stubFxRate("USD", 3.67);
    stubPromoCode("EXPIRING", 10, true);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(500.00))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.GOLD)
            .promoCode("EXPIRING")
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
                          var warnings = body.getJsonArray("warnings");

                          assertThat(warnings).isNotEmpty().contains("PROMO_EXPIRES_SOON");

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should handle NONE tier with zero bonus")
  void shouldHandleNoneTier(VertxTestContext testContext) {
    stubFxRate("USD", 3.0);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000.00))
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

                          var body = response.bodyAsJsonObject();
                          assertThat(body.getInteger("basePoints")).isEqualTo(3000);
                          assertThat(body.getInteger("tierBonus")).isZero();
                          assertThat(body.getInteger("promoBonus")).isZero();
                          assertThat(body.getInteger("totalPoints")).isEqualTo(3000);

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should test GOLD tier level")
  void shouldTestGoldTierLevel(VertxTestContext testContext) {
    stubFxRate("USD", 2.0);

    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1000.00))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.GOLD)
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
                          assertThat(body.getInteger("basePoints")).isEqualTo(2000);
                          assertThat(body.getInteger("tierBonus")).isEqualTo(600);
                          assertThat(body.getInteger("totalPoints")).isEqualTo(2600);

                          testContext.completeNow();
                        })));
  }
}
