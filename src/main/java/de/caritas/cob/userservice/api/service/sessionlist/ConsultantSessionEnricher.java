package de.caritas.cob.userservice.api.service.sessionlist;

import de.caritas.cob.userservice.api.container.RocketChatCredentials;
import de.caritas.cob.userservice.api.container.RocketChatRoomInformation;
import de.caritas.cob.userservice.api.facade.sessionlist.RocketChatRoomInformationProvider;
import de.caritas.cob.userservice.api.helper.Helper;
import de.caritas.cob.userservice.api.helper.SessionListAnalyser;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.model.ConsultantSessionResponseDTO;
import de.caritas.cob.userservice.api.model.SessionDTO;
import de.caritas.cob.userservice.api.repository.consultant.Consultant;
import de.caritas.cob.userservice.api.repository.session.ConsultingType;
import de.caritas.cob.userservice.api.service.sessionlist.AvailableLastMessageUpdater;
import java.util.Optional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.service.spi.ServiceException;
import org.springframework.stereotype.Service;

/**
 * Service class to enrich a session of an consultant with required Rocket.Chat data.
 */
@Service
@RequiredArgsConstructor
public class ConsultantSessionEnricher {

  private final @NonNull SessionListAnalyser sessionListAnalyser;
  private final @NonNull RocketChatRoomInformationProvider rocketChatRoomInformationProvider;
  private final @NonNull ConsultingTypeManager consultingTypeManager;

  /**
   * Enriches the given session with the following information from Rocket.Chat. "last message",
   * "last message date", and "messages read".
   *
   * @param consultantSessionResponseDTO the session to be enriched
   * @param rcToken                      the Rocket.Chat authentiaction token of the current
   *                                     consultant
   * @param consultant                   the {@link Consultant}
   * @return the enriched {@link ConsultantSessionResponseDTO}
   */
  public ConsultantSessionResponseDTO updateRequiredConsultantSessionValues(
      ConsultantSessionResponseDTO consultantSessionResponseDTO, String rcToken,
      Consultant consultant) {

    var rocketChatRoomInformation = this.rocketChatRoomInformationProvider
        .retrieveRocketChatInformation(RocketChatCredentials.builder()
            .rocketChatToken(rcToken)
            .rocketChatUserId(consultant.getRocketChatId())
            .build());

    SessionDTO session = consultantSessionResponseDTO.getSession();
    String groupId = session.getGroupId();

    session.setMonitoring(getMonitoringProperty(session));

    session.setMessagesRead(sessionListAnalyser.areMessagesForRocketChatGroupReadByUser(
        rocketChatRoomInformation.getReadMessages(), groupId));

    if (sessionListAnalyser.isLastMessageForRocketChatGroupIdAvailable(
        rocketChatRoomInformation.getLastMessagesRoom(), groupId)) {
      new AvailableLastMessageUpdater(this.sessionListAnalyser)
          .updateSessionWithAvailableLastMessage(rocketChatRoomInformation,
              consultant.getRocketChatId(), consultantSessionResponseDTO::setLatestMessage, session,
              groupId);
    } else {
      setFallbackDate(consultantSessionResponseDTO, session);
    }

    // Due to a Rocket.Chat bug the read state is only set, when a message was posted
    if (isFeedbackFlagAvailable(rocketChatRoomInformation, consultantSessionResponseDTO)) {
      session.setFeedbackRead(
          rocketChatRoomInformation.getReadMessages().get(session.getFeedbackGroupId()));
    } else {
      // Fallback: If map doesn't contain feedback group id set to true -> no feedback label in frontend application
      session.setFeedbackRead(!rocketChatRoomInformation.getLastMessagesRoom()
          .containsKey(session.getFeedbackGroupId()));
    }

    return consultantSessionResponseDTO;
  }

  private boolean getMonitoringProperty(SessionDTO session) {

    Optional<ConsultingType> consultingType = ConsultingType.valueOf(session.getConsultingType());

    if (consultingType.isEmpty()) {
      throw new ServiceException(String
          .format("Session with id %s does not have a valid consulting type.", session.getId()));
    }
    var consultingTypeSettings =
        consultingTypeManager.getConsultingTypeSettings(consultingType.get());

    return consultingTypeSettings.isMonitoring();
  }

  private void setFallbackDate(ConsultantSessionResponseDTO consultantSessionResponseDTO,
      SessionDTO session) {
    session.setMessageDate(Helper.UNIXTIME_0.getTime());
    consultantSessionResponseDTO.setLatestMessage(Helper.UNIXTIME_0);
  }

  private boolean isFeedbackFlagAvailable(RocketChatRoomInformation rocketChatRoomInformation,
      ConsultantSessionResponseDTO session) {
    return rocketChatRoomInformation.getLastMessagesRoom()
        .containsKey(session.getSession().getFeedbackGroupId())
        && rocketChatRoomInformation.getReadMessages()
        .containsKey(session.getSession().getFeedbackGroupId());
  }

}
