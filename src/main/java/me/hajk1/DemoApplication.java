package me.hajk1;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import me.hajk1.infrastructure.config.JacksonConfig;

@Slf4j
public class DemoApplication {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    // Configure Jackson
    JacksonConfig.configure();

    log.info("Starting demo application with embedded mock services...");

    // Start mock FX service on port 9090
    startMockFxService(vertx);

    // Start mock Promo service on port 9091
    startMockPromoService(vertx);

    // Wait a bit for mocks to start
    vertx.setTimer(
        500,
        id -> {
          // Start main application
          JsonObject config =
              new JsonObject()
                  .put("http.port", 8080)
                  .put("fx.service.url", "http://localhost:9090")
                  .put("promo.service.url", "http://localhost:9091")
                  .put("fx.retry.maxAttempts", 3)
                  .put("promo.timeout.ms", 2000);

          vertx
              .deployVerticle(
                  new Application(), new io.vertx.core.DeploymentOptions().setConfig(config))
              .onSuccess(
                  deploymentId -> {
                    log.info("✅ Loyalty Points Service started on http://localhost:8080");
                    log.info(
                        "Try: curl -X POST http://localhost:8080/v1/points/quote -H 'Content-Type: application/json' -d '{\"fareAmount\":1234.50,\"currency\":\"USD\",\"cabinClass\":\"ECONOMY\",\"customerTier\":\"SILVER\",\"promoCode\":\"SUMMER25\"}'");
                  })
              .onFailure(
                  err -> {
                    log.error("❌ Failed to start application: {}", err.getMessage());
                    System.exit(1);
                  });
        });
  }

  private static void startMockFxService(Vertx vertx) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router
        .get("/fx/rates")
        .handler(
            ctx -> {
              String from = ctx.request().getParam("from");
              String to = ctx.request().getParam("to");

              log.info("📞 FX Service: {} -> {}", from, to);

              // Mock FX rates
              double rate =
                  switch (from) {
                    case "USD" -> 3.67;
                    case "EUR" -> 4.05;
                    case "GBP" -> 4.73;
                    case "JPY" -> 0.025;
                    default -> 1.0;
                  };

              ctx.response()
                  .putHeader("Content-Type", "application/json")
                  .end(
                      new JsonObject()
                          .put("rate", rate)
                          .put("timestamp", "2025-12-03T10:00:00Z")
                          .encode());
            });

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(9090)
        .onSuccess(server -> log.info("✅ Mock FX Service on http://localhost:9090"))
        .onFailure(err -> log.error("❌ Failed to start FX service: {}", err.getMessage()));
  }

  private static void startMockPromoService(Vertx vertx) {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    router
        .get("/promo/:code")
        .handler(
            ctx -> {
              String code = ctx.pathParam("code");

              log.info("📞 Promo Service: {}", code);

              // Mock promo codes
              JsonObject promo =
                  switch (code) {
                    case "SUMMER25" ->
                        new JsonObject()
                            .put("code", "SUMMER25")
                            .put("bonusPercentage", 25)
                            .put("expiresInDays", 30);
                    case "WINTER50" ->
                        new JsonObject()
                            .put("code", "WINTER50")
                            .put("bonusPercentage", 50)
                            .put("expiresInDays", 5);
                    case "MEGA100" ->
                        new JsonObject()
                            .put("code", "MEGA100")
                            .put("bonusPercentage", 100)
                            .put("expiresInDays", 15);
                    default -> null;
                  };

              if (promo == null) {
                ctx.response().setStatusCode(404).end();
              } else {
                ctx.response().putHeader("Content-Type", "application/json").end(promo.encode());
              }
            });

    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(9091)
        .onSuccess(server -> log.info("✅ Mock Promo Service on http://localhost:9091"))
        .onFailure(err -> log.error("❌ Failed to start Promo service: {}", err.getMessage()));
  }
}
