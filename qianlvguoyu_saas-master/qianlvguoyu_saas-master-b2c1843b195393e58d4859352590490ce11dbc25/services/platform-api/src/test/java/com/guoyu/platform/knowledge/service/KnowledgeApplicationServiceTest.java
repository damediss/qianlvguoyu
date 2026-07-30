package com.guoyu.platform.knowledge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.knowledge.dto.KnowledgeAnswerDraftCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeDocumentCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSearchReq;
import com.guoyu.platform.knowledge.entity.KnowledgeChunkEntity;
import com.guoyu.platform.knowledge.entity.KnowledgeDocumentAclEntity;
import com.guoyu.platform.knowledge.entity.KnowledgeDocumentEntity;
import com.guoyu.platform.knowledge.entity.KnowledgeIndexTaskEntity;
import com.guoyu.platform.knowledge.entity.KnowledgeSpaceEntity;
import com.guoyu.platform.knowledge.entity.KnowledgeSpaceRoleEntity;
import com.guoyu.platform.knowledge.mapper.KnowledgeChunkMapper;
import com.guoyu.platform.knowledge.mapper.KnowledgeDocumentAclMapper;
import com.guoyu.platform.knowledge.mapper.KnowledgeDocumentMapper;
import com.guoyu.platform.knowledge.mapper.KnowledgeIndexTaskMapper;
import com.guoyu.platform.knowledge.mapper.KnowledgeSpaceMapper;
import com.guoyu.platform.knowledge.mapper.KnowledgeSpaceRoleMapper;
import com.guoyu.platform.knowledge.vo.KnowledgeAnswerDraftVO;
import com.guoyu.platform.knowledge.vo.KnowledgeDocumentVO;
import com.guoyu.platform.knowledge.vo.KnowledgeSearchResultVO;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class KnowledgeApplicationServiceTest {

  private final KnowledgeSpaceMapper spaceMapper = Mockito.mock(KnowledgeSpaceMapper.class);
  private final KnowledgeSpaceRoleMapper spaceRoleMapper = Mockito.mock(KnowledgeSpaceRoleMapper.class);
  private final KnowledgeDocumentMapper documentMapper = Mockito.mock(KnowledgeDocumentMapper.class);
  private final KnowledgeDocumentAclMapper documentAclMapper = Mockito.mock(KnowledgeDocumentAclMapper.class);
  private final KnowledgeChunkMapper chunkMapper = Mockito.mock(KnowledgeChunkMapper.class);
  private final KnowledgeIndexTaskMapper indexTaskMapper = Mockito.mock(KnowledgeIndexTaskMapper.class);
  private final AiGenerationDraftMapper aiGenerationDraftMapper = Mockito.mock(AiGenerationDraftMapper.class);
  private final FileApplicationService fileApplicationService = Mockito.mock(FileApplicationService.class);
  private final KnowledgeDocumentParser documentParser = Mockito.mock(KnowledgeDocumentParser.class);
  private final KnowledgeApplicationService service = new KnowledgeApplicationService(
      spaceMapper,
      spaceRoleMapper,
      documentMapper,
      documentAclMapper,
      chunkMapper,
      indexTaskMapper,
      aiGenerationDraftMapper,
      fileApplicationService,
      documentParser,
      new KnowledgeTextChunker()
  );

  @BeforeEach
  void setUp() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L,
        1L,
        "course",
        "课程人员",
        "business",
        List.of("COURSE_STAFF"),
        List.of("knowledge:search", "knowledge:answer", "knowledge:document")
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void searchSkipsKnowledgeBaseWhenRoleCannotReadIt() {
    KnowledgeSpaceEntity privateSpace = space(10L, 2000L, "enabled");
    when(spaceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(privateSpace));
    when(spaceRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

    List<KnowledgeSearchResultVO> results = service.search(new KnowledgeSearchReq("电工", null, null, 5));

    assertThat(results).isEmpty();
    verify(chunkMapper, never()).selectList(any(Wrapper.class));
  }

  @Test
  void answerDraftStoresInsufficientEvidenceWhenNoCitationIsFound() {
    when(spaceMapper.selectList(any(Wrapper.class))).thenReturn(List.of());
    Mockito.doAnswer(invocation -> {
      AiGenerationDraftEntity draft = invocation.getArgument(0);
      draft.setId(900L);
      return 1;
    }).when(aiGenerationDraftMapper).insert(any(AiGenerationDraftEntity.class));

    KnowledgeAnswerDraftVO draft = service.createAnswerDraft(
        new KnowledgeAnswerDraftCreateReq("公司报销制度是什么？", null, null, 5)
    );

    assertThat(draft.draftId()).isEqualTo(900L);
    assertThat(draft.evidenceSufficient()).isFalse();
    assertThat(draft.answer()).contains("未找到可支撑回答的知识库资料");
  }

  @Test
  void searchSkipsRestrictedDocumentWhenUserIsNotInDocumentAcl() {
    KnowledgeSpaceEntity space = space(10L, 2000L, "enabled");
    KnowledgeDocumentEntity document = document(30L, "restricted");
    KnowledgeChunkEntity chunk = chunk(50L, "合同金额为 100 万元，签署人为张三。");
    when(spaceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(space));
    when(spaceRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(spaceRole("COURSE_STAFF")));
    when(chunkMapper.selectList(any(Wrapper.class))).thenReturn(List.of(chunk));
    when(documentMapper.selectOne(any(Wrapper.class))).thenReturn(document);
    when(documentAclMapper.selectList(any(Wrapper.class))).thenReturn(List.of());

    List<KnowledgeSearchResultVO> results = service.search(new KnowledgeSearchReq("合同金额", null, null, 5));

    assertThat(results).isEmpty();
  }

  @Test
  void searchAllowsRestrictedDocumentForSignerAclUser() {
    KnowledgeSpaceEntity space = space(10L, 2000L, "enabled");
    KnowledgeDocumentEntity document = document(30L, "restricted");
    KnowledgeChunkEntity chunk = chunk(50L, "合同金额为 100 万元，签署人为张三。");
    when(spaceMapper.selectList(any(Wrapper.class))).thenReturn(List.of(space));
    when(spaceRoleMapper.selectList(any(Wrapper.class))).thenReturn(List.of(spaceRole("COURSE_STAFF")));
    when(chunkMapper.selectList(any(Wrapper.class))).thenReturn(List.of(chunk));
    when(documentMapper.selectOne(any(Wrapper.class))).thenReturn(document);
    when(documentAclMapper.selectList(any(Wrapper.class))).thenReturn(List.of(documentUserAcl(1000L)));

    List<KnowledgeSearchResultVO> results = service.search(new KnowledgeSearchReq("合同金额", null, null, 5));

    assertThat(results).hasSize(1);
    assertThat(results.getFirst().content()).contains("100 万元");
  }

  @Test
  void ingestDocumentIndexesRawTextIntoChunks() {
    setAdminPrincipal();
    KnowledgeSpaceEntity space = space(10L, 1000L, "enabled");
    FileEntity file = new FileEntity();
    file.setId(20L);
    file.setTenantId(1L);
    file.setStatus("uploaded");
    when(spaceMapper.selectOne(any(Wrapper.class))).thenReturn(space);
    when(fileApplicationService.getExistingFile(1L, 20L)).thenReturn(file);
    when(documentParser.extractText(file, "电工安全操作\n\n考试前必须检查工具。"))
        .thenReturn("电工安全操作\n\n考试前必须检查工具。");
    Mockito.doAnswer(invocation -> {
      KnowledgeDocumentEntity document = invocation.getArgument(0);
      document.setId(30L);
      return 1;
    }).when(documentMapper).insert(any(KnowledgeDocumentEntity.class));
    Mockito.doAnswer(invocation -> {
      KnowledgeIndexTaskEntity task = invocation.getArgument(0);
      task.setId(40L);
      return 1;
    }).when(indexTaskMapper).insert(any(KnowledgeIndexTaskEntity.class));

    KnowledgeDocumentVO document = service.ingestDocument(
        10L,
        new KnowledgeDocumentCreateReq(
            20L,
            "电工安全资料",
            "course",
            "file",
            "restricted",
            "confidential",
            List.of(1000L),
            List.of("PLATFORM_ADMIN"),
            "电工安全操作\n\n考试前必须检查工具。"
        )
    );

    ArgumentCaptor<KnowledgeChunkEntity> chunkCaptor = ArgumentCaptor.forClass(KnowledgeChunkEntity.class);
    verify(chunkMapper).insert(chunkCaptor.capture());
    assertThat(document.status()).isEqualTo("indexed");
    assertThat(document.accessScope()).isEqualTo("restricted");
    assertThat(document.sensitiveLevel()).isEqualTo("confidential");
    assertThat(document.chunkCount()).isEqualTo(1);
    assertThat(chunkCaptor.getValue().getContent()).contains("电工安全操作");
    assertThat(chunkCaptor.getValue().getVectorStatus()).isEqualTo("pending");
  }

  private void setAdminPrincipal() {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        1000L,
        1L,
        "admin",
        "平台管理员",
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of("knowledge:document")
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );
  }

  private KnowledgeSpaceEntity space(Long id, Long ownerUserId, String status) {
    KnowledgeSpaceEntity entity = new KnowledgeSpaceEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setCode("kb-" + id);
    entity.setName("知识库" + id);
    entity.setScopeType("enterprise");
    entity.setOwnerUserId(ownerUserId);
    entity.setStatus(status);
    entity.setDeleted(0);
    return entity;
  }

  private KnowledgeSpaceRoleEntity spaceRole(String roleCode) {
    KnowledgeSpaceRoleEntity entity = new KnowledgeSpaceRoleEntity();
    entity.setId(70L);
    entity.setTenantId(1L);
    entity.setSpaceId(10L);
    entity.setRoleCode(roleCode);
    entity.setDeleted(0);
    return entity;
  }

  private KnowledgeDocumentEntity document(Long id, String accessScope) {
    KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setSpaceId(10L);
    entity.setFileId(20L);
    entity.setTitle("合同资料");
    entity.setSourceType("file");
    entity.setAccessScope(accessScope);
    entity.setSensitiveLevel("confidential");
    entity.setStatus("indexed");
    entity.setDeleted(0);
    return entity;
  }

  private KnowledgeChunkEntity chunk(Long id, String content) {
    KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
    entity.setId(id);
    entity.setTenantId(1L);
    entity.setSpaceId(10L);
    entity.setDocumentId(30L);
    entity.setChunkNo(1);
    entity.setContent(content);
    entity.setDeleted(0);
    return entity;
  }

  private KnowledgeDocumentAclEntity documentUserAcl(Long userId) {
    KnowledgeDocumentAclEntity entity = new KnowledgeDocumentAclEntity();
    entity.setId(80L);
    entity.setTenantId(1L);
    entity.setDocumentId(30L);
    entity.setPrincipalType("user");
    entity.setPrincipalValue(String.valueOf(userId));
    entity.setDeleted(0);
    return entity;
  }
}
