package de.caritas.cob.UserService.api.facade;

import static de.caritas.cob.UserService.testHelper.TestConstants.ACTIVE_CHAT;
import static de.caritas.cob.UserService.testHelper.TestConstants.CONSULTANT;
import static de.caritas.cob.UserService.testHelper.TestConstants.INACTIVE_CHAT;
import static de.caritas.cob.UserService.testHelper.TestConstants.RC_GROUP_ID;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import javax.ws.rs.InternalServerErrorException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import de.caritas.cob.UserService.api.exception.responses.ConflictException;
import de.caritas.cob.UserService.api.exception.responses.ForbiddenException;
import de.caritas.cob.UserService.api.helper.ChatHelper;
import de.caritas.cob.UserService.api.repository.chat.Chat;
import de.caritas.cob.UserService.api.service.ChatService;
import de.caritas.cob.UserService.api.service.RocketChatService;

@RunWith(MockitoJUnitRunner.class)
public class StartChatFacadeTest {

  @InjectMocks
  private StartChatFacade startChatFacade;
  @Mock
  private ChatHelper chatHelper;
  @Mock
  private RocketChatService rocketChatService;
  @Mock
  private ChatService chatService;
  @Mock
  private Chat chat;

  /**
   * Method: startChat
   */
  @Test
  public void startChat_Should_ThrowRequestForbiddenException_WhenConsultantHasNoPermissionForChat() {

    when(chatHelper.isChatAgenciesContainConsultantAgency(ACTIVE_CHAT, CONSULTANT))
        .thenReturn(false);

    try {
      startChatFacade.startChat(ACTIVE_CHAT, CONSULTANT);
      fail("Expected exception: RequestForbiddenException");
    } catch (ForbiddenException sequestForbiddenException) {
      assertTrue("Excepted RequestForbiddenException thrown", true);
    }

  }

  @Test
  public void startChat_Should_ThrowConflictException_WhenChatIsAlreadyStarted() {

    when(chatHelper.isChatAgenciesContainConsultantAgency(ACTIVE_CHAT, CONSULTANT))
        .thenReturn(true);

    try {
      startChatFacade.startChat(ACTIVE_CHAT, CONSULTANT);
      fail("Expected exception: ConflictException");
    } catch (ConflictException conflictException) {
      assertTrue("Excepted ConflictException thrown", true);
    }

  }

  @Test
  public void startChat_Should_ThrowInternalServerError_WhenChatHasNoGroupId() {

    when(chat.isActive()).thenReturn(false);
    when(chat.getGroupId()).thenReturn(null);

    when(chatHelper.isChatAgenciesContainConsultantAgency(chat, CONSULTANT)).thenReturn(true);

    try {
      startChatFacade.startChat(chat, CONSULTANT);
      fail("Expected exception: InternalServerErrorException");
    } catch (InternalServerErrorException internalServerErrorException) {
      assertTrue("Excepted InternalServerErrorException thrown", true);
    }

  }

  @Test
  public void startChat_Should_AddConsultantToRocketChatGroup() {

    when(chatHelper.isChatAgenciesContainConsultantAgency(INACTIVE_CHAT, CONSULTANT))
        .thenReturn(true);

    startChatFacade.startChat(INACTIVE_CHAT, CONSULTANT);

    verify(rocketChatService, times(1)).addUserToGroup(CONSULTANT.getRocketChatId(),
        INACTIVE_CHAT.getGroupId());

  }

  @Test
  public void startChat_Should_SetChatActiveAndSaveChat() {

    when(chat.getGroupId()).thenReturn(RC_GROUP_ID);

    when(chatHelper.isChatAgenciesContainConsultantAgency(chat, CONSULTANT)).thenReturn(true);

    startChatFacade.startChat(chat, CONSULTANT);

    verify(chat, times(1)).setActive(true);
    verify(chatService, times(1)).saveChat(chat);

  }

}
