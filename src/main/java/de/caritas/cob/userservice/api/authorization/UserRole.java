package de.caritas.cob.userservice.api.authorization;

import java.util.Arrays;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserRole {

  USER("user"),
  CONSULTANT("consultant"),
  TECHNICAL("technical"),
  U25_CONSULTANT("u25-consultant"),
  U25_MAIN_CONSULTANT("u25-main-consultant"),
  KREUZBUND_CONSULTANT("kreuzbund-consultant"),
  USER_ADMIN("user-admin");

  private final String value;

  public static Optional<UserRole> getRoleByValue(String value) {
    return Arrays.stream(values()).filter(userRole -> userRole.value.equals(value)).findFirst();
  }
}
