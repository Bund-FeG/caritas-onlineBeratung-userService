package de.caritas.cob.userservice.api.service.helper;

import static de.caritas.cob.userservice.api.exception.httpresponses.customheader.HttpStatusExceptionReason.EMAIL_NOT_AVAILABLE;
import static java.util.Objects.isNull;

import de.caritas.cob.userservice.api.authorization.Authorities;
import de.caritas.cob.userservice.api.authorization.UserRole;
import de.caritas.cob.userservice.api.exception.httpresponses.CustomValidationHttpStatusException;
import de.caritas.cob.userservice.api.exception.keycloak.KeycloakException;
import de.caritas.cob.userservice.api.helper.UserHelper;
import de.caritas.cob.userservice.api.model.CreateUserResponseDTO;
import de.caritas.cob.userservice.api.model.keycloak.KeycloakCreateUserResponseDTO;
import de.caritas.cob.userservice.api.model.registration.UserDTO;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.service.helper.aspect.KeycloakAdminClientLogout;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;
import lombok.Synchronized;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ErrorRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Helper class for the KeycloakService. Communicates to the Keycloak Admin API over the Keycloak
 * Admin Client.
 */
@Service
public class KeycloakAdminClientHelper {

  @Value("${keycloak.auth-server-url}")
  private String KEYCLOAK_SERVER_URL;

  @Value("${keycloak.realm}")
  private String KEYCLOAK_REALM;

  @Value("${keycloakService.admin.username}")
  private String KEYCLOAK_USERNAME;

  @Value("${keycloakService.admin.password}")
  private String KEYCLOAK_PASSWORD;

  @Value("${keycloakService.admin.clientId}")
  private String KEYCLOAK_CLIENT_ID;

  @Value("${keycloakService.user.role}")
  private String KEYCLOAK_USER_ROLE;

  @Value("${keycloakService.techuser.id}")
  private String KEYCLOAK_TECH_USER_ID;

  @Value("${api.error.userRegistered}")
  private String ERROR_USER_REGISTERED;

  @Value("${api.error.keycloakError}")
  private String KEYCLOAK_ERROR;

  @Value("${keycloakApi.error.username}")
  private String KEYCLOAK_ERROR_USERNAME;

  @Value("${keycloakApi.error.email}")
  private String KEYCLOAK_ERROR_EMAIL;

  @Value("${user.password.invalid}")
  private String PASSWORD_INVALID;

  @Value("${api.error.emailConflict}")
  private String EMAIL_CONFLICT;

  @Value("${api.error.usernameConflict}")
  private String USERNAME_CONFLICT;

  private Keycloak keycloakInstance;

  @Autowired
  private UserHelper userHelper;

  private Keycloak getInstance() {

    this.keycloakInstance = Keycloak.getInstance(KEYCLOAK_SERVER_URL, KEYCLOAK_REALM,
        KEYCLOAK_USERNAME, KEYCLOAK_PASSWORD, KEYCLOAK_CLIENT_ID);

    return this.keycloakInstance;
  }

  /**
   * Creates a user in Keycloak and returns its Keycloak user ID.
   *
   * @param user {@link UserDTO}
   * @return {@link KeycloakCreateUserResponseDTO}
   */
  public KeycloakCreateUserResponseDTO createKeycloakUser(final UserDTO user) throws Exception {
    return createKeycloakUser(user, null, null);
  }

  /**
   * Creates a user with firstname and lastname in Keycloak and returns its Keycloak user ID.
   *
   * @param user {@link UserDTO}
   * @param firstName first name of user
   * @param lastName last name of user
   * @return {@link KeycloakCreateUserResponseDTO}
   */
  @KeycloakAdminClientLogout
  public KeycloakCreateUserResponseDTO createKeycloakUser(final UserDTO user,
      final String firstName, final String lastName) {
    UserRepresentation kcUser = getUserRepresentation(user, firstName, lastName);
    Response response = getInstance().realm(KEYCLOAK_REALM).users().create(kcUser);
    KeycloakCreateUserResponseDTO keycloakResponse = new KeycloakCreateUserResponseDTO();
    int usernameAvailable = 1;
    int emailAvailable = 1;

    if (response.getStatus() == HttpStatus.CREATED.value()) {
      return new KeycloakCreateUserResponseDTO(getCreatedUserId(response.getLocation()));
    } else {
      String errorMsg = response.readEntity(ErrorRepresentation.class).getErrorMessage();
      keycloakResponse.setStatus(HttpStatus.CONFLICT);

      // Check whether username and/or e-mail address are already taken and set the appropriate
      // error codes and messages
      if (errorMsg.equals(KEYCLOAK_ERROR_EMAIL)) {
        // Only e-mail address is already taken
        emailAvailable = 0;
      } else if (errorMsg.equals(KEYCLOAK_ERROR_USERNAME)) {
        // Username is taken
        usernameAvailable = 0;

        if (!isEmailAvailable(user.getEmail())) {
          // and e-mail address is taken also
          emailAvailable = 0;
        }
      } else {
        throw new KeycloakException(KEYCLOAK_ERROR);
      }
    }

    if (keycloakResponse.getStatus().equals(HttpStatus.CONFLICT)) {
      keycloakResponse.setResponseDTO(
          new CreateUserResponseDTO().usernameAvailable(usernameAvailable)
              .emailAvailable(emailAvailable));
    }

    return keycloakResponse;
  }

  @KeycloakAdminClientLogout
  @Synchronized
  private boolean isEmailAvailable(String email) {
    // Get user resource and change e-mail address of technical user
    UserResource techUserResource =
        getInstance().realm(KEYCLOAK_REALM).users().get(KEYCLOAK_TECH_USER_ID);
    UserRepresentation userRepresentation = techUserResource.toRepresentation();
    String originalEmail = userRepresentation.getEmail();
    userRepresentation.setEmail(email);
    // Try to update technical user's e-mail address
    try {
      techUserResource.update(userRepresentation);
    } catch (Exception e) {
      LogService.logDebug(String.format("E-Mail address already existing in Keycloak: %s", email));
      return false;
    }

    // Reset technical user
    userRepresentation.setEmail(originalEmail);
    techUserResource.update(userRepresentation);

    return true;
  }

  private CredentialRepresentation getCredentialRepresentation(final String password) {
    CredentialRepresentation credentials = new CredentialRepresentation();
    credentials.setType(CredentialRepresentation.PASSWORD);
    credentials.setValue(password);
    credentials.setTemporary(false);

    return credentials;
  }

  private UserRepresentation getUserRepresentation(final UserDTO user, final String firstName,
      final String lastName) {
    UserRepresentation kcUser = new UserRepresentation();
    kcUser.setUsername(user.getUsername());
    kcUser.setEmail(user.getEmail());
    kcUser.setEmailVerified(true);
    if (firstName != null) {
      kcUser.setFirstName(firstName);
    }
    if (lastName != null) {
      kcUser.setLastName(lastName);
    }
    kcUser.setEnabled(true);

    return kcUser;
  }

  private String getCreatedUserId(final URI location) {
    if (location != null) {
      String path = location.getPath();
      return path.substring(path.lastIndexOf('/') + 1);
    }

    return null;
  }

  /**
   * Assigns the role "user" to the given user ID.
   *
   * @param userId Keycloak user ID
   */
  @KeycloakAdminClientLogout
  public void updateUserRole(final String userId) {
    updateRole(userId, KEYCLOAK_USER_ROLE);
  }

  /**
   * Assigns the role with the given name to the given user ID.
   *
   * @param userId Keycloak user ID
   * @param roleName Keycloak role name
   */
  @KeycloakAdminClientLogout
  public void updateRole(final String userId, final String roleName) {
    // Get realm and user resources
    RealmResource realmResource = getInstance().realm(KEYCLOAK_REALM);
    UsersResource userRessource = realmResource.users();
    UserResource user = userRessource.get(userId);
    boolean isRoleUpdated = false;

    // Assign role
    RoleRepresentation roleRepresentation = realmResource.roles().get(roleName).toRepresentation();
    if (isNull(roleRepresentation.getAttributes())) {
      roleRepresentation.setAttributes(new LinkedHashMap<>());
    }
    user.roles().realmLevel()
        .add(Arrays.asList(roleRepresentation));

    // Check if role has been assigned successfully
    List<RoleRepresentation> userRoles = user.roles().realmLevel().listAll();
    for (RoleRepresentation role : userRoles) {
      if (role.toString().toUpperCase().equals(roleName.toUpperCase())) {
        LogService.logDebug(String.format("Added role \"user\" to %s", userId));
        isRoleUpdated = true;
      }
    }

    if (!isRoleUpdated) {
      throw new KeycloakException("Could not update user role");
    }
  }

  /**
   * Updates the Keycloak password for a user.
   *
   * @param userId Keycloak user ID
   * @param password user password
   */
  @KeycloakAdminClientLogout
  public void updatePassword(final String userId, final String password) {
    CredentialRepresentation newCredentials = getCredentialRepresentation(password);
    UserResource userResource = getInstance().realm(KEYCLOAK_REALM).users().get(userId);

    userResource.resetPassword(newCredentials);
    LogService.logDebug(String.format("Updated user credentials for %s", userId));
  }

  /**
   * If user didn't provide an email, set to dummy address (userId@caritas-online-beratung.de). No *
   * success/error status possible, because the Keycloak Client doesn't provide one either. *
   *
   * @param userId Keycloak user ID
   * @param user {@link UserDTO}
   * @return the (dummy) email address
   */
  @KeycloakAdminClientLogout
  public String updateDummyEmail(final String userId, UserDTO user) {
    String dummyEmail = userHelper.getDummyEmail(userId);
    user.setEmail(dummyEmail);
    UserResource userResource = getInstance().realm(KEYCLOAK_REALM).users().get(userId);

    userResource.update(getUserRepresentation(user, null, null));
    LogService.logDebug(String.format("Set email dummy for %s to %s", userId, dummyEmail));

    return dummyEmail;
  }

  /**
   * Updates first name, last name and email address of user wth given id in keycloak.
   *
   * @param userId Keycloak user ID
   * @param userDTO {@link UserDTO}
   * @param firstName the new first name
   * @param lastName the new last name
   */
  @KeycloakAdminClientLogout
  public void updateUserData(final String userId, UserDTO userDTO,
      String firstName, String lastName) {
    if (!isEmailAvailable(userDTO.getEmail())) {
      throw new CustomValidationHttpStatusException(EMAIL_NOT_AVAILABLE);
    }
    UserResource userResource = getInstance().realm(KEYCLOAK_REALM).users().get(userId);
    userResource.update(getUserRepresentation(userDTO, firstName, lastName));
  }

  /**
   * Delete the user if something went wrong during the registration process.
   *
   * @param userId Keycloak user ID
   */
  @KeycloakAdminClientLogout
  public void rollBackUser(String userId) {
    try {
      getInstance().realm(KEYCLOAK_REALM).users().get(userId).remove();
      LogService.logDebug(String.format("User %s has been removed due to rollback", userId));
    } catch (Exception e) {
      LogService
          .logKeycloakError(String.format("User could not be removed/rolled back: %s", userId));
    }
  }

  /**
   * Returns true if the given user has the provided authority.
   *
   * @param userId Keycloak user ID
   * @param authority Keycloak authority
   * @return true if user hast provided authority
   */
  @KeycloakAdminClientLogout
  public boolean userHasAuthority(String userId, String authority) {

    List<RoleRepresentation> userRoles = null;

    try {
      userRoles = getUserRoles(userId);

    } catch (Exception ex) {
      String error = String.format("Could not get roles for user id %s", userId);
      LogService.logKeycloakError(error, ex);
      throw new KeycloakException(error);
    }

    for (RoleRepresentation role : userRoles) {
      Optional<UserRole> userRoleOptional = UserRole.getRoleByValue(role.getName());
      if (userRoleOptional.isPresent()) {
        List<String> authorities = Authorities.getAuthoritiesByUserRole(userRoleOptional.get());
        if (authorities.contains(authority)) {
          return true;
        }
      }
    }

    return false;
  }

  @KeycloakAdminClientLogout
  private List<RoleRepresentation> getUserRoles(String userId) {
    return getInstance().realm(KEYCLOAK_REALM).users().get(userId).roles().realmLevel().listAll();
  }

  /**
   * Returns a list of {@link UserRepresentation} containing all users that match the given search
   * string.
   *
   * @param username Keycloak user name
   * @return {@link List} of found users
   */
  @KeycloakAdminClientLogout
  public List<UserRepresentation> findByUsername(String username) {
    return getInstance().realm(KEYCLOAK_REALM).users().search(username);
  }

  /**
   * Closes the provided session.
   *
   * @param sessionId Keycloak session ID
   */
  public void closeSession(String sessionId) {
    getInstance().realm(KEYCLOAK_REALM).deleteSession(sessionId);
  }

  /**
   * Closes the Keycloak Admin CLI instance.
   */
  public void closeInstance() {
    /**
     * The Keycloak.close() method does actually only close the connection and does NOT delete the
     * session at the moment. There is already an issue for this. Will be implemented in a "future"
     * version: https://issues.jboss.org/browse/KEYCLOAK-7895
     *
     * TODO
     *
     * -> Thus this close() functionality is commented out (to only maintain one open session at
     * once).
     *
     */
    // if (this.keycloakInstance != null && !this.keycloakInstance.isClosed()) {
    // this.keycloakInstance.close();
    // this.keycloakInstance = null;
    // }
  }
}
