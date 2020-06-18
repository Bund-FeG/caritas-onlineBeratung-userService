package de.caritas.cob.UserService.api.service;

import static de.caritas.cob.UserService.testHelper.TestConstants.ERROR;
import static de.caritas.cob.UserService.testHelper.TestConstants.USER;
import static de.caritas.cob.UserService.testHelper.TestConstants.USER_AGENCY;
import static de.caritas.cob.UserService.testHelper.TestConstants.USER_AGENCY_LIST;
import static org.hamcrest.CoreMatchers.everyItem;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.test.context.junit4.SpringRunner;
import de.caritas.cob.UserService.api.exception.ServiceException;
import de.caritas.cob.UserService.api.repository.userAgency.UserAgency;
import de.caritas.cob.UserService.api.repository.userAgency.UserAgencyRepository;

@RunWith(SpringRunner.class)
public class UserAgencyServiceTest {

  @InjectMocks
  private UserAgencyService userAgencyService;
  @Mock
  private LogService logService;
  @Mock
  private UserAgencyRepository userAgencyRepository;


  /**
   * Method: saveUserAgency
   * 
   */

  @Test
  public void saveUserAgency_Should_ThrowServiceException_When_DatabaseFails() {

    @SuppressWarnings("serial")
    DataAccessException ex = new DataAccessException(ERROR) {};
    when(userAgencyRepository.save(Mockito.any())).thenThrow(ex);

    try {
      userAgencyService.saveUserAgency(USER_AGENCY);
      fail("Expected exception: ServiceException");
    } catch (ServiceException serviceException) {
      assertTrue("Excepted ServiceException thrown", true);
    }
  }

  @Test
  public void saveUser_Should_UserObject() {

    when(userAgencyRepository.save(Mockito.any())).thenReturn(USER_AGENCY);

    UserAgency result = userAgencyService.saveUserAgency(USER_AGENCY);

    assertNotNull(result);
    assertEquals(USER_AGENCY, result);
  }

  /**
   * Method: getUserAgenciesByUser
   * 
   */

  @Test
  public void getUserAgenciesByUser_Should_ReturnServiceException_When_RepositoryFails()
      throws Exception {

    @SuppressWarnings("serial")
    DataAccessException ex = new DataAccessException(ERROR) {};
    when(userAgencyRepository.findByUser(Mockito.any())).thenThrow(ex);

    try {
      userAgencyService.getUserAgenciesByUser(USER);
      fail("Expected exception: ServiceException");
    } catch (ServiceException serviceEx) {
      assertTrue("Excepted ServiceException thrown", true);
    }
  }

  @Test
  public void getUserAgenciesByUser_Should_ReturnListOfUserAgencyObjects_When_RepositoryCallIsSuccessfull() {

    when(userAgencyRepository.findByUser(Mockito.any())).thenReturn(USER_AGENCY_LIST);

    List<UserAgency> result = userAgencyService.getUserAgenciesByUser(USER);

    assertThat(result, everyItem(instanceOf(UserAgency.class)));
  }

  /**
   * Method: deleteUser
   * 
   */

  @Test
  public void deleteUserAgency_Should_ThrowServiceException_When_DatabaseFails() {

    @SuppressWarnings("serial")
    DataAccessException ex = new DataAccessException(ERROR) {};
    doThrow(ex).when(userAgencyRepository).delete(USER_AGENCY);

    try {
      userAgencyService.deleteUserAgency(USER_AGENCY);
      fail("Expected exception: ServiceException");
    } catch (ServiceException serviceEx) {
      assertTrue("Excepted ServiceException thrown", true);
    }
  }

  @Test
  public void deleteUserAgency_Should_ThrowIllegalArgumentException_When_UserAgencyIsNull() {

    IllegalArgumentException ex = new IllegalArgumentException();
    doThrow(ex).when(userAgencyRepository).delete(null);

    try {
      userAgencyService.deleteUserAgency(null);
      fail("Expected exception: ServiceException");
    } catch (ServiceException serviceEx) {
      assertTrue("Excepted ServiceException thrown", true);
    }
  }

  @Test
  public void deleteUserAgency_Should_CallDeleteUserAgencyRepository() {

    userAgencyService.deleteUserAgency(USER_AGENCY);

    verify(userAgencyRepository, times(1)).delete(USER_AGENCY);
  }

}
