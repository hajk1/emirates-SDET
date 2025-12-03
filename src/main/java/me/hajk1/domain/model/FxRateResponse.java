package me.hajk1.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class FxRateResponse {
  double rate;
  String timestamp; // Changed from Instant to String to avoid issues
}
