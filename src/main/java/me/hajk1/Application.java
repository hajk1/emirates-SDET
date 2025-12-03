package me.hajk1;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;
import me.hajk1.domain.service.PointsCalculationService;
import me.hajk1.domain.service.PointsCalculationServiceImpl;
import me.hajk1.infrastructure.client.HttpFxRateService;
import me.hajk1.infrastructure.client.HttpPromoService;
import me.hajk1.infrastructure.config.JacksonConfig;
import me.hajk1.infrastructure.http.PointsQuoteHandler;

@Slf4j
public class Application extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) {
    // Configure Jackson first - this is important!
    // Without this, Instant deserialization fails
    JacksonConfig.configure();

    JsonObject config = config();

    // Create web client
    WebClient webClient = WebClient.create(vertx);

    // Create services
    var fxService =
        new HttpFxRateService(
            webClient,
            config.getString("fx.service.url"),
            config.getInteger("fx.retry.maxAttempts", 3) // This now means 3 total attempts
            );

    var promoService =
        new HttpPromoService(
            webClient,
            config.getString("promo.service.url"),
            config.getLong("promo.timeout.ms", 2000L));

    PointsCalculationService calculationService =
        new PointsCalculationServiceImpl(fxService, promoService);

    // Create router
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    router.post("/v1/points/quote").handler(new PointsQuoteHandler(calculationService));

    // Start server
    int port = config.getInteger("http.port", 8080);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(port)
        .onSuccess(
            server -> {
              log.info("Server started on port {}", server.actualPort());
              startPromise.complete();
            })
        .onFailure(
            ex -> {
              log.error("Failed to start server", ex);
              startPromise.fail(ex);
            });
  }
}
