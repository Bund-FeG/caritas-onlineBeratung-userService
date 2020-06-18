package de.caritas.cob.UserService.api.model.rocketChat.room;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Rocket.Chat rooms.get DTO
 *
 */

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RoomsGetDTO {

  private RoomsUpdateDTO[] update;
  private boolean success;
  private String status;
  private String message;

}
