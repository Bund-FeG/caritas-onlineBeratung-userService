package de.caritas.cob.userservice.api.service.session;

import static de.caritas.cob.userservice.localdatetime.CustomLocalDateTime.nowInUtc;
import static java.util.Collections.emptyList;
import static java.util.Objects.nonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import de.caritas.cob.userservice.api.authorization.UserRole;
import de.caritas.cob.userservice.api.exception.UpdateFeedbackGroupIdException;
import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.exception.httpresponses.ForbiddenException;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.exception.httpresponses.NotFoundException;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeSettings;
import de.caritas.cob.userservice.api.model.AgencyDTO;
import de.caritas.cob.userservice.api.model.ConsultantSessionDTO;
import de.caritas.cob.userservice.api.model.ConsultantSessionResponseDTO;
import de.caritas.cob.userservice.api.model.UserSessionResponseDTO;
import de.caritas.cob.userservice.api.model.registration.UserDTO;
import de.caritas.cob.userservice.api.model.user.SessionConsultantForUserDTO;
import de.caritas.cob.userservice.api.repository.consultant.Consultant;
import de.caritas.cob.userservice.api.repository.consultantagency.ConsultantAgency;
import de.caritas.cob.userservice.api.repository.session.ConsultingType;
import de.caritas.cob.userservice.api.repository.session.RegistrationType;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.repository.session.SessionRepository;
import de.caritas.cob.userservice.api.repository.session.SessionStatus;
import de.caritas.cob.userservice.api.repository.user.User;
import de.caritas.cob.userservice.api.service.agency.AgencyService;
import de.caritas.cob.userservice.api.service.ConsultantService;
import de.caritas.cob.userservice.api.service.LogService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service for sessions
 */
@Service
@RequiredArgsConstructor
public class SessionService {

  private final @NonNull SessionRepository sessionRepository;
  private final @NonNull AgencyService agencyService;
  private final @NonNull ConsultantService consultantService;
  private final @NonNull ConsultingTypeManager consultingTypeManager;

  /**
   * Returns the sessions for a user
   *
   * @return the sessions
   */
  public List<Session> getSessionsForUser(User user) {
    return sessionRepository.findByUser(user);
  }

  /**
   * Returns the session for the provided sessionId.
   *
   * @param sessionId the session ID
   * @return {@link Session}
   */
  public Optional<Session> getSession(Long sessionId) {
    return sessionRepository.findById(sessionId);
  }

  /**
   * Returns the sessions for the given user and consultingType.
   *
   * @param user {@link User}
   * @return list of {@link Session}
   */
  public List<Session> getSessionsForUserByConsultingType(User user,
      ConsultingType consultingType) {
    return sessionRepository.findByUserAndConsultingType(user, consultingType);
  }

  /**
   * Updates the given session by assigning the provided consultant and {@link SessionStatus}.
   *
   * @param session    the session
   * @param consultant the consultant
   * @param status     the status of the session
   */
  public void updateConsultantAndStatusForSession(Session session, Consultant consultant,
      SessionStatus status) {
    session.setConsultant(consultant);
    session.setStatus(status);
    saveSession(session);
  }

  /**
   * Updates the feedback group id of the given {@link Session}.
   *
   * @param session         an optional session
   * @param feedbackGroupId the ID of the feedback group
   */
  public void updateFeedbackGroupId(Session session, String feedbackGroupId)
      throws UpdateFeedbackGroupIdException {
    try {
      session.setFeedbackGroupId(feedbackGroupId);
      saveSession(session);

    } catch (InternalServerErrorException serviceException) {
      throw new UpdateFeedbackGroupIdException(
          String.format("Could not update feedback group id %s for session %s", feedbackGroupId,
              session.getId()), serviceException);
    }
  }

  /**
   * Returns a list of current sessions (no matter if an enquiry message has been written or not)
   * for the provided user ID.
   *
   * @param userId Keycloak/MariaDB user ID
   * @return {@link List} of {@link UserSessionResponseDTO}
   */
  public List<UserSessionResponseDTO> getSessionsForUserId(String userId) {
    List<UserSessionResponseDTO> sessionResponseDTOs = new ArrayList<>();
    List<Session> sessions = sessionRepository.findByUserUserId(userId);
    if (isNotEmpty(sessions)) {
      List<AgencyDTO> agencies =
          agencyService.getAgencies(
              sessions.stream().map(Session::getAgencyId).collect(Collectors.toList()));
      sessionResponseDTOs = convertToUserSessionResponseDTO(sessions, agencies);
    }
    return sessionResponseDTOs;
  }

  /**
   * Initialize a {@link Session} as initial registered enquiry.
   *
   * @param user                   the user
   * @param userDto                the dto of the user
   * @return the initialized session
   */
  public Session initializeSession(User user, UserDTO userDto, boolean isTeamSession) {
    return initializeSession(user, userDto, isTeamSession, RegistrationType.REGISTERED,
        SessionStatus.INITIAL);
  }

  /**
   * Initialize a {@link Session}.
   *
   * @param user                   {@link User}
   * @param userDto                {@link UserDTO}
   * @param isTeamSession          is team session flag
   * @param registrationType       {@link RegistrationType}
   * @param sessionStatus          {@link SessionStatus}
   * @return the initialized {@link Session}
   */
  public Session initializeSession(User user, UserDTO userDto, boolean isTeamSession,
      RegistrationType registrationType, SessionStatus sessionStatus) {
    var consultingTypeSettings = obtainConsultingTypeSettings(userDto);
    var session = Session.builder()
        .user(user)
        .consultingType(consultingTypeSettings.getConsultingType())
        .registrationType(registrationType)
        .postcode(userDto.getPostcode())
        .agencyId(userDto.getAgencyId())
        .status(sessionStatus)
        .teamSession(isTeamSession)
        .monitoring(consultingTypeSettings.isMonitoring())
        .createDate(nowInUtc())
        .updateDate(nowInUtc())
        .build();

    return saveSession(session);
  }

  private ConsultingTypeSettings obtainConsultingTypeSettings(UserDTO userDTO) {
    return consultingTypeManager.getConsultingTypeSettings(
        ConsultingType.fromConsultingType(userDTO.getConsultingType()));
  }

  /**
   * Save a {@link Session} to the database.
   *
   * @param session the session
   * @return the {@link Session}
   */
  public Session saveSession(Session session) {
    return sessionRepository.save(session);
  }

  /**
   * Returns a list of {@link ConsultantSessionResponseDTO} containing team sessions excluding
   * sessions which are taken by the consultant.
   *
   * @param consultant the consultant
   * @return A list of {@link ConsultantSessionResponseDTO}
   */
  public List<ConsultantSessionResponseDTO> getTeamSessionsForConsultant(Consultant consultant) {

    List<Session> sessions = null;

    Set<ConsultantAgency> consultantAgencies = consultant.getConsultantAgencies();
    if (nonNull(consultantAgencies)) {
      List<Long> consultantAgencyIds = consultantAgencies.stream()
          .map(ConsultantAgency::getAgencyId).collect(Collectors.toList());

      sessions = sessionRepository
          .findByAgencyIdInAndConsultantNotAndStatusAndTeamSessionOrderByEnquiryMessageDateAsc(
              consultantAgencyIds, consultant, SessionStatus.IN_PROGRESS, true);
    }

    List<ConsultantSessionResponseDTO> sessionDTOs = null;

    if (nonNull(sessions)) {
      sessionDTOs = sessions.stream()
          .map(session -> new SessionMapper().toConsultantSessionDto(session))
          .collect(Collectors.toList());
    }

    return sessionDTOs;
  }

  /**
   * Retrieves all related enquiries of given {@link Consultant}.
   *
   * @param consultant the consultant
   * @return the related {@link ConsultantSessionResponseDTO}s
   */
  public List<ConsultantSessionResponseDTO> getEnquiriesForConsultant(Consultant consultant) {
    return getEnquiriesForConsultant(consultant, null);
  }

  public List<ConsultantSessionResponseDTO> getEnquiriesForConsultant(Consultant consultant,
      RegistrationType registrationType) {
    Set<ConsultantAgency> consultantAgencies = consultant.getConsultantAgencies();
    if (isNotEmpty(consultantAgencies)) {
      return retrieveEnquiriesForConsultantAgencies(consultantAgencies, registrationType);
    }
    return emptyList();
  }

  private List<ConsultantSessionResponseDTO> retrieveEnquiriesForConsultantAgencies(
      Set<ConsultantAgency> consultantAgencies, RegistrationType registrationType) {
    List<Long> consultantAgencyIds = consultantAgencies.stream()
        .map(ConsultantAgency::getAgencyId)
        .collect(Collectors.toList());

    final List<Session> sessions = retrieveSessionsByRegistrationType(
        registrationType, consultantAgencyIds);

    return sessions.stream()
        .map(session -> new SessionMapper().toConsultantSessionDto(session))
        .collect(Collectors.toList());
  }

  private List<Session> retrieveSessionsByRegistrationType(RegistrationType registrationType,
      List<Long> consultantAgencyIds) {
    final List<Session> sessions;
    if (registrationType != null) {
      sessions = this.sessionRepository
          .findByAgencyIdInAndConsultantIsNullAndStatusAndRegistrationTypeOrderByEnquiryMessageDateAsc(
              consultantAgencyIds, SessionStatus.NEW, registrationType);
    } else {
      sessions = this.sessionRepository
          .findByAgencyIdInAndConsultantIsNullAndStatusOrderByEnquiryMessageDateAsc(
              consultantAgencyIds, SessionStatus.NEW);
    }
    return sessions;
  }

  /**
   * Retrieves all related active sessions of given {@link Consultant}.
   *
   * @param consultant the consultant
   * @return the related {@link ConsultantSessionResponseDTO}s
   */
  public List<ConsultantSessionResponseDTO> getActiveSessionsForConsultant(Consultant consultant) {
    return sessionRepository.findByConsultantAndStatus(consultant, SessionStatus.IN_PROGRESS)
        .stream()
        .map(session -> new SessionMapper().toConsultantSessionDto(session))
        .collect(Collectors.toList());
  }

  private List<UserSessionResponseDTO> convertToUserSessionResponseDTO(List<Session> sessions,
      List<AgencyDTO> agencies) {
    return sessions.stream()
        .map(session -> buildUserSessionDTO(session, agencies))
        .collect(Collectors.toList());
  }

  private UserSessionResponseDTO buildUserSessionDTO(Session session, List<AgencyDTO> agencies) {
    return new UserSessionResponseDTO()
        .session(new SessionMapper().convertToSessionDTO(session))
        .agency(agencies.stream()
            .filter(agency -> agency.getId().longValue() == session.getAgencyId().longValue())
            .findAny()
            .orElseThrow())
        .consultant(nonNull(session.getConsultant()) ? convertToSessionConsultantForUserDTO(
            session.getConsultant()) : null);
  }

  private SessionConsultantForUserDTO convertToSessionConsultantForUserDTO(Consultant consultant) {
    return new SessionConsultantForUserDTO(consultant.getUsername(), consultant.isAbsent(),
        consultant.getAbsenceMessage());
  }

  /**
   * Delete a {@link Session}
   *
   * @param session the {@link Session}
   */
  public void deleteSession(Session session) {
    sessionRepository.delete(session);
  }

  /**
   * Returns the session for the provided Rocket.Chat group ID. Logs a warning if the given user is
   * not allowed to access this session.
   *
   * @param rcGroupId Rocket.Chat group ID
   * @param userId    Rocket.Chat user ID
   * @param roles     user roles
   * @return {@link Session}
   */
  public Session getSessionByGroupIdAndUser(String rcGroupId, String userId, Set<String> roles) {
    var session = getSessionByGroupId(rcGroupId);
    checkUserPermissionForSession(session, userId, roles);

    return session;
  }

  private Session getSessionByGroupId(String rcGroupId) {
    return sessionRepository.findByGroupId(rcGroupId).orElseThrow(
        () -> new NotFoundException(String.format("Session with groupId %s not found.", rcGroupId),
            LogService::logWarn));
  }

  private void checkUserPermissionForSession(Session session, String userId, Set<String> roles) {
    checkForUserOrConsultantRole(roles);
    checkIfUserAndNotOwnerOfSession(session, userId, roles);
    checkIfConsultantAndNotAssignedToSessionOrAgency(session, userId, roles);
  }

  private void checkForUserOrConsultantRole(Set<String> roles) {
    if (!roles.contains(UserRole.USER.getValue())
        && !roles.contains(UserRole.CONSULTANT.getValue())) {
      throw new ForbiddenException("No user or consultant role to retrieve sessions",
          LogService::logForbidden);
    }
  }

  private void checkIfUserAndNotOwnerOfSession(Session session, String userId, Set<String> roles) {
    if (roles.contains(UserRole.USER.getValue()) && !session.getUser().getUserId().equals(userId)) {
      throw new ForbiddenException(
          String.format("User %s has no permission to access session %s", userId, session.getId()),
          LogService::logForbidden);
    }
  }

  private void checkIfConsultantAndNotAssignedToSessionOrAgency(Session session, String userId,
      Set<String> roles) {
    if (roles.contains(UserRole.CONSULTANT.getValue())) {
      var consultant = this.consultantService.getConsultant(userId)
          .orElseThrow(() -> new BadRequestException(String
              .format("Consultant with id %s does not exist", userId)));
      checkPermissionForConsultantSession(session, consultant);
    }
  }

  /**
   * Returns the session for the Rocket.Chat feedback group id.
   *
   * @param feedbackGroupId the id of the feedback group
   * @return the session
   */
  public Session getSessionByFeedbackGroupId(String feedbackGroupId) {
    return sessionRepository.findByFeedbackGroupId(feedbackGroupId).orElse(null);
  }

  /**
   * Returns a {@link ConsultantSessionDTO} for a specific session.
   *
   * @param sessionId  the session ID to fetch
   * @param consultant the calling consultant
   * @return {@link ConsultantSessionDTO} entity for the specific session
   */
  public ConsultantSessionDTO fetchSessionForConsultant(@NonNull Long sessionId,
      @NonNull Consultant consultant) {

    var session = getSession(sessionId)
        .orElseThrow(
            () -> new NotFoundException(String.format("Session with id %s not found.", sessionId)));
    checkPermissionForConsultantSession(session, consultant);
    return toConsultantSessionDTO(session);
  }

  private ConsultantSessionDTO toConsultantSessionDTO(Session session) {

    return new ConsultantSessionDTO()
        .isTeamSession(session.isTeamSession())
        .agencyId(session.getAgencyId())
        .consultingType(session.getConsultingType().getValue())
        .id(session.getId())
        .status(session.getStatus().getValue())
        .askerId(session.getUser().getUserId())
        .askerRcId(session.getUser().getRcUserId())
        .askerUserName(session.getUser().getUsername())
        .feedbackGroupId(session.getFeedbackGroupId())
        .groupId(session.getGroupId())
        .isMonitoring(session.isMonitoring())
        .postcode(session.getPostcode())
        .consultantId(nonNull(session.getConsultant()) ? session.getConsultant().getId() : null)
        .consultantRcId(
            nonNull(session.getConsultant()) ? session.getConsultant().getRocketChatId() : null);
  }

  private void checkPermissionForConsultantSession(Session session, Consultant consultant) {

    if (!isConsultantAssignedToSession(session, consultant)
        && !(session.isTeamSession() && isConsultantAssignedToSessionAgency(consultant, session))) {
      throw new ForbiddenException(String
          .format("No permission for session %s by consultant %s", session.getId(),
              consultant.getId()));
    }
  }

  private boolean isConsultantAssignedToSession(Session session, Consultant consultant) {
    return nonNull(session.getConsultant())
        && session.getConsultant().getId().equals(consultant.getId());
  }

  private boolean isConsultantAssignedToSessionAgency(Consultant consultant, Session session) {
    return consultant.getConsultantAgencies().stream()
        .anyMatch(consultantAgency -> consultantAgency.getAgencyId().equals(session.getAgencyId()));
  }
}
