package de.caritas.cob.UserService.api.model.rocketChat.login;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * MeDTO for LoginResponseDTO
 *
 */
@Getter
@Setter
@NoArgsConstructor
public class MeDTO {
  private String username;
  private String _id;
  private String status;
  private SettingsDTO settings;
  private List<String> roles;
  private String name;
  private String active;
  private String utcOffset;
  private List<EmailsDTO> emails;
  private String statusConnection;
}
