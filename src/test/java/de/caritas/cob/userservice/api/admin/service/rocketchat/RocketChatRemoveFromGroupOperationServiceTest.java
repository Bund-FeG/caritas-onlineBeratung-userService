package de.caritas.cob.userservice.api.admin.service.rocketchat;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.facade.RocketChatFacade;
import de.caritas.cob.userservice.api.model.rocketchat.group.GroupMemberDTO;
import de.caritas.cob.userservice.api.repository.consultant.Consultant;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.repository.session.SessionStatus;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RocketChatRemoveFromGroupOperationServiceTest {

  private RocketChatRemoveFromGroupOperationService removeService;

  @Mock
  private RocketChatFacade rocketChatFacade;

  @Mock
  private KeycloakAdminClientService keycloakAdminClientService;

  @Mock
  private Session session;

  @Mock
  private Consultant consultant;

  @Before
  public void setup() {
    Map<Session, List<Consultant>> sessionConsultants = new HashMap<>();
    sessionConsultants.put(session, singletonList(consultant));
    this.removeService =
        RocketChatRemoveFromGroupOperationService
            .getInstance(this.rocketChatFacade, this.keycloakAdminClientService)
            .onSessionConsultants(sessionConsultants);
  }

  @Test
  public void removeFromGroupsOrRollbackOnFailure_Should_notCollectMembers_When_rcUserIdIsEmpty() {
    this.removeService.removeFromGroupsOrRollbackOnFailure();

    verify(this.rocketChatFacade, times(0)).getStandardMembersOfGroup(any());
  }

  @Test
  public void removeFromGroupsOrRollbackOnFailure_Should_executeRemove_When_rcUserIdIsGiven() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getFeedbackGroupId()).thenReturn("feedback");
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(any()))
        .thenReturn(singletonList(groupMemberDTO));

    this.removeService.removeFromGroupsOrRollbackOnFailure();

    verify(this.rocketChatFacade, times(1)).removeUserFromGroup("rcId", "group");
    verify(this.rocketChatFacade, times(1)).removeUserFromGroup("rcId", "feedback");
  }

  @Test
  public void removeFromGroupsOrRollbackOnFailure_Should_throwInternalServerError_When_rollbackFailes() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getStatus()).thenReturn(SessionStatus.NEW);
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(anyString()))
        .thenReturn(singletonList(groupMemberDTO));
    doThrow(new RuntimeException("")).when(this.rocketChatFacade)
        .removeUserFromGroup(anyString(), anyString());
    doThrow(new RuntimeException("")).when(this.rocketChatFacade)
        .addUserToRocketChatGroup(anyString(), anyString());

    try {
      this.removeService.removeFromGroupsOrRollbackOnFailure();
      fail("No Exception thrown");
    } catch (InternalServerErrorException e) {
      assertThat(e.getMessage(), containsString("ERROR: Failed to rollback"));
    }
  }

  @Test
  public void removeFromGroupsOrRollbackOnFailure_Should_throwInternalServerErrorAndPerformRollback_When_error() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getStatus()).thenReturn(SessionStatus.NEW);
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(any()))
        .thenReturn(singletonList(groupMemberDTO));
    doThrow(new RuntimeException("")).when(this.rocketChatFacade).removeUserFromGroup(any(), any());

    try {
      this.removeService.removeFromGroupsOrRollbackOnFailure();
      fail("No Exception thrown");
    } catch (InternalServerErrorException e) {
      verify(this.rocketChatFacade, times(1)).addUserToRocketChatGroup("rcId", "group");
    }
  }

  @Test
  public void removeFromGroupsOrRollbackOnFailure_Should_notExecuteRemove_When_consultantHasSpecialAuthorities() {
    when(this.session.getGroupId()).thenReturn("group");
    when(this.session.getFeedbackGroupId()).thenReturn("feedback");
    when(this.consultant.getRocketChatId()).thenReturn("rcId");
    GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
    groupMemberDTO.set_id(this.consultant.getRocketChatId());
    when(this.rocketChatFacade.retrieveRocketChatMembers(any()))
        .thenReturn(singletonList(groupMemberDTO));
    when(this.keycloakAdminClientService.userHasAuthority(any(), any())).thenReturn(true);

    this.removeService.removeFromGroupsOrRollbackOnFailure();

    verify(this.rocketChatFacade, times(0)).removeUserFromGroup("rcId", "group");
    verify(this.rocketChatFacade, times(0)).removeUserFromGroup("rcId", "feedback");
  }

}
