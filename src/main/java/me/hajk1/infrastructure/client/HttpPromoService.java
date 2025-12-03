package me.hajk1.infrastructure.client;

import io.vertx.core.Future;
import io.vertx.ext.web.client.WebClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hajk1.domain.model.PromoDetails;
import me.hajk1.domain.service.PromoService;

@Slf4j
@RequiredArgsConstructor
public class HttpPromoService implements PromoService {

  private final WebClient webClient;
  private final String baseUrl;
  private final long timeoutMs;

  @Override
  public Future<PromoDetails> getPromoDetails(String promoCode) {
    log.debug("Fetching promo details for: {}", promoCode);

    return webClient
        .getAbs(baseUrl + "/promo/" + promoCode)
        .timeout(timeoutMs)
        .send()
        .compose(
            response -> {
              if (response.statusCode() == 200) {
                PromoDetails promo = response.bodyAsJsonObject().mapTo(PromoDetails.class);
                return Future.succeededFuture(promo);
              } else if (response.statusCode() == 404) {
                // Invalid promo codes are not errors - just mean no promo applies
                // This is intentional graceful degradation
                return Future.failedFuture("Promo code not found");
              } else {
                return Future.failedFuture("Promo service error: " + response.statusCode());
              }
            });
  }
}
