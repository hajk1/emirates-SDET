package me.hajk1.domain.service;

import io.vertx.core.Future;
import me.hajk1.domain.model.PromoDetails;

public interface PromoService {
  Future<PromoDetails> getPromoDetails(String promoCode);
}
