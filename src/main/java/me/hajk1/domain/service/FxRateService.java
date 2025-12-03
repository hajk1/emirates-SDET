package me.hajk1.domain.service;

import io.vertx.core.Future;

public interface FxRateService {
  Future<Double> getRate(String fromCurrency, String toCurrency);
}
