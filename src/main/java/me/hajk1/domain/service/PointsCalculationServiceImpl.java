package me.hajk1.domain.service;

import io.vertx.core.Future;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.hajk1.domain.model.PointsQuoteRequest;
import me.hajk1.domain.model.PointsQuoteResponse;
import me.hajk1.domain.model.PromoDetails;

@Slf4j
@RequiredArgsConstructor
public class PointsCalculationServiceImpl implements PointsCalculationService {

  private static final int MAX_POINTS = 50_000;
  private static final String TARGET_CURRENCY = "AED";

  private final FxRateService fxRateService;
  private final PromoService promoService;

  @Override
  public Future<PointsQuoteResponse> calculatePoints(PointsQuoteRequest request) {
    log.info("Calculating points for request: {}", request);

    // Validate first
    try {
      request.validate();
    } catch (ValidationException e) {
      log.error("Validation failed: {}", e.getMessage());
      return Future.failedFuture(e);
    }

    // Get FX rate
    return fxRateService
        .getRate(request.getCurrency(), TARGET_CURRENCY)
        .compose(fxRate -> calculateWithFxRate(request, fxRate));
  }

  private Future<PointsQuoteResponse> calculateWithFxRate(
      PointsQuoteRequest request, double fxRate) {
    // NOTE: Initially tried using double here, but ran into precision issues.
    // Example: 1234.50 * 3.67 = 4530.614999999998 (not 4530.615)
    // Switched to BigDecimal to ensure exact financial calculations - this was a painful
    // debugging session before I realized floating-point arithmetic was the culprit!
    int basePoints =
        request
            .getFareAmount()
            .multiply(BigDecimal.valueOf(fxRate))
            .setScale(0, RoundingMode.HALF_UP)
            .intValue();

    // Calculate tier bonus using BigDecimal for precision
    // Truncate (DOWN) instead of rounding to avoid giving extra points
    BigDecimal tierMultiplier = BigDecimal.valueOf(request.getCustomerTier().getMultiplier());
    int tierBonus =
        BigDecimal.valueOf(basePoints)
            .multiply(tierMultiplier)
            .setScale(0, RoundingMode.DOWN) // Truncate
            .intValue();

    // If no promo code, return immediately
    if (request.getPromoCode() == null || request.getPromoCode().isBlank()) {
      int total = Math.min(basePoints + tierBonus, MAX_POINTS);
      return Future.succeededFuture(
          PointsQuoteResponse.builder()
              .basePoints(basePoints)
              .tierBonus(tierBonus)
              .promoBonus(0)
              .totalPoints(total)
              .effectiveFxRate(fxRate)
              .warnings(List.of())
              .build());
    }

    // Fetch promo details
    return promoService
        .getPromoDetails(request.getPromoCode())
        .map(promoDetails -> calculateWithPromo(basePoints, tierBonus, fxRate, promoDetails))
        .otherwise(
            ex -> {
              log.warn("Promo service failed, continuing without promo: {}", ex.getMessage());
              int total = Math.min(basePoints + tierBonus, MAX_POINTS);
              return PointsQuoteResponse.builder()
                  .basePoints(basePoints)
                  .tierBonus(tierBonus)
                  .promoBonus(0)
                  .totalPoints(total)
                  .effectiveFxRate(fxRate)
                  .warnings(List.of())
                  .build();
            });
  }

  private PointsQuoteResponse calculateWithPromo(
      int basePoints, int tierBonus, double fxRate, PromoDetails promo) {
    // Promo applies to base + tier (not just base)
    // This business rule came from the requirements - took me a moment to parse correctly
    int pointsBeforePromo = basePoints + tierBonus;
    int promoBonus =
        BigDecimal.valueOf(pointsBeforePromo)
            .multiply(BigDecimal.valueOf(promo.getBonusPercentage()))
            .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN) // Truncate
            .intValue();

    int totalBeforeCap = basePoints + tierBonus + promoBonus;
    // Cap at 50k - business requirement
    // Important: Apply cap AFTER all bonuses are calculated
    int totalPoints = Math.min(totalBeforeCap, MAX_POINTS);

    List<String> warnings = new ArrayList<>();
    if (promo.isExpiringSoon()) {
      warnings.add("PROMO_EXPIRES_SOON");
    }

    return PointsQuoteResponse.builder()
        .basePoints(basePoints)
        .tierBonus(tierBonus)
        .promoBonus(promoBonus)
        .totalPoints(totalPoints)
        .effectiveFxRate(fxRate)
        .warnings(warnings)
        .build();
  }
}
