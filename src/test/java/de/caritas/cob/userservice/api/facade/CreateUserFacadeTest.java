package de.caritas.cob.userservice.api.facade;

import static de.caritas.cob.userservice.api.exception.httpresponses.customheader.HttpStatusExceptionReason.USERNAME_NOT_AVAILABLE;
import static de.caritas.cob.userservice.testHelper.KeycloakConstants.KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITHOUT_USER_ID;
import static de.caritas.cob.userservice.testHelper.KeycloakConstants.KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTING_TYPE_SETTINGS_KREUZBUND;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTING_TYPE_SETTINGS_WITH_FORMAL_LANGUAGE;
import static de.caritas.cob.userservice.testHelper.TestConstants.CONSULTING_TYPE_SUCHT;
import static de.caritas.cob.userservice.testHelper.TestConstants.ERROR;
import static de.caritas.cob.userservice.testHelper.TestConstants.USER;
import static de.caritas.cob.userservice.testHelper.TestConstants.USER_DTO_KREUZBUND;
import static de.caritas.cob.userservice.testHelper.TestConstants.USER_DTO_SUCHT;
import static de.caritas.cob.userservice.testHelper.TestConstants.USER_DTO_SUCHT_WITHOUT_EMAIL;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.authorization.UserRole;
import de.caritas.cob.userservice.api.exception.httpresponses.BadRequestException;
import de.caritas.cob.userservice.api.exception.httpresponses.CustomValidationHttpStatusException;
import de.caritas.cob.userservice.api.exception.httpresponses.InternalServerErrorException;
import de.caritas.cob.userservice.api.exception.keycloak.KeycloakException;
import de.caritas.cob.userservice.api.facade.rollback.RollbackFacade;
import de.caritas.cob.userservice.api.helper.AgencyVerifier;
import de.caritas.cob.userservice.api.helper.UserVerifier;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeManager;
import de.caritas.cob.userservice.api.manager.consultingtype.ConsultingTypeSettings;
import de.caritas.cob.userservice.api.model.registration.UserDTO;
import de.caritas.cob.userservice.api.service.helper.KeycloakAdminClientService;
import de.caritas.cob.userservice.api.service.user.UserService;
import org.hamcrest.Matchers;
import org.jeasy.random.EasyRandom;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;

@RunWith(MockitoJUnitRunner.class)
public class CreateUserFacadeTest {

  @InjectMocks
  private CreateUserFacade createUserFacade;
  @Mock
  private KeycloakAdminClientService keycloakAdminClientService;
  @Mock
  private UserService userService;
  @Mock
  private RollbackFacade rollbackFacade;
  @Mock
  private ConsultingTypeManager consultingTypeManager;
  @Mock
  private AgencyVerifier agencyVerifier;
  @Mock
  private CreateNewConsultingTypeFacade createNewConsultingTypeFacade;
  @Mock
  private UserVerifier userVerifier;

  @Test
  public void createUserAndInitializeAccount_Should_throwExpectedStatusException_When_UsernameIsAlreadyExisting() {
    doThrow(new CustomValidationHttpStatusException(USERNAME_NOT_AVAILABLE))
        .when(userVerifier).checkIfUsernameIsAvailable(any());

    try {
      this.createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT);
    } catch (CustomValidationHttpStatusException e) {
      assertThat(e.getCustomHttpHeader(), notNullValue());
      assertThat(e.getCustomHttpHeader().get("X-Reason").get(0),
          Matchers.is(USERNAME_NOT_AVAILABLE.name()));
    }
  }

  @Test(expected = BadRequestException.class)
  public void createUserAndInitializeAccount_Should_ThrowBadRequest_When_ProvidedConsultingTypeDoesNotMatchAgency() {
    doNothing().when(userVerifier).checkIfUsernameIsAvailable(any());
    doThrow(new BadRequestException(ERROR))
        .when(agencyVerifier).checkIfConsultingTypeMatchesToAgency(any());

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT);
  }

  @Test
  public void createUserAndInitializeAccount_Should_throwConflictException_When_usernameIsNotAvailable() {
    doThrow(new CustomValidationHttpStatusException(USERNAME_NOT_AVAILABLE, HttpStatus.CONFLICT))
        .when(userVerifier).checkIfUsernameIsAvailable(any());

    try {
      this.createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT);
    } catch (CustomValidationHttpStatusException e) {
      assertThat(e.getCustomHttpHeader(), notNullValue());
      assertThat(e.getCustomHttpHeader().get("X-Reason").get(0),
          Matchers.is(USERNAME_NOT_AVAILABLE.name()));
      assertThat(e.getHttpStatus(), is(HttpStatus.CONFLICT));
    }
  }

  @Test(expected = InternalServerErrorException.class)
  public void createUserAndInitializeAccount_Should_ThrowInternalServerErrorException_When_CreateKeycloakUserReturnsNoUserId() {

    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITHOUT_USER_ID);

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT);
  }

  @Test(expected = InternalServerErrorException.class)
  public void createUserAndInitializeAccount_Should_ThrowInternalServerErrorExceptionAndRollbackUserAccount_When_KeycloakHelperUpdateUserRoleReturnsException() {

    doNothing().when(userVerifier).checkIfUsernameIsAvailable(any());
    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID);
    doThrow(new KeycloakException(ERROR))
        .when(keycloakAdminClientService).updateRole(anyString(), any(UserRole.class));

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT);

    verify(rollbackFacade, times(1)).rollBackUserAccount(any());
  }

  @Test(expected = InternalServerErrorException.class)
  public void createUserAndInitializeAccount_Should_ThrowInternalServerErrorExceptionAndRollbackUserAccount_When_UpdateKeycloakPasswordFails() {

    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID);
    doThrow(new KeycloakException(ERROR)).when(keycloakAdminClientService)
        .updatePassword(anyString(), anyString());

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT);

    verify(rollbackFacade, times(1)).rollBackUserAccount(any());
  }

  @Test(expected = InternalServerErrorException.class)
  public void createUserAndInitializeAccount_Should_ThrowInternalServerErrorExceptionAndRollbackUserAccount_When_UpdateKeycloakDummyEmailFails() {
    EasyRandom easyRandom = new EasyRandom();
    UserDTO userDTO = easyRandom.nextObject(UserDTO.class);
    userDTO.setConsultingType(String.valueOf(CONSULTING_TYPE_SUCHT.getValue()));
    userDTO.setEmail(null);

    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID);
    doNothing().when(keycloakAdminClientService).updatePassword(anyString(),
        anyString());
    doThrow(new KeycloakException(ERROR)).when(keycloakAdminClientService)
        .updateDummyEmail(anyString(), any());

    createUserFacade.createUserAccountWithInitializedConsultingType(userDTO);

    verify(rollbackFacade, times(1)).rollBackUserAccount(any());
  }

  @Test
  public void createUserAndInitializeAccount_Should_UpdateDummyEmail_When_NoEmailProvided() {

    when(consultingTypeManager.getConsultingTypeSettings(any()))
        .thenReturn(CONSULTING_TYPE_SETTINGS_WITH_FORMAL_LANGUAGE);
    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID);
    doNothing().when(keycloakAdminClientService).updatePassword(anyString(),
        anyString());

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT_WITHOUT_EMAIL);

    verify(keycloakAdminClientService, times(1)).updateDummyEmail(anyString(),
        any());
    verify(rollbackFacade, times(0)).rollBackUserAccount(any());
  }

  @Test(expected = InternalServerErrorException.class)
  public void createUserAndInitializeAccount_Should_ThrowInternalServerErrorExceptionAndRollbackUserAccount_When_CreateAccountInMariaDBFails() {

    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID);
    when(consultingTypeManager.getConsultingTypeSettings(any()))
        .thenReturn(CONSULTING_TYPE_SETTINGS_WITH_FORMAL_LANGUAGE);
    when(userService.createUser(any(), any(), any(), anyBoolean()))
        .thenThrow(new InternalServerErrorException(ERROR));

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_SUCHT_WITHOUT_EMAIL);

    verify(rollbackFacade, times(1)).rollBackUserAccount(any());
  }

  @Test
  public void createUserAndInitializeAccount_Should_LogOutFromRocketChat_When_ConsultingTypeIsKreuzbundAndRocketChatLoginSucceeded() {

    when(consultingTypeManager.getConsultingTypeSettings(any()))
        .thenReturn(CONSULTING_TYPE_SETTINGS_KREUZBUND);
    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID);
    doNothing().when(keycloakAdminClientService).updatePassword(anyString(),
        anyString());
    when(userService.createUser(any(), any(), any(), anyBoolean())).thenReturn(USER);

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_KREUZBUND);

    verify(rollbackFacade, times(0)).rollBackUserAccount(any());
  }

  @Test
  public void createUserAndInitializeAccount_Should_CallInitializeNewConsultingType_When_EverythingSucceeded() {

    when(consultingTypeManager.getConsultingTypeSettings(any()))
        .thenReturn(CONSULTING_TYPE_SETTINGS_KREUZBUND);
    when(keycloakAdminClientService.createKeycloakUser(any()))
        .thenReturn(KEYCLOAK_CREATE_USER_RESPONSE_DTO_WITH_USER_ID);
    doNothing().when(keycloakAdminClientService).updatePassword(anyString(),
        anyString());
    when(userService.createUser(any(), any(), any(), anyBoolean())).thenReturn(USER);

    createUserFacade.createUserAccountWithInitializedConsultingType(USER_DTO_KREUZBUND);

    verify(createNewConsultingTypeFacade, times(1))
        .initializeNewConsultingType(any(), any(), any(ConsultingTypeSettings.class));
    verify(rollbackFacade, times(0)).rollBackUserAccount(any());
  }
}
