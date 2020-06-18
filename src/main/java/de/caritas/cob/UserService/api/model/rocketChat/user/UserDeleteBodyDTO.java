package de.caritas.cob.UserService.api.model.rocketChat.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 
 * Body object for Rocket.Chat API Call for deleting an user
 * https://rocket.chat/docs/developer-guides/rest-api/users/delete/
 * 
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UserDeleteBodyDTO {

  private String userId;
}
