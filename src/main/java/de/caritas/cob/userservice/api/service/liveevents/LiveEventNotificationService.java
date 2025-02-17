package de.caritas.cob.userservice.api.service.liveevents;

import static de.caritas.cob.userservice.liveservice.generated.web.model.EventType.ANONYMOUSENQUIRYACCEPTED;
import static de.caritas.cob.userservice.liveservice.generated.web.model.EventType.DIRECTMESSAGE;
import static de.caritas.cob.userservice.liveservice.generated.web.model.EventType.NEWANONYMOUSENQUIRY;
import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import de.caritas.cob.userservice.api.helper.AuthenticatedUser;
import de.caritas.cob.userservice.api.repository.user.User;
import de.caritas.cob.userservice.api.service.LogService;
import de.caritas.cob.userservice.api.service.PushMessageService;
import de.caritas.cob.userservice.api.service.user.UserService;
import de.caritas.cob.userservice.liveservice.generated.web.LiveControllerApi;
import de.caritas.cob.userservice.liveservice.generated.web.model.LiveEventMessage;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

/**
 * Service class to provide live event triggers to the live service.
 */
@Service
@RequiredArgsConstructor
public class LiveEventNotificationService {

  private final @NonNull LiveControllerApi liveControllerApi;
  private final @NonNull UserIdsProviderFactory userIdsProviderFactory;
  private final @NonNull AuthenticatedUser authenticatedUser;
  private final @NonNull PushMessageService pushMessageService;
  private final @NonNull UserService userService;

  private static final String RC_GROUP_ID_MESSAGE_TEMPLATE = "Rocket.Chat group ID: %s";
  private static final String NEW_ANONYMOUS_ENQUIRY_MESSAGE_TEMPLATE =
      "Anonymous Enquiry ID: %s";

  /**
   * Sends a anonymous enquiry accepted event to the live service,
   *
   * @param userId the id of the user who should receive the event
   */
  public void sendAcceptAnonymousEnquiryEventToUser(String userId) {
    if (isNotBlank(userId)) {
      var liveEventMessage = new LiveEventMessage().eventType(ANONYMOUSENQUIRYACCEPTED);
      sendLiveEventMessage(singletonList(userId), liveEventMessage);
    }
  }

  private void sendLiveEventMessage(List<String> userIds, LiveEventMessage liveEventMessage) {
    sendLiveEventMessage(userIds, liveEventMessage,
        () -> String.format("Unable to trigger live event to users %s with message %s",
            userIds, liveEventMessage));
  }

  private void sendLiveEventMessage(List<String> userIds, LiveEventMessage liveEventMessage,
      Supplier<String> errorMessageSupplier) {
    try {
      this.liveControllerApi.sendLiveEvent(userIds, liveEventMessage);
    } catch (RestClientException e) {
      LogService.logInternalServerError(errorMessageSupplier.get(), e);
    }
  }

  /**
   * Collects all relevant user or consultant ids of chats and sessions and sends a new
   * direct message to the live service.
   *
   * @param rcGroupId the rocket chat group id used to observe relevant users
   */
  public void sendLiveDirectMessageEventToUsers(String rcGroupId) {
    if (isNotBlank(rcGroupId)) {
      List<String> userIds = this.userIdsProviderFactory.byRocketChatGroup(rcGroupId)
          .collectUserIds(rcGroupId).stream()
          .filter(this::notInitiatingUser)
          .collect(Collectors.toList());

      triggerDirectMessageLiveEvent(userIds, rcGroupId);
      triggerMobilePushNotification(userIds);
    }
  }

  private boolean notInitiatingUser(String userId) {
    return !userId.equals(this.authenticatedUser.getUserId());
  }

  private void triggerDirectMessageLiveEvent(List<String> userIds, String rcGroupId) {
    if (isNotEmpty(userIds)) {
      var liveEventMessage = new LiveEventMessage().eventType(DIRECTMESSAGE);

      sendLiveEventMessage(userIds, liveEventMessage, () -> {
        var rcMessage = String.format(RC_GROUP_ID_MESSAGE_TEMPLATE, rcGroupId);
        return makeUserIdsEventTypeMessage(liveEventMessage, userIds, rcMessage);
      });
    }
  }

  private String makeUserIdsEventTypeMessage(LiveEventMessage triggeredLiveEventMessage,
      List<String> toUsers, String withMessage) {
    return String.format("Unable to trigger %s live event to users %s with message %s",
        triggeredLiveEventMessage.getEventType(), toUsers, withMessage);
  }

  private void triggerMobilePushNotification(List<String> userIds) {
    userIds.forEach(this::sendPushNotificationForUser);
  }

  private void sendPushNotificationForUser(String userId) {
    this.userService.getUser(userId)
        .ifPresent(this::sendPushNotificationIfUserHasMobileToken);
  }

  private void sendPushNotificationIfUserHasMobileToken(User user) {
    if (isNotBlank(user.getMobileToken())) {
      this.pushMessageService.pushNewMessageEvent(user.getMobileToken());
    }
  }

  /**
   * Sends a new anonymous enquiry live event to the provided user IDs.
   *
   * @param userIds list of consultant user IDs
   * @param sessionId anonymous enquiry ID
   */
  public void sendLiveNewAnonymousEnquiryEventToUsers(List<String> userIds, Long sessionId) {
    if (isNotEmpty(userIds)) {
      var liveEventMessage = new LiveEventMessage()
          .eventType(NEWANONYMOUSENQUIRY);

      sendLiveEventMessage(userIds, liveEventMessage, () -> {
        var anonymousEnquiryMessage =
            String.format(NEW_ANONYMOUS_ENQUIRY_MESSAGE_TEMPLATE, sessionId);
        return makeUserIdsEventTypeMessage(liveEventMessage, userIds, anonymousEnquiryMessage);
      });
    }
  }
}
