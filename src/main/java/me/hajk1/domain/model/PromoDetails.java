package me.hajk1.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class PromoDetails {
  String code;
  int bonusPercentage;

  @JsonProperty("expiresInDays")
  int expiresInDays;

  public boolean isExpiringSoon() {
    return expiresInDays <= 7;
  }
}
