package me.hajk1.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import me.hajk1.domain.service.ValidationException;

@Value
@Builder
@Jacksonized
public class PointsQuoteRequest {
  BigDecimal fareAmount;
  String currency;
  CabinClass cabinClass;
  CustomerTier customerTier;
  String promoCode;

  public void validate() {
    if (fareAmount == null || fareAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ValidationException("Fare amount must be positive");
    }
    if (currency == null || currency.isBlank()) {
      throw new ValidationException("Currency is required");
    }
    if (cabinClass == null) {
      throw new ValidationException("Cabin class is required");
    }
  }
}
