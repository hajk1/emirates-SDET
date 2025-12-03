package me.hajk1.infrastructure.http;

import io.vertx.core.Handler;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hajk1.domain.model.PointsQuoteRequest;
import me.hajk1.domain.service.PointsCalculationService;
import me.hajk1.domain.service.ValidationException;

@Slf4j
@RequiredArgsConstructor
public class PointsQuoteHandler implements Handler<RoutingContext> {

  private final PointsCalculationService calculationService;

  @Override
  public void handle(RoutingContext ctx) {
    try {
      JsonObject json = ctx.body().asJsonObject();
      log.debug("Received request: {}", json.encode());

      PointsQuoteRequest request = json.mapTo(PointsQuoteRequest.class);

      calculationService
          .calculatePoints(request)
          .onSuccess(
              response -> {
                log.debug("Calculated response: {}", response);
                ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(200)
                    .end(JsonObject.mapFrom(response).encode());
              })
          .onFailure(ex -> handleError(ctx, ex));

    } catch (DecodeException e) {
      log.error("Failed to decode request", e);
      handleError(ctx, new ValidationException("Invalid JSON format"));
    } catch (Exception e) {
      log.error("Unexpected error", e);
      handleError(ctx, e);
    }
  }

  private void handleError(RoutingContext ctx, Throwable ex) {
    log.error("Error processing request: {}", ex.getMessage(), ex);

    int statusCode = 500;
    String message = "Internal server error";

    if (ex instanceof ValidationException) {
      statusCode = 400;
      message = ex.getMessage();
    }
    // Could refactor this to use a map of exception types -> status codes
    // but keeping it simple for now - YAGNI principle
    ctx.response()
        .putHeader("Content-Type", "application/json")
        .setStatusCode(statusCode)
        .end(new JsonObject().put("error", message).encode());
  }
}
