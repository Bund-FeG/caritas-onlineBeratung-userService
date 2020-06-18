package de.caritas.cob.UserService.api.model.rocketChat.subscriptions;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rocket.Chat subscriptions.get DTO
 *
 */

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SubscriptionsGetDTO {

  private SubscriptionsUpdateDTO[] update;
  private boolean success;
  private String status;
  private String message;

}
