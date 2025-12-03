package me.hajk1.infrastructure.client;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hajk1.domain.model.FxRateResponse;
import me.hajk1.domain.service.FxRateService;

@Slf4j
@RequiredArgsConstructor
public class HttpFxRateService implements FxRateService {

  private final WebClient webClient;
  private final String baseUrl;
  private final int maxAttempts;

  // TODO: Consider adding exponential backoff between retries
  // Currently retries happen immediately, which might overwhelm a struggling service

  @Override
  public Future<Double> getRate(String fromCurrency, String toCurrency) {
    return fetchWithRetry(fromCurrency, toCurrency, 1);
  }

  private Future<Double> fetchWithRetry(String from, String to, int attemptNumber) {
    log.debug("Fetching FX rate: {} -> {} (attempt {}/{})", from, to, attemptNumber, maxAttempts);

    return webClient
        .getAbs(baseUrl + "/fx/rates")
        .addQueryParam("from", from)
        .addQueryParam("to", to)
        .send()
        .compose(
            response -> {
              // Success case
              if (response.statusCode() == 200) {
                FxRateResponse fxRate = response.bodyAsJsonObject().mapTo(FxRateResponse.class);
                log.debug("FX rate received: {}", fxRate.getRate());
                return Future.succeededFuture(fxRate.getRate());
              }

              // Retry on 5xx errors only (server errors, not client errors)
              if (response.statusCode() >= 500 && attemptNumber < maxAttempts) {
                log.warn(
                    "FX service error ({}), retrying... (attempt {}/{})",
                    response.statusCode(),
                    attemptNumber,
                    maxAttempts);
                return fetchWithRetry(from, to, attemptNumber + 1);
              }

              // Note: Originally had a .recover() block here too, which caused double retries
              // (15 attempts instead of 3!). Took me a while to debug with WireMock verify.
              // Lesson learned: handle everything in compose() to avoid duplicate retry logic.
              String error =
                  String.format(
                      "FX service returned: %d after %d attempts",
                      response.statusCode(), attemptNumber);
              log.error(error);
              return Future.failedFuture(error);
            });
    // Removed recover block entirely - HTTP errors are handled in compose
  }
}
