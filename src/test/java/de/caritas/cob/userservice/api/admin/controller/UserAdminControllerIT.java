package de.caritas.cob.userservice.api.admin.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.caritas.cob.userservice.api.admin.report.service.ViolationReportGenerator;
import de.caritas.cob.userservice.api.admin.service.ConsultantAgencyAdminService;
import de.caritas.cob.userservice.api.admin.service.SessionAdminService;
import de.caritas.cob.userservice.api.authorization.RoleAuthorizationAuthorityMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.client.LinkDiscoverers;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@WebMvcTest(UserAdminController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureTestDatabase(replace = Replace.ANY)
public class UserAdminControllerIT {

  protected static final String CONSULTANT_ID = "1da238c6-cd46-4162-80f1-bff74eafeAAA";

  protected static final String ROOT_PATH = "/useradmin";
  protected static final String SESSION_PATH = ROOT_PATH + "/session";
  protected static final String CONSULTANT_AGENCY_PATH = ROOT_PATH + "/consultant/" + CONSULTANT_ID + "/agencies";
  protected static final String REPORT_PATH = ROOT_PATH + "/report";
  protected static final String PAGE_PARAM = "page";
  protected static final String PER_PAGE_PARAM = "perPage";

  @Autowired
  private MockMvc mvc;

  @MockBean
  private SessionAdminService sessionAdminService;

  @MockBean
  private ViolationReportGenerator violationReportGenerator;

  @MockBean
  private ConsultantAgencyAdminService consultantAgencyAdminService;

  @MockBean
  private LinkDiscoverers linkDiscoverers;

  @MockBean
  private RoleAuthorizationAuthorityMapper roleAuthorizationAuthorityMapper;

  @Test
  public void getSessions_Should_returnBadRequest_When_requiredPaginationParamsAreMissing()
      throws Exception {
    this.mvc.perform(get(SESSION_PATH))
        .andExpect(status().isBadRequest());
  }

  @Test
  public void getRoot_Should_returnExpectedRootDTO()
      throws Exception {
    this.mvc.perform(get(ROOT_PATH))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$._links").exists())
        .andExpect(jsonPath("$._links.self").exists())
        .andExpect(jsonPath("$._links.self.href", endsWith("/useradmin")))
        .andExpect(jsonPath("$._links.sessions").exists())
        .andExpect(
            jsonPath("$._links.sessions.href", endsWith("/useradmin/session?page=1&perPage=20")));
  }

  @Test
  public void getSessions_Should_returnOk_When_requiredPaginationParamsAreGiven()
      throws Exception {
    this.mvc.perform(get(SESSION_PATH)
        .param(PAGE_PARAM, "0")
        .param(PER_PAGE_PARAM, "1"))
        .andExpect(status().isOk());

    verify(this.sessionAdminService, times(1))
        .findSessions(eq(0), eq(1), any());
  }

  @Test
  public void getConsultantAgency_Should_returnOk_When_requiredConsultantIdParamIsGiven()
      throws Exception {
    this.mvc.perform(get(CONSULTANT_AGENCY_PATH))
        .andExpect(status().isOk());

    verify(this.consultantAgencyAdminService, times(1))
        .findConsultantAgencies(eq(CONSULTANT_ID));
  }

  @Test
  public void generateReport_Should_returnOk() throws Exception {
    this.mvc.perform(get(REPORT_PATH))
        .andExpect(status().isOk());

    verify(this.violationReportGenerator, times(1)).generateReport();
  }

}
