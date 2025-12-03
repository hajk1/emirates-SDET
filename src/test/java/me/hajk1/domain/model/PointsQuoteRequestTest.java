package me.hajk1.domain.model;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;
import me.hajk1.domain.service.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PointsQuoteRequestTest {

  @Test
  void shouldRejectNegativeFareAmount() {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(-100))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .build();

    assertThatThrownBy(request::validate)
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("positive");
  }

  @Test
  void shouldRejectZeroFareAmount() {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.ZERO)
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .build();

    assertThatThrownBy(request::validate)
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("positive");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "   "})
  void shouldRejectInvalidCurrency(String currency) {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(100))
            .currency(currency)
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .build();

    assertThatThrownBy(request::validate)
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Currency");
  }

  @Test
  void shouldAcceptValidRequest() {
    var request =
        PointsQuoteRequest.builder()
            .fareAmount(BigDecimal.valueOf(1234.50))
            .currency("USD")
            .cabinClass(CabinClass.ECONOMY)
            .customerTier(CustomerTier.SILVER)
            .promoCode("SUMMER25")
            .build();

    assertThatNoException().isThrownBy(request::validate);
  }
}
