package de.caritas.cob.userservice.api.service.user.anonymous;

import de.caritas.cob.userservice.api.authorization.UserRole;
import de.caritas.cob.userservice.api.container.RocketChatCredentials;
import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.exception.rocketchat.RocketChatLoginException;
import de.caritas.cob.userservice.api.facade.CreateUserFacade;
import de.caritas.cob.userservice.api.facade.rollback.RollbackFacade;
import de.caritas.cob.userservice.api.facade.rollback.RollbackUserAccountInformation;
import de.caritas.cob.userservice.api.model.keycloak.KeycloakCreateUserResponseDTO;
import de.caritas.cob.userservice.api.model.keycloak.login.KeycloakLoginResponseDTO;
import de.caritas.cob.userservice.api.model.registration.UserDTO;
import de.caritas.cob.userservice.api.model.rocketchat.login.LoginResponseDTO;
import de.caritas.cob.userservice.api.model.user.AnonymousUserCredentials;
import de.caritas.cob.userservice.api.service.KeycloakService;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientService;
import de.caritas.cob.userservice.api.service.rocketchat.RocketChatService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/**
 * Service to create anonymous user accounts.
 */
@Service
@RequiredArgsConstructor
public class AnonymousUserCreatorService {

  private final @NonNull KeycloakAdminClientService keycloakAdminClientService;
  private final @NonNull CreateUserFacade createUserFacade;
  private final @NonNull KeycloakService keycloakService;
  private final @NonNull RocketChatService rocketChatService;
  private final @NonNull RollbackFacade rollbackFacade;

  /**
   * Creates an anonymous user account in Keycloak, MariaDB and Rocket.Chat.
   *
   * @param userDto {@link UserDTO}
   * @return {@link AnonymousUserCredentials}
   */
  public AnonymousUserCredentials createAnonymousUser(UserDTO userDto) {

    KeycloakCreateUserResponseDTO response = keycloakAdminClientService.createKeycloakUser(userDto);
    createUserFacade.updateKeycloakAccountAndCreateDatabaseUserAccount(response.getUserId(),
        userDto, UserRole.ANONYMOUS);

    KeycloakLoginResponseDTO kcLoginResponseDTO;
    ResponseEntity<LoginResponseDTO> rcLoginResponseDto;
    try {
      kcLoginResponseDTO = keycloakService.loginUser(userDto.getUsername(), userDto.getPassword());
      rcLoginResponseDto =
          rocketChatService.loginUserFirstTime(userDto.getUsername(), userDto.getPassword());
    } catch (RocketChatLoginException | BadRequestException e) {
      rollBackAnonymousUserAccount(response.getUserId());
      throw new InternalServerErrorException(e.getMessage(), LogService::logInternalServerError);
    }

    return AnonymousUserCredentials.builder()
        .userId(response.getUserId())
        .accessToken(kcLoginResponseDTO.getAccessToken())
        .expiresIn(kcLoginResponseDTO.getExpiresIn())
        .refreshToken(kcLoginResponseDTO.getRefreshToken())
        .refreshExpiresIn(kcLoginResponseDTO.getExpiresIn())
        .rocketChatCredentials(obtainRocketChatCredentials(rcLoginResponseDto))
        .build();
  }

  private void rollBackAnonymousUserAccount(String userId) {
    rollbackFacade.rollBackUserAccount(RollbackUserAccountInformation.builder()
        .userId(userId)
        .rollBackUserAccount(true)
        .build());
  }

  private RocketChatCredentials obtainRocketChatCredentials(
      ResponseEntity<LoginResponseDTO> response) {
    return RocketChatCredentials.builder()
        .rocketChatUserId(response.getBody().getData().getUserId())
        .rocketChatToken(response.getBody().getData().getAuthToken())
        .build();
  }
}
