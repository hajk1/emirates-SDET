package me.hajk1.domain.service;

import io.vertx.core.Future;
import me.hajk1.domain.model.PointsQuoteRequest;
import me.hajk1.domain.model.PointsQuoteResponse;

public interface PointsCalculationService {
  Future<PointsQuoteResponse> calculatePoints(PointsQuoteRequest request);
}
