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

@DisplayName("Points Quote - Validation")
class ValidationComponentTest extends ComponentTestBase {

  @Test
  @DisplayName("Should reject negative fare amount")
  void shouldRejectNegativeFare(VertxTestContext testContext) {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(-100))
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
                          assertThat(response.statusCode()).isEqualTo(400);

                          var body = response.bodyAsJsonObject();
                          assertThat(body.getString("error"))
                              .contains("Fare amount must be positive");

                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should reject zero fare amount")
  void shouldRejectZeroFare(VertxTestContext testContext) {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.ZERO)
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
                          assertThat(response.statusCode()).isEqualTo(400);
                          assertThat(response.bodyAsJsonObject().getString("error"))
                              .contains("positive");
                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should reject null currency")
  void shouldRejectNullCurrency(VertxTestContext testContext) {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(100))
            .currency(null)
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
                          assertThat(response.statusCode()).isEqualTo(400);
                          assertThat(response.bodyAsJsonObject().getString("error"))
                              .contains("Currency");
                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should reject blank currency")
  void shouldRejectBlankCurrency(VertxTestContext testContext) {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(100))
            .currency("  ")
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
                          assertThat(response.statusCode()).isEqualTo(400);
                          assertThat(response.bodyAsJsonObject().getString("error"))
                              .contains("Currency");
                          testContext.completeNow();
                        })));
  }

  @Test
  @DisplayName("Should reject null cabin class")
  void shouldRejectNullCabinClass(VertxTestContext testContext) {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(100))
            .currency("USD")
            .cabinClass(null)
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
                          assertThat(response.statusCode()).isEqualTo(400);
                          assertThat(response.bodyAsJsonObject().getString("error"))
                              .contains("Cabin class");
                          testContext.completeNow();
                        })));
  }
}
