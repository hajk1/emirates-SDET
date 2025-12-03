package me.hajk1.domain.model;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class PointsQuoteResponse {
  int basePoints;
  int tierBonus;
  int promoBonus;
  int totalPoints;
  double effectiveFxRate;

  @Builder.Default List<String> warnings = List.of();
}
