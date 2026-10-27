package com.guoyu.platform.competition.player.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.guoyu.platform.competition.entity.CompetitionPlayerIdentityEntity;
import com.guoyu.platform.competition.mapper.CompetitionPlayerIdentityMapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

class CompetitionPlayerIdentityDomainServiceTest {

  private static final String ID_CARD = "11010519491231002X";
  private static final String IMPORTED_MOBILE = "13900139000";

  @Test
  void changedMobileIsReportedAccuratelyWithoutMarkingOuterTransactionRollbackOnly() {
    CompetitionPlayerIdentityMapper playerMapper = mock(CompetitionPlayerIdentityMapper.class);
    CompetitionPlayerDataHash dataHash = mock(CompetitionPlayerDataHash.class);
    when(dataHash.lookupHash(ID_CARD)).thenReturn("id-card-hash");
    when(dataHash.lookupHash(IMPORTED_MOBILE)).thenReturn("imported-mobile-hash");

    CompetitionPlayerIdentityEntity existing = new CompetitionPlayerIdentityEntity();
    existing.setId(9001L);
    existing.setIdCardLookupHash("id-card-hash");
    existing.setMobileLookupHash("registered-mobile-hash");
    when(playerMapper.selectForUpdateByIdentityHashes("id-card-hash", "imported-mobile-hash"))
        .thenReturn(List.of(existing));

    DriverManagerDataSource dataSource = new DriverManagerDataSource(
        "jdbc:h2:mem:competition-player-identity;DB_CLOSE_DELAY=-1");
    DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
    CompetitionPlayerIdentityDomainService target = new CompetitionPlayerIdentityDomainService(
        playerMapper, dataHash, mock(PasswordEncoder.class));
    TransactionInterceptor interceptor = new TransactionInterceptor(
        transactionManager, new AnnotationTransactionAttributeSource());
    ProxyFactory proxyFactory = new ProxyFactory(target);
    proxyFactory.addAdvice(interceptor);
    CompetitionPlayerIdentityDomainService service =
        (CompetitionPlayerIdentityDomainService) proxyFactory.getProxy();

    AtomicReference<CompetitionPlayerIdentityConflictException> captured = new AtomicReference<>();
    TransactionTemplate outerTransaction = new TransactionTemplate(transactionManager);

    assertThatCode(() -> outerTransaction.executeWithoutResult(status -> {
      try {
        service.resolveOrCreate(1L, ID_CARD, IMPORTED_MOBILE);
      } catch (CompetitionPlayerIdentityConflictException ex) {
        captured.set(ex);
      }
    })).doesNotThrowAnyException();
    assertThat(captured.get())
        .isNotNull()
        .hasMessage("该身份证对应的登记手机号与导入手机号不一致");
  }
}
