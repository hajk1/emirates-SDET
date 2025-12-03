package me.hajk1.domain.service;

public class ValidationException extends RuntimeException {
  public ValidationException(String fareAmountMustBePositive) {
    super(fareAmountMustBePositive);
  }
}
