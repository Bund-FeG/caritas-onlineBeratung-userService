package de.caritas.cob.UserService.api.model.rocketChat.group;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response object for Rocket.Chat API Call for getting the members of a group
 * https://rocket.chat/docs/developer-guides/rest-api/groups/members/
 *
 */

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class GroupMemberResponseDTO {

  private GroupMemberDTO[] members;
  private String count;
  private String offset;
  private String total;
  private boolean success;
  private String error;
  private String errorType;
}
