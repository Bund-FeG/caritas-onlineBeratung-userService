package de.caritas.cob.userservice.api.admin.service.session.pageprovider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.caritas.cob.userservice.api.model.SessionFilter;
import de.caritas.cob.userservice.api.repository.session.ConsultingType;
import de.caritas.cob.userservice.api.repository.session.Session;
import de.caritas.cob.userservice.api.repository.session.SessionRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@RunWith(MockitoJUnitRunner.class)
public class ConsultingTypeSessionPageProviderTest {

  @InjectMocks
  private ConsultingTypeSessionPageProvider consultingTypeSessionPageProvider;

  @Mock
  private SessionRepository sessionRepository;

  @Mock
  private SessionFilter sessionFilter;

  @Test
  public void supports_Should_returnTrue_When_consultantTypeFilterIsSet() {
    when(this.sessionFilter.getConsultingType()).thenReturn(1);

    boolean supports = this.consultingTypeSessionPageProvider.isSupported();

    assertThat(supports, is(true));
  }

  @Test
  public void supports_Should_returnFalse_When_consultantTypeFilterIsNotSet() {
    when(this.sessionFilter.getConsultingType()).thenReturn(null);

    boolean supports = this.consultingTypeSessionPageProvider.isSupported();

    assertThat(supports, is(false));
  }

  @Test
  public void executeQuery_Should_executeQueryOnRepository_When_pagebleIsGiven() {
    when(this.sessionFilter.getConsultingType()).thenReturn(1);
    PageRequest pageable = PageRequest.of(0, 1);

    this.consultingTypeSessionPageProvider.executeQuery(pageable);

    verify(this.sessionRepository, atLeastOnce()).findByConsultingType(ConsultingType.U25, pageable);
  }

  @Test
  public void executeQuery_Should_notExecuteQueryOnRepositoryAndreturnEmptyPage_When_consultingTypeDoesNotExist() {
    when(this.sessionFilter.getConsultingType()).thenReturn(99);
    PageRequest pageable = PageRequest.of(0, 1);

    Page<Session> sessions = this.consultingTypeSessionPageProvider.executeQuery(pageable);

    assertThat(sessions.getContent(), hasSize(0));
    verifyNoInteractions(this.sessionRepository);
  }

}
