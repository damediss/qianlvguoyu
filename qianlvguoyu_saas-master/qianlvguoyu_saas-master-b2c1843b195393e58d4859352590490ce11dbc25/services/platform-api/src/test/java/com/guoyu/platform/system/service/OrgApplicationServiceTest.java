package com.guoyu.platform.system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.system.entity.OrgEntity;
import com.guoyu.platform.system.mapper.OrgMapper;
import com.guoyu.platform.system.vo.OrgTreeVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class OrgApplicationServiceTest {

  private final OrgApplicationService service = new OrgApplicationService(Mockito.mock(OrgMapper.class));

  @Test
  void buildTreeKeepsChildrenUnderParent() {
    OrgEntity root = org(1L, 0L, "root", 1);
    OrgEntity child = org(2L, 1L, "child", 1);
    OrgEntity sibling = org(3L, 0L, "sibling", 2);

    List<OrgTreeVO> tree = service.buildTree(List.of(child, sibling, root));

    assertThat(tree).hasSize(2);
    assertThat(tree.getFirst().getCode()).isEqualTo("root");
    assertThat(tree.getFirst().getChildren()).hasSize(1);
    assertThat(tree.getFirst().getChildren().getFirst().getCode()).isEqualTo("child");
  }

  private OrgEntity org(Long id, Long parentId, String code, int sortOrder) {
    OrgEntity entity = new OrgEntity();
    entity.setId(id);
    entity.setParentId(parentId);
    entity.setCode(code);
    entity.setName(code);
    entity.setOrgType("department");
    entity.setSortOrder(sortOrder);
    entity.setStatus("enabled");
    return entity;
  }
}
