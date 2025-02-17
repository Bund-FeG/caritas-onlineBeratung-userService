package de.caritas.cob.userservice.api.admin.service.rocketchat;

import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.facade.RocketChatFacade;
import de.caritas.cob.userservice.api.repository.consultant.Consultant;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientService;
import java.util.List;
import java.util.function.Consumer;

/**
 * Provider for group operations with Rocket.Chat.
 */
public class RocketChatAddToGroupOperationService extends RocketChatGroupOperation {

  private List<Session> sessions;
  private Consultant consultant;

  private RocketChatAddToGroupOperationService(RocketChatFacade rocketChatFacade,
      KeycloakAdminClientService keycloakAdminClientService, Consumer<String> logMethod) {
    super(rocketChatFacade, keycloakAdminClientService);
    this.logMethod = logMethod;
  }

  /**
   * Creates the {@link RocketChatAddToGroupOperationService} instance.
   *
   * @param rocketChatFacade the target service to perform operations
   * @return the {@link RocketChatAddToGroupOperationService} instance
   */
  public static RocketChatAddToGroupOperationService getInstance(RocketChatFacade rocketChatFacade,
      KeycloakAdminClientService keycloakAdminClientService, Consumer<String> logMethod) {
    return new RocketChatAddToGroupOperationService(rocketChatFacade, keycloakAdminClientService,
        logMethod);
  }

  /**
   * Sets the {@link Session} list for group operations.
   *
   * @param sessions the {@link Session} list
   * @return the {@link RocketChatAddToGroupOperationService} instance
   */
  public RocketChatAddToGroupOperationService onSessions(List<Session> sessions) {
    this.sessions = sessions;
    return this;
  }

  /**
   * Sets the {@link Consultant}.
   *
   * @param consultant the consultant to add in the groups
   * @return the {@link RocketChatAddToGroupOperationService} instance
   */
  public RocketChatAddToGroupOperationService withConsultant(Consultant consultant) {
    this.consultant = consultant;
    return this;
  }

  /**
   * Adds the user to configured groups.
   */
  public void addToGroupsOrRollbackOnFailure() {
    this.sessions.forEach(this::addToSpecificSessionOrRollbackOnFailure);
  }

  private void addToSpecificSessionOrRollbackOnFailure(Session session) {
    try {
      addConsultantToGroupOfSession(session, this.consultant);
    } catch (Exception e) {
      rollback();
      throw new InternalServerErrorException(
          String.format(
              "ERROR: Consultant could not be added to rc group %s: Technical user could not be"
                  + " added to group (%s).",
              session.getGroupId(), resolveTypeOfSession(session)), e,
          LogService::logRocketChatError);
    }
  }

  private void rollback() {
    this.sessions.forEach(this::removeUserFromSession);
  }

  private void removeUserFromSession(Session session) {
    try {
      removeConsultantFromSession(session, this.consultant);
    } catch (Exception e) {
      throw new InternalServerErrorException(
          String.format("ERROR: Failed to rollback %s of group %s:",
              resolveTypeOfSession(session), session.getGroupId()), e,
          LogService::logRocketChatError);
    }
  }

}
