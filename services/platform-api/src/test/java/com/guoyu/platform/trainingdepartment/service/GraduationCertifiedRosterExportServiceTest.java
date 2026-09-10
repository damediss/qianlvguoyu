package com.guoyu.platform.trainingdepartment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.guoyu.platform.trainingdepartment.entity.TrainCertificateEntity;
import com.guoyu.platform.trainingdepartment.entity.TrainPeopleListEntity;
import com.guoyu.platform.trainingdepartment.service.GraduationCertifiedRosterExportService.RosterScope;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GraduationCertifiedRosterExportServiceTest {

  @Test
  void formatLevelLabel_usesRensheSeriesTitle() {
    assertEquals("五级/初级工", GraduationCertifiedRosterExportService.formatLevelLabel(1));
    assertEquals("四级/中级工", GraduationCertifiedRosterExportService.formatLevelLabel(2));
    assertEquals("三级/高级工", GraduationCertifiedRosterExportService.formatLevelLabel(3));
    assertEquals("二级/技师", GraduationCertifiedRosterExportService.formatLevelLabel(4));
    assertEquals("一级/高级技师", GraduationCertifiedRosterExportService.formatLevelLabel(5));
    assertEquals("", GraduationCertifiedRosterExportService.formatLevelLabel(null));
    assertEquals("", GraduationCertifiedRosterExportService.formatLevelLabel(0));
    assertEquals("", GraduationCertifiedRosterExportService.formatLevelLabel(9));
  }

  @Test
  void filterByScope_certifiedUsesSubsidyCertifiedRule() {
    TrainPeopleListEntity a = people(1L);
    TrainPeopleListEntity b = people(2L);
    TrainPeopleListEntity c = people(3L);
    TrainPeopleListEntity d = people(4L);
    Map<Long, TrainCertificateEntity> certs = new HashMap<>();
    certs.put(1L, cert("S001"));
    certs.put(2L, cert("  "));
    certs.put(3L, cert(null));
    TrainCertificateEntity fake = cert("GC-123");
    certs.put(4L, fake);

    List<TrainPeopleListEntity> certified = GraduationCertifiedRosterExportService.filterByScope(
        List.of(a, b, c, d), certs, RosterScope.CERTIFIED);
    assertEquals(1, certified.size());
    assertEquals(1L, certified.get(0).getId());

    List<TrainPeopleListEntity> all = GraduationCertifiedRosterExportService.filterByScope(
        List.of(a, b, c, d), certs, RosterScope.ALL);
    assertEquals(4, all.size());
  }

  @Test
  void rosterScope_defaultsToCertified() {
    assertEquals(RosterScope.CERTIFIED, RosterScope.fromParam(null));
    assertEquals(RosterScope.CERTIFIED, RosterScope.fromParam(""));
    assertEquals(RosterScope.CERTIFIED, RosterScope.fromParam("certified"));
    assertEquals(RosterScope.ALL, RosterScope.fromParam("all"));
  }

  private static TrainPeopleListEntity people(Long id) {
    TrainPeopleListEntity p = new TrainPeopleListEntity();
    p.setId(id);
    return p;
  }

  private static TrainCertificateEntity cert(String no) {
    TrainCertificateEntity c = new TrainCertificateEntity();
    c.setCertificateNo(no);
    return c;
  }
}
