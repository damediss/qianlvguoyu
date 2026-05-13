package com.guoyu.platform.system.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.RoleMenuRelationMapper;
import com.guoyu.platform.system.vo.MenuTreeVO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MenuApplicationServiceTest {

  private final MenuApplicationService service = new MenuApplicationService(
      Mockito.mock(MenuMapper.class),
      Mockito.mock(RoleMenuRelationMapper.class)
  );

  @Test
  void buildTreeKeepsPagesUnderDirectory() {
    MenuEntity root = menu(1L, 0L, "system", "directory", 1);
    MenuEntity child = menu(2L, 1L, "system_user", "page", 1);
    MenuEntity sibling = menu(3L, 0L, "course", "directory", 2);

    List<MenuTreeVO> tree = service.buildTree(List.of(child, sibling, root));

    assertThat(tree).hasSize(2);
    assertThat(tree.getFirst().getCode()).isEqualTo("system");
    assertThat(tree.getFirst().getChildren()).hasSize(1);
    assertThat(tree.getFirst().getChildren().getFirst().getCode()).isEqualTo("system_user");
  }

  private MenuEntity menu(Long id, Long parentId, String code, String menuType, int sortOrder) {
    MenuEntity entity = new MenuEntity();
    entity.setId(id);
    entity.setParentId(parentId);
    entity.setCode(code);
    entity.setName(code);
    entity.setMenuType(menuType);
    entity.setSortOrder(sortOrder);
    entity.setVisible(1);
    entity.setStatus("enabled");
    return entity;
  }
}
