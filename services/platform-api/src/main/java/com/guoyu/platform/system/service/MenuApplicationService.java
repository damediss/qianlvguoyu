package com.guoyu.platform.system.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.security.SecurityContext;
import com.guoyu.platform.system.dto.MenuBatchVisibleReq;
import com.guoyu.platform.system.dto.MenuCreateReq;
import com.guoyu.platform.system.dto.MenuUpdateReq;
import com.guoyu.platform.system.entity.MenuEntity;
import com.guoyu.platform.system.entity.RoleEntity;
import com.guoyu.platform.system.entity.RoleMenuEntity;
import com.guoyu.platform.system.mapper.MenuMapper;
import com.guoyu.platform.system.mapper.RoleMapper;
import com.guoyu.platform.system.mapper.RoleMenuRelationMapper;
import com.guoyu.platform.system.vo.MenuTreeVO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class MenuApplicationService {

  private final MenuMapper menuMapper;
  private final RoleMapper roleMapper;
  private final RoleMenuRelationMapper roleMenuRelationMapper;

  public MenuApplicationService(MenuMapper menuMapper, RoleMapper roleMapper,
      RoleMenuRelationMapper roleMenuRelationMapper) {
    this.menuMapper = menuMapper;
    this.roleMapper = roleMapper;
    this.roleMenuRelationMapper = roleMenuRelationMapper;
  }

  /** Expose tenantId for @Cacheable SPEL key: #root.target.tenantId() */
  public Long tenantId() {
    return SecurityContext.currentUser().getTenantId();
  }

  @Cacheable(value = "menuTree", key = "#root.target.tenantId() + ':#ROOT'", unless = "#result == null")
  @Transactional(readOnly = true)
  public List<MenuTreeVO> tree() {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<MenuEntity> menus = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0)
        .orderByAsc(MenuEntity::getSortOrder)
        .orderByAsc(MenuEntity::getId));
    List<MenuTreeVO> tree = buildTree(menus);
    // 2026-06-18 调试：dump menu VO name 字段的中文是否正常
    // 用于诊断 Redis 缓存 read 报"Unexpected token (START_OBJECT)"的根因
    // 如果 name 在 Java 内存里就已经是乱码，问题在 DB -> Java 这一段
    // 如果 name 在 Java 内存里正常但 Redis 里是乱码，问题在 Java -> Redis 这一段
    if (log.isDebugEnabled() && tree != null) {
      log.debug("[MenuTree.VO-CHECK] totalNodes={} sampleNames={}",
          tree.size(),
          sampleNames(tree, 5));
    }
    return tree;
  }

  private static String sampleNames(List<MenuTreeVO> tree, int n) {
    StringBuilder sb = new StringBuilder("[");
    int count = 0;
    for (MenuTreeVO root : tree) {
      if (count++ >= n) break;
      sb.append("{id=").append(root.getId()).append(",name=").append(repr(root.getName())).append("}, ");
      if (root.getChildren() != null) {
        for (MenuTreeVO c : root.getChildren()) {
          if (count++ >= n * 3) break;
          sb.append("{id=").append(c.getId()).append(",name=").append(repr(c.getName())).append("}, ");
        }
      }
    }
    if (sb.length() > 1) sb.setLength(sb.length() - 2);
    return sb.append("]").toString();
  }

  /** Java 字符串字面量表示（控制字符转义），方便在日志里精确看到字符。 */
  private static String repr(String s) {
    if (s == null) return "null";
    StringBuilder sb = new StringBuilder(s.length() + 2);
    sb.append('"');
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      if (c == '"') sb.append("\\\"");
      else if (c == '\\') sb.append("\\\\");
      else if (c == '\n') sb.append("\\n");
      else if (c == '\r') sb.append("\\r");
      else if (c == '\t') sb.append("\\t");
      else if (c < 0x20) sb.append(String.format("\\x%02x", (int) c));
      else sb.append(c);
    }
    sb.append('"');
    return sb.toString();
  }

  @CacheEvict(value = "menuTree", allEntries = true)
  @Transactional
  public MenuTreeVO create(MenuCreateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    validateParent(tenantId, req.parentId(), null);
    ensureCodeAvailable(tenantId, req.code());

    MenuEntity entity = new MenuEntity();
    entity.setTenantId(tenantId);
    entity.setParentId(req.parentId());
    entity.setMenuType(req.menuType());
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setPath(req.path());
    entity.setComponent(req.component());
    entity.setPermissionCode(req.permissionCode());
    entity.setIcon(req.icon());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setVisible(req.visible() == null ? 1 : req.visible());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    menuMapper.insert(entity);

    // Auto-bind new menu to PLATFORM_ADMIN role so it appears in the sidebar
    bindToAdminRole(tenantId, entity.getId());

    return toTreeVO(entity);
  }

  private void bindToAdminRole(Long tenantId, Long menuId) {
    Long adminRoleId = findAdminRoleId(tenantId);
    if (adminRoleId == null) {
      log.warn("PLATFORM_ADMIN role not found in tenant {}, skipping auto-bind for menu {}", tenantId, menuId);
      return;
    }
    boolean alreadyBound = roleMenuRelationMapper.exists(new LambdaQueryWrapper<RoleMenuEntity>()
        .eq(RoleMenuEntity::getTenantId, tenantId)
        .eq(RoleMenuEntity::getRoleId, adminRoleId)
        .eq(RoleMenuEntity::getMenuId, menuId)
        .eq(RoleMenuEntity::getDeleted, 0));
    if (alreadyBound) return;

    RoleMenuEntity binding = new RoleMenuEntity();
    binding.setTenantId(tenantId);
    binding.setRoleId(adminRoleId);
    binding.setMenuId(menuId);
    binding.setDeleted(0);
    roleMenuRelationMapper.insert(binding);
  }

  private Long findAdminRoleId(Long tenantId) {
    return roleMapper.selectList(new LambdaQueryWrapper<RoleEntity>()
            .eq(RoleEntity::getTenantId, tenantId)
            .eq(RoleEntity::getCode, "PLATFORM_ADMIN")
            .eq(RoleEntity::getDeleted, 0))
        .stream()
        .findFirst()
        .map(RoleEntity::getId)
        .orElse(null);
  }

  @CacheEvict(value = "menuTree", allEntries = true)
  @Transactional
  public MenuTreeVO update(Long id, MenuUpdateReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    MenuEntity entity = getExisting(tenantId, id);
    validateParent(tenantId, req.parentId(), id);

    entity.setParentId(req.parentId());
    entity.setMenuType(req.menuType());
    entity.setName(req.name());
    entity.setPath(req.path());
    entity.setComponent(req.component());
    entity.setPermissionCode(req.permissionCode());
    entity.setIcon(req.icon());
    entity.setSortOrder(req.sortOrder() == null ? 0 : req.sortOrder());
    entity.setVisible(req.visible() == null ? 1 : req.visible());
    entity.setStatus(req.status());
    menuMapper.updateById(entity);
    return toTreeVO(entity);
  }

  @CacheEvict(value = "menuTree", allEntries = true)
  @Transactional
  public void batchUpdateVisible(MenuBatchVisibleReq req) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    List<MenuEntity> entities = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
        .in(MenuEntity::getId, req.ids())
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getDeleted, 0));
    if (entities.size() != req.ids().size()) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Some menus not found or do not belong to current tenant");
    }
    for (MenuEntity entity : entities) {
      entity.setVisible(req.visible());
      menuMapper.updateById(entity);
    }
    log.info("Batch updated visible to {} for {} menus in tenant {}", req.visible(), entities.size(), tenantId);
  }

  @CacheEvict(value = "menuTree", allEntries = true)
  @Transactional
  public void delete(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    MenuEntity entity = getExisting(tenantId, id);
    int childCount = countChildren(tenantId, id);
    if (childCount > 0) {
      throw new BusinessException(
          ErrorCode.CONFLICT,
          "Menu has child nodes (" + childCount + "). Pass ?cascade=true to delete the whole subtree.");
    }
    int removedBindings = roleMenuRelationMapper.softDeleteByMenus(tenantId, List.of(id));
    if (removedBindings > 0) {
      log.info("Removed {} role-menu bindings before deleting menu id={}", removedBindings, id);
    }
    // 用 deleteById 而不是 updateById(setDeleted=1)：
    // BaseEntity.deleted 标了 @TableLogic，updateById 会给 UPDATE 加 WHERE deleted=0
    // 导致永远 0 行受影响。deleteById 是 MP 专为逻辑删除设计的方法。
    int affected = menuMapper.deleteById(id);
    if (affected == 0) {
      log.warn("deleteById affected 0 rows for menu id={} tenant={} (already deleted?)", id, tenantId);
    }
  }

  /**
   * 级联软删：递归删除整支子树（深度优先，从叶子往上），并清空 sys_role_menu 中
   * 与这些菜单的关联。删除完成后不会回滚"角色失去菜单访问权限"——那是用户预期的副作用。
   *
   * 实现要点：
   * 1. 用 BFS 收集所有后代 id（避免深递归爆栈）
   * 2. 先删 role_menu 关联（外层），再软删菜单本身（deleted=1）
   * 3. 不主动校验"被角色绑定"——用户既然选了 cascade，已经默许级联
   */
  @CacheEvict(value = "menuTree", allEntries = true)
  @Transactional
  public int deleteWithChildren(Long id) {
    Long tenantId = SecurityContext.currentUser().getTenantId();
    MenuEntity entity = getExisting(tenantId, id);

    // BFS 收集所有后代 id（不含自己）
    List<Long> allIds = new ArrayList<>();
    java.util.Deque<Long> queue = new java.util.ArrayDeque<>();
    queue.add(id);
    while (!queue.isEmpty()) {
      Long current = queue.poll();
      List<Long> children = menuMapper.selectList(new LambdaQueryWrapper<MenuEntity>()
              .eq(MenuEntity::getTenantId, tenantId)
              .eq(MenuEntity::getParentId, current)
              .eq(MenuEntity::getDeleted, 0)
              .select(MenuEntity::getId))
          .stream()
          .map(MenuEntity::getId)
          .toList();
      for (Long childId : children) {
        allIds.add(childId);
        queue.add(childId);
      }
    }

    // 整支 id 列表 = 父 + 所有后代
    List<Long> wholeTreeIds = new ArrayList<>(allIds.size() + 1);
    wholeTreeIds.add(id);
    wholeTreeIds.addAll(allIds);

    // 1) 清空角色-菜单关联
    int removedBindings = roleMenuRelationMapper.softDeleteByMenus(tenantId, wholeTreeIds);
    // 2) 软删整支菜单
    // 注意：不能用 updateById(entity.setDeleted(1))，因为 BaseEntity.deleted 标了 @TableLogic，
    // MP 会自动给 UPDATE 加 WHERE deleted=0 条件，导致永远 0 行受影响。
    // deleteById 是 MP 专门为逻辑删除设计的方法，会正确生成
    //   UPDATE sys_menu SET deleted=1 [, updated_at, updated_by] WHERE id=? AND deleted=0
    int deletedCount = 0;
    for (Long menuId : wholeTreeIds) {
      deletedCount += menuMapper.deleteById(menuId);
    }

    log.info("Cascade-deleted menu tree root={} totalIds={} removedBindings={} operator={}",
        id, wholeTreeIds.size(), removedBindings, SecurityContext.currentUser().getUserId());
    return wholeTreeIds.size();
  }

  private int countChildren(Long tenantId, Long parentId) {
    return Math.toIntExact(menuMapper.selectCount(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getParentId, parentId)
        .eq(MenuEntity::getDeleted, 0)));
  }

  List<MenuTreeVO> buildTree(List<MenuEntity> menus) {
    Map<Long, MenuTreeVO> nodeMap = new LinkedHashMap<>();
    for (MenuEntity menu : menus) {
      MenuTreeVO node = toTreeVO(menu);
      nodeMap.put(node.getId(), node);
    }
    for (MenuTreeVO node : nodeMap.values()) {
      MenuTreeVO parent = nodeMap.get(node.getParentId());
      if (parent != null) {
        parent.getChildren().add(node);
      }
    }
    Comparator<MenuTreeVO> comparator = Comparator
        .comparing(MenuTreeVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
        .thenComparing(MenuTreeVO::getId);
    nodeMap.values().forEach(node -> node.getChildren().sort(comparator));
    // 2026-06-18 BUG-003：必须返回 ArrayList，不能用 Stream.toList()。
    // Stream.toList() 返回 java.util.ImmutableCollections$ListN（继承自 final 类
    // AbstractImmutableList），Jackson DefaultTyping.NON_FINAL 不会给 final 类加
    // type id，导致外层 List 在 cache value 里没 type id，read 时 polymorphic
    // 反序列化报 "Unexpected token (START_ARRAY), expected VALUE_STRING"。
    // Collectors.toCollection(ArrayList::new) 强制返回 ArrayList（非 final），
    // Jackson 就会 wrap 成 ["java.util.ArrayList", [...]]，read 路径有 type id。
    return nodeMap.values().stream()
        .filter(node -> node.getParentId() == null || node.getParentId() == 0 || !nodeMap.containsKey(node.getParentId()))
        .sorted(comparator)
        .collect(Collectors.toCollection(ArrayList::new));
  }

  private void ensureCodeAvailable(Long tenantId, String code) {
    boolean exists = menuMapper.exists(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getCode, code)
        .eq(MenuEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Menu code already exists");
    }
  }

  private void validateParent(Long tenantId, Long parentId, Long currentId) {
    if (parentId == null || parentId == 0) {
      return;
    }
    if (parentId.equals(currentId)) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Menu parent cannot be itself");
    }
    boolean parentExists = menuMapper.exists(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getId, parentId)
        .eq(MenuEntity::getDeleted, 0));
    if (!parentExists) {
      throw new BusinessException(ErrorCode.BAD_REQUEST, "Menu parent not found");
    }
  }

  private MenuEntity getExisting(Long tenantId, Long id) {
    MenuEntity entity = menuMapper.selectOne(new LambdaQueryWrapper<MenuEntity>()
        .eq(MenuEntity::getTenantId, tenantId)
        .eq(MenuEntity::getId, id)
        .eq(MenuEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Menu not found");
    }
    return entity;
  }

  private MenuTreeVO toTreeVO(MenuEntity entity) {
    MenuTreeVO vo = new MenuTreeVO();
    vo.setId(entity.getId());
    vo.setParentId(entity.getParentId());
    vo.setMenuType(entity.getMenuType());
    vo.setCode(entity.getCode());
    vo.setName(entity.getName());
    vo.setPath(entity.getPath());
    vo.setComponent(entity.getComponent());
    vo.setPermissionCode(entity.getPermissionCode());
    vo.setIcon(entity.getIcon());
    vo.setSortOrder(entity.getSortOrder());
    vo.setVisible(entity.getVisible());
    vo.setStatus(entity.getStatus());
    return vo;
  }
}
