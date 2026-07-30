package com.guoyu.platform.knowledge.service;

import lombok.extern.slf4j.Slf4j;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.guoyu.platform.ai.entity.AiGenerationDraftEntity;
import com.guoyu.platform.ai.mapper.AiGenerationDraftMapper;
import com.guoyu.platform.common.api.PageResponse;
import com.guoyu.platform.common.exception.BusinessException;
import com.guoyu.platform.common.exception.ErrorCode;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.service.FileApplicationService;
import com.guoyu.platform.knowledge.dto.KnowledgeAnswerDraftCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeDocumentCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeDocumentQueryReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSearchReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSpaceCreateReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSpaceQueryReq;
import com.guoyu.platform.knowledge.dto.KnowledgeSpaceUpdateReq;
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
import com.guoyu.platform.knowledge.vo.KnowledgeCitationVO;
import com.guoyu.platform.knowledge.vo.KnowledgeDocumentVO;
import com.guoyu.platform.knowledge.vo.KnowledgeIndexTaskVO;
import com.guoyu.platform.knowledge.vo.KnowledgeSearchResultVO;
import com.guoyu.platform.knowledge.vo.KnowledgeSpaceVO;
import com.guoyu.platform.security.PlatformAdminSupport;
import com.guoyu.platform.security.PlatformUserPrincipal;
import com.guoyu.platform.security.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class KnowledgeApplicationService {

  private static final String MODEL_NAME = "local-rag-mvp";

  private final KnowledgeSpaceMapper spaceMapper;
  private final KnowledgeSpaceRoleMapper spaceRoleMapper;
  private final KnowledgeDocumentMapper documentMapper;
  private final KnowledgeDocumentAclMapper documentAclMapper;
  private final KnowledgeChunkMapper chunkMapper;
  private final KnowledgeIndexTaskMapper indexTaskMapper;
  private final AiGenerationDraftMapper aiGenerationDraftMapper;
  private final FileApplicationService fileApplicationService;
  private final KnowledgeDocumentParser documentParser;
  private final KnowledgeTextChunker textChunker;

  public KnowledgeApplicationService(
      KnowledgeSpaceMapper spaceMapper,
      KnowledgeSpaceRoleMapper spaceRoleMapper,
      KnowledgeDocumentMapper documentMapper,
      KnowledgeDocumentAclMapper documentAclMapper,
      KnowledgeChunkMapper chunkMapper,
      KnowledgeIndexTaskMapper indexTaskMapper,
      AiGenerationDraftMapper aiGenerationDraftMapper,
      FileApplicationService fileApplicationService,
      KnowledgeDocumentParser documentParser,
      KnowledgeTextChunker textChunker
  ) {
    this.spaceMapper = spaceMapper;
    this.spaceRoleMapper = spaceRoleMapper;
    this.documentMapper = documentMapper;
    this.documentAclMapper = documentAclMapper;
    this.chunkMapper = chunkMapper;
    this.indexTaskMapper = indexTaskMapper;
    this.aiGenerationDraftMapper = aiGenerationDraftMapper;
    this.fileApplicationService = fileApplicationService;
    this.documentParser = documentParser;
    this.textChunker = textChunker;
  }

  @Transactional(readOnly = true)
  public PageResponse<KnowledgeSpaceVO> pageSpaces(KnowledgeSpaceQueryReq req) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    LambdaQueryWrapper<KnowledgeSpaceEntity> wrapper = new LambdaQueryWrapper<KnowledgeSpaceEntity>()
        .eq(KnowledgeSpaceEntity::getTenantId, principal.getTenantId())
        .eq(KnowledgeSpaceEntity::getDeleted, 0)
        .orderByDesc(KnowledgeSpaceEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.and(item -> item
          .like(KnowledgeSpaceEntity::getCode, req.getKeyword())
          .or()
          .like(KnowledgeSpaceEntity::getName, req.getKeyword()));
    }
    if (StringUtils.hasText(req.getScopeType())) {
      wrapper.eq(KnowledgeSpaceEntity::getScopeType, req.getScopeType());
    }
    if (StringUtils.hasText(req.getCategoryCode())) {
      wrapper.eq(KnowledgeSpaceEntity::getCategoryCode, req.getCategoryCode());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(KnowledgeSpaceEntity::getStatus, req.getStatus());
    }

    List<KnowledgeSpaceVO> accessible = spaceMapper.selectList(wrapper).stream()
        .filter(space -> canReadSpace(principal, space))
        .map(this::toSpaceVO)
        .toList();
    return paginate(accessible, req.getPage(), req.getPageSize());
  }

  @Transactional(readOnly = true)
  public KnowledgeSpaceVO getSpace(Long id) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    KnowledgeSpaceEntity entity = getExistingSpace(principal.getTenantId(), id);
    ensureCanReadSpace(principal, entity);
    return toSpaceVO(entity);
  }

  @Transactional
  public KnowledgeSpaceVO createSpace(KnowledgeSpaceCreateReq req) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    ensureSpaceCodeAvailable(principal.getTenantId(), req.code());

    KnowledgeSpaceEntity entity = new KnowledgeSpaceEntity();
    entity.setTenantId(principal.getTenantId());
    entity.setCode(req.code());
    entity.setName(req.name());
    entity.setScopeType(req.scopeType());
    entity.setOwnerUserId(req.ownerUserId() == null ? principal.getUserId() : req.ownerUserId());
    entity.setCategoryCode(req.categoryCode());
    entity.setDescription(req.description());
    entity.setStatus(StringUtils.hasText(req.status()) ? req.status() : "enabled");
    entity.setDeleted(0);
    spaceMapper.insert(entity);
    replaceVisibleRoles(principal.getTenantId(), entity.getId(), req.visibleRoleCodes());
    return toSpaceVO(entity);
  }

  @Transactional
  public KnowledgeSpaceVO updateSpace(Long id, KnowledgeSpaceUpdateReq req) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    KnowledgeSpaceEntity entity = getExistingSpace(principal.getTenantId(), id);
    ensureCanManageSpace(principal, entity);
    entity.setName(req.name());
    entity.setCategoryCode(req.categoryCode());
    entity.setDescription(req.description());
    entity.setStatus(req.status());
    spaceMapper.updateById(entity);
    replaceVisibleRoles(principal.getTenantId(), entity.getId(), req.visibleRoleCodes());
    return toSpaceVO(entity);
  }

  @Transactional
  public void deleteSpace(Long id) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    KnowledgeSpaceEntity entity = getExistingSpace(principal.getTenantId(), id);
    ensureCanManageSpace(principal, entity);
    spaceMapper.deleteById(entity);
  }

  @Transactional(readOnly = true)
  public PageResponse<KnowledgeDocumentVO> pageDocuments(Long spaceId, KnowledgeDocumentQueryReq req) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    KnowledgeSpaceEntity space = getExistingSpace(principal.getTenantId(), spaceId);
    ensureCanReadSpace(principal, space);

    LambdaQueryWrapper<KnowledgeDocumentEntity> wrapper = new LambdaQueryWrapper<KnowledgeDocumentEntity>()
        .eq(KnowledgeDocumentEntity::getTenantId, principal.getTenantId())
        .eq(KnowledgeDocumentEntity::getSpaceId, spaceId)
        .eq(KnowledgeDocumentEntity::getDeleted, 0)
        .orderByDesc(KnowledgeDocumentEntity::getCreatedAt);
    if (StringUtils.hasText(req.getKeyword())) {
      wrapper.like(KnowledgeDocumentEntity::getTitle, req.getKeyword());
    }
    if (StringUtils.hasText(req.getCategoryCode())) {
      wrapper.eq(KnowledgeDocumentEntity::getCategoryCode, req.getCategoryCode());
    }
    if (StringUtils.hasText(req.getStatus())) {
      wrapper.eq(KnowledgeDocumentEntity::getStatus, req.getStatus());
    }

    List<KnowledgeDocumentVO> docs = documentMapper.selectList(wrapper).stream()
        .filter(document -> canReadDocument(principal, space, document))
        .map(this::toDocumentVO)
        .toList();
    return paginate(docs, req.getPage(), req.getPageSize());
  }

  @Transactional
  public KnowledgeDocumentVO ingestDocument(Long spaceId, KnowledgeDocumentCreateReq req) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    KnowledgeSpaceEntity space = getExistingSpace(principal.getTenantId(), spaceId);
    ensureCanManageSpace(principal, space);
    FileEntity file = fileApplicationService.getExistingFile(principal.getTenantId(), req.fileId());

    KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
    document.setTenantId(principal.getTenantId());
    document.setSpaceId(space.getId());
    document.setFileId(file.getId());
    document.setTitle(req.title());
    document.setCategoryCode(StringUtils.hasText(req.categoryCode()) ? req.categoryCode() : space.getCategoryCode());
    document.setSourceType(StringUtils.hasText(req.sourceType()) ? req.sourceType() : "file");
    document.setAccessScope(StringUtils.hasText(req.accessScope()) ? req.accessScope() : "inherit");
    document.setSensitiveLevel(StringUtils.hasText(req.sensitiveLevel()) ? req.sensitiveLevel() : "normal");
    document.setParserStatus("pending");
    document.setIndexStatus("pending");
    document.setChunkCount(0);
    document.setStatus("indexing");
    document.setDeleted(0);
    documentMapper.insert(document);
    replaceDocumentAcl(principal.getTenantId(), document.getId(), req.visibleUserIds(), req.visibleRoleCodes());

    KnowledgeIndexTaskEntity task = createIndexTask(principal.getTenantId(), space.getId(), document.getId());
    try {
      String text = documentParser.extractText(file, req.rawText());
      List<KnowledgeTextChunk> chunks = textChunker.chunk(text);
      if (chunks.isEmpty()) {
        markDocumentFailed(document, task, "Document contains no extractable text");
        return toDocumentVO(document);
      }
      replaceChunks(principal.getTenantId(), space.getId(), document.getId(), chunks);
      document.setParserStatus("parsed");
      document.setIndexStatus("indexed");
      document.setChunkCount(chunks.size());
      document.setTextSha256(sha256(text));
      document.setErrorMessage(null);
      document.setStatus("indexed");
      documentMapper.updateById(document);
      completeIndexTask(task);
      return toDocumentVO(document);
    } catch (BusinessException ex) {
      markDocumentFailed(document, task, ex.getMessage());
      return toDocumentVO(document);
    }
  }

  @Transactional(readOnly = true)
  public KnowledgeIndexTaskVO getIndexTask(Long id) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    KnowledgeIndexTaskEntity task = indexTaskMapper.selectOne(new LambdaQueryWrapper<KnowledgeIndexTaskEntity>()
        .eq(KnowledgeIndexTaskEntity::getTenantId, principal.getTenantId())
        .eq(KnowledgeIndexTaskEntity::getId, id)
        .eq(KnowledgeIndexTaskEntity::getDeleted, 0));
    if (task == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Knowledge index task not found");
    }
    KnowledgeSpaceEntity space = getExistingSpace(principal.getTenantId(), task.getSpaceId());
    ensureCanReadSpace(principal, space);
    return toIndexTaskVO(task);
  }

  @Transactional(readOnly = true)
  public List<KnowledgeSearchResultVO> search(KnowledgeSearchReq req) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    return searchInternal(principal, req.query(), req.spaceIds(), req.categoryCode(), req.topK());
  }

  @Transactional
  public KnowledgeAnswerDraftVO createAnswerDraft(KnowledgeAnswerDraftCreateReq req) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    List<KnowledgeSearchResultVO> citations = searchInternal(
        principal,
        req.question(),
        req.spaceIds(),
        req.categoryCode(),
        req.topK()
    );
    boolean evidenceSufficient = !citations.isEmpty();
    String answer = evidenceSufficient ? buildGroundedAnswer(req.question(), citations) : insufficientEvidenceAnswer();

    AiGenerationDraftEntity draft = new AiGenerationDraftEntity();
    draft.setTenantId(principal.getTenantId());
    draft.setDraftType("knowledge_answer");
    draft.setSourceType("knowledge_base");
    draft.setSourceId(citations.isEmpty() ? null : citations.get(0).spaceId());
    draft.setPrompt(req.question());
    draft.setResultText(answerWithCitationText(answer, citations));
    draft.setModelName(MODEL_NAME);
    draft.setStatus("generated");
    draft.setDeleted(0);
    aiGenerationDraftMapper.insert(draft);

    return new KnowledgeAnswerDraftVO(
        draft.getId(),
        answer,
        evidenceSufficient,
        MODEL_NAME,
        citations,
        draft.getCreatedAt()
    );
  }

  @Transactional(readOnly = true)
  public KnowledgeCitationVO getCitation(Long chunkId) {
    PlatformUserPrincipal principal = SecurityContext.currentUser();
    KnowledgeChunkEntity chunk = chunkMapper.selectOne(new LambdaQueryWrapper<KnowledgeChunkEntity>()
        .eq(KnowledgeChunkEntity::getTenantId, principal.getTenantId())
        .eq(KnowledgeChunkEntity::getId, chunkId)
        .eq(KnowledgeChunkEntity::getDeleted, 0));
    if (chunk == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Knowledge chunk not found");
    }
    KnowledgeSpaceEntity space = getExistingSpace(principal.getTenantId(), chunk.getSpaceId());
    ensureCanReadSpace(principal, space);
    KnowledgeDocumentEntity document = getExistingDocument(principal.getTenantId(), chunk.getDocumentId());
    ensureCanReadDocument(principal, space, document);
    return new KnowledgeCitationVO(
        chunk.getId(),
        chunk.getTenantId(),
        space.getId(),
        space.getName(),
        document.getId(),
        document.getTitle(),
        document.getFileId(),
        chunk.getHeading(),
        chunk.getPageNo(),
        chunk.getChunkNo(),
        chunk.getContent(),
        chunk.getCreatedAt()
    );
  }

  private List<KnowledgeSearchResultVO> searchInternal(
      PlatformUserPrincipal principal,
      String query,
      List<Long> requestedSpaceIds,
      String categoryCode,
      Integer topK
  ) {
    int limit = sanitizeTopK(topK);
    Map<Long, KnowledgeSpaceEntity> spaces = accessibleSpaces(principal, requestedSpaceIds);
    if (spaces.isEmpty()) {
      return List.of();
    }

    List<String> terms = queryTerms(query);
    LambdaQueryWrapper<KnowledgeChunkEntity> wrapper = new LambdaQueryWrapper<KnowledgeChunkEntity>()
        .eq(KnowledgeChunkEntity::getTenantId, principal.getTenantId())
        .in(KnowledgeChunkEntity::getSpaceId, spaces.keySet())
        .eq(KnowledgeChunkEntity::getDeleted, 0)
        .last("LIMIT 300");
    if (!terms.isEmpty()) {
      wrapper.and(item -> {
        for (int i = 0; i < terms.size(); i++) {
          if (i > 0) {
            item.or();
          }
          item.like(KnowledgeChunkEntity::getContent, terms.get(i));
        }
      });
    }

    Map<Long, KnowledgeDocumentEntity> documents = new LinkedHashMap<>();
    return chunkMapper.selectList(wrapper).stream()
        .map(chunk -> toSearchResult(principal, query, terms, spaces, documents, categoryCode, chunk))
        .filter(Objects::nonNull)
        .sorted(Comparator.comparingDouble(KnowledgeSearchResultVO::score).reversed())
        .limit(limit)
        .toList();
  }

  private KnowledgeSearchResultVO toSearchResult(
      PlatformUserPrincipal principal,
      String query,
      List<String> terms,
      Map<Long, KnowledgeSpaceEntity> spaces,
      Map<Long, KnowledgeDocumentEntity> documents,
      String categoryCode,
      KnowledgeChunkEntity chunk
  ) {
    KnowledgeDocumentEntity document = documents.computeIfAbsent(
        chunk.getDocumentId(),
        id -> getExistingDocument(chunk.getTenantId(), id)
    );
    if (!"indexed".equals(document.getStatus())) {
      return null;
    }
    KnowledgeSpaceEntity space = spaces.get(chunk.getSpaceId());
    if (!canReadDocument(principal, space, document)) {
      return null;
    }
    if (StringUtils.hasText(categoryCode) && !categoryCode.equals(document.getCategoryCode())) {
      return null;
    }
    double score = score(query, terms, chunk.getContent());
    if (score <= 0) {
      return null;
    }
    return new KnowledgeSearchResultVO(
        chunk.getId(),
        space.getId(),
        space.getName(),
        document.getId(),
        document.getTitle(),
        chunk.getHeading(),
        chunk.getPageNo(),
        chunk.getContent(),
        score,
        citationLabel(document, chunk)
    );
  }

  private Map<Long, KnowledgeSpaceEntity> accessibleSpaces(PlatformUserPrincipal principal, List<Long> requestedSpaceIds) {
    LambdaQueryWrapper<KnowledgeSpaceEntity> wrapper = new LambdaQueryWrapper<KnowledgeSpaceEntity>()
        .eq(KnowledgeSpaceEntity::getTenantId, principal.getTenantId())
        .eq(KnowledgeSpaceEntity::getDeleted, 0)
        .eq(KnowledgeSpaceEntity::getStatus, "enabled");
    if (requestedSpaceIds != null && !requestedSpaceIds.isEmpty()) {
      wrapper.in(KnowledgeSpaceEntity::getId, requestedSpaceIds);
    }
    Map<Long, KnowledgeSpaceEntity> spaces = new LinkedHashMap<>();
    for (KnowledgeSpaceEntity space : spaceMapper.selectList(wrapper)) {
      if (canReadSpace(principal, space)) {
        spaces.put(space.getId(), space);
      }
    }
    return spaces;
  }

  private boolean canReadSpace(PlatformUserPrincipal principal, KnowledgeSpaceEntity space) {
    if (isPlatformAdmin(principal)) {
      return true;
    }
    if (Objects.equals(space.getOwnerUserId(), principal.getUserId())) {
      return true;
    }
    List<String> visibleRoleCodes = visibleRoleCodes(space.getTenantId(), space.getId());
    if (visibleRoleCodes.isEmpty()) {
      return false;
    }
    Set<String> userRoleCodes = new HashSet<>(principal.getRoleCodes());
    return visibleRoleCodes.stream().anyMatch(userRoleCodes::contains);
  }

  private void ensureCanReadSpace(PlatformUserPrincipal principal, KnowledgeSpaceEntity space) {
    if (!canReadSpace(principal, space)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "No permission to access knowledge base");
    }
  }

  private void ensureCanManageSpace(PlatformUserPrincipal principal, KnowledgeSpaceEntity space) {
    if (!isPlatformAdmin(principal) && !Objects.equals(space.getOwnerUserId(), principal.getUserId())) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "No permission to manage knowledge base");
    }
  }

  private boolean canReadDocument(
      PlatformUserPrincipal principal,
      KnowledgeSpaceEntity space,
      KnowledgeDocumentEntity document
  ) {
    if (!canReadSpace(principal, space)) {
      return false;
    }
    if (isPlatformAdmin(principal) || Objects.equals(space.getOwnerUserId(), principal.getUserId())) {
      return true;
    }
    if (!"restricted".equals(document.getAccessScope())) {
      return true;
    }
    return documentAclAllows(principal, document.getTenantId(), document.getId());
  }

  private void ensureCanReadDocument(
      PlatformUserPrincipal principal,
      KnowledgeSpaceEntity space,
      KnowledgeDocumentEntity document
  ) {
    if (!canReadDocument(principal, space, document)) {
      throw new BusinessException(ErrorCode.FORBIDDEN, "No permission to access knowledge document");
    }
  }

  private boolean isPlatformAdmin(PlatformUserPrincipal principal) {
    return PlatformAdminSupport.isSuperAdmin(principal);
  }

  private KnowledgeSpaceEntity getExistingSpace(Long tenantId, Long id) {
    KnowledgeSpaceEntity entity = spaceMapper.selectOne(new LambdaQueryWrapper<KnowledgeSpaceEntity>()
        .eq(KnowledgeSpaceEntity::getTenantId, tenantId)
        .eq(KnowledgeSpaceEntity::getId, id)
        .eq(KnowledgeSpaceEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Knowledge base not found");
    }
    return entity;
  }

  private KnowledgeDocumentEntity getExistingDocument(Long tenantId, Long id) {
    KnowledgeDocumentEntity entity = documentMapper.selectOne(new LambdaQueryWrapper<KnowledgeDocumentEntity>()
        .eq(KnowledgeDocumentEntity::getTenantId, tenantId)
        .eq(KnowledgeDocumentEntity::getId, id)
        .eq(KnowledgeDocumentEntity::getDeleted, 0));
    if (entity == null) {
      throw new BusinessException(ErrorCode.NOT_FOUND, "Knowledge document not found");
    }
    return entity;
  }

  private void ensureSpaceCodeAvailable(Long tenantId, String code) {
    boolean exists = spaceMapper.exists(new LambdaQueryWrapper<KnowledgeSpaceEntity>()
        .eq(KnowledgeSpaceEntity::getTenantId, tenantId)
        .eq(KnowledgeSpaceEntity::getCode, code)
        .eq(KnowledgeSpaceEntity::getDeleted, 0));
    if (exists) {
      throw new BusinessException(ErrorCode.CONFLICT, "Knowledge base code already exists");
    }
  }

  private void replaceVisibleRoles(Long tenantId, Long spaceId, List<String> visibleRoleCodes) {
    spaceRoleMapper.delete(new LambdaQueryWrapper<KnowledgeSpaceRoleEntity>()
        .eq(KnowledgeSpaceRoleEntity::getTenantId, tenantId)
        .eq(KnowledgeSpaceRoleEntity::getSpaceId, spaceId)
        .eq(KnowledgeSpaceRoleEntity::getDeleted, 0));
    if (visibleRoleCodes == null || visibleRoleCodes.isEmpty()) {
      return;
    }
    visibleRoleCodes.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .distinct()
        .forEach(roleCode -> {
          KnowledgeSpaceRoleEntity relation = new KnowledgeSpaceRoleEntity();
          relation.setTenantId(tenantId);
          relation.setSpaceId(spaceId);
          relation.setRoleCode(roleCode);
          relation.setDeleted(0);
          spaceRoleMapper.insert(relation);
        });
  }

  private void replaceDocumentAcl(
      Long tenantId,
      Long documentId,
      List<Long> visibleUserIds,
      List<String> visibleRoleCodes
  ) {
    documentAclMapper.delete(new LambdaQueryWrapper<KnowledgeDocumentAclEntity>()
        .eq(KnowledgeDocumentAclEntity::getTenantId, tenantId)
        .eq(KnowledgeDocumentAclEntity::getDocumentId, documentId)
        .eq(KnowledgeDocumentAclEntity::getDeleted, 0));
    if (visibleUserIds != null) {
      visibleUserIds.stream()
          .filter(Objects::nonNull)
          .distinct()
          .forEach(userId -> insertDocumentAcl(tenantId, documentId, "user", String.valueOf(userId)));
    }
    if (visibleRoleCodes != null) {
      visibleRoleCodes.stream()
          .filter(StringUtils::hasText)
          .map(String::trim)
          .distinct()
          .forEach(roleCode -> insertDocumentAcl(tenantId, documentId, "role", roleCode));
    }
  }

  private void insertDocumentAcl(Long tenantId, Long documentId, String principalType, String principalValue) {
    KnowledgeDocumentAclEntity acl = new KnowledgeDocumentAclEntity();
    acl.setTenantId(tenantId);
    acl.setDocumentId(documentId);
    acl.setPrincipalType(principalType);
    acl.setPrincipalValue(principalValue);
    acl.setDeleted(0);
    documentAclMapper.insert(acl);
  }

  private boolean documentAclAllows(PlatformUserPrincipal principal, Long tenantId, Long documentId) {
    List<KnowledgeDocumentAclEntity> acl = documentAclMapper.selectList(new LambdaQueryWrapper<KnowledgeDocumentAclEntity>()
        .eq(KnowledgeDocumentAclEntity::getTenantId, tenantId)
        .eq(KnowledgeDocumentAclEntity::getDocumentId, documentId)
        .eq(KnowledgeDocumentAclEntity::getDeleted, 0));
    if (acl.isEmpty()) {
      return false;
    }
    Set<String> roleCodes = new HashSet<>(principal.getRoleCodes());
    String userId = String.valueOf(principal.getUserId());
    return acl.stream().anyMatch(item ->
        ("user".equals(item.getPrincipalType()) && userId.equals(item.getPrincipalValue()))
            || ("role".equals(item.getPrincipalType()) && roleCodes.contains(item.getPrincipalValue()))
    );
  }

  private List<String> visibleRoleCodes(Long tenantId, Long spaceId) {
    return spaceRoleMapper.selectList(new LambdaQueryWrapper<KnowledgeSpaceRoleEntity>()
            .eq(KnowledgeSpaceRoleEntity::getTenantId, tenantId)
            .eq(KnowledgeSpaceRoleEntity::getSpaceId, spaceId)
            .eq(KnowledgeSpaceRoleEntity::getDeleted, 0))
        .stream()
        .map(KnowledgeSpaceRoleEntity::getRoleCode)
        .toList();
  }

  private KnowledgeIndexTaskEntity createIndexTask(Long tenantId, Long spaceId, Long documentId) {
    KnowledgeIndexTaskEntity task = new KnowledgeIndexTaskEntity();
    task.setTenantId(tenantId);
    task.setSpaceId(spaceId);
    task.setDocumentId(documentId);
    task.setTaskType("ingest");
    task.setStatus("running");
    task.setRetryCount(0);
    task.setStartedAt(LocalDateTime.now());
    task.setDeleted(0);
    indexTaskMapper.insert(task);
    return task;
  }

  private void completeIndexTask(KnowledgeIndexTaskEntity task) {
    task.setStatus("completed");
    task.setFinishedAt(LocalDateTime.now());
    task.setErrorMessage(null);
    indexTaskMapper.updateById(task);
  }

  private void markDocumentFailed(KnowledgeDocumentEntity document, KnowledgeIndexTaskEntity task, String message) {
    String errorMessage = truncate(message, 512);
    document.setParserStatus("failed");
    document.setIndexStatus("failed");
    document.setStatus("failed");
    document.setErrorMessage(errorMessage);
    documentMapper.updateById(document);

    task.setStatus("failed");
    task.setErrorMessage(errorMessage);
    task.setFinishedAt(LocalDateTime.now());
    indexTaskMapper.updateById(task);
  }

  private void replaceChunks(Long tenantId, Long spaceId, Long documentId, List<KnowledgeTextChunk> chunks) {
    chunkMapper.delete(new LambdaQueryWrapper<KnowledgeChunkEntity>()
        .eq(KnowledgeChunkEntity::getTenantId, tenantId)
        .eq(KnowledgeChunkEntity::getDocumentId, documentId)
        .eq(KnowledgeChunkEntity::getDeleted, 0));
    for (KnowledgeTextChunk textChunk : chunks) {
      KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
      chunk.setTenantId(tenantId);
      chunk.setSpaceId(spaceId);
      chunk.setDocumentId(documentId);
      chunk.setChunkNo(textChunk.chunkNo());
      chunk.setHeading(textChunk.heading());
      chunk.setPageNo(textChunk.pageNo());
      chunk.setContent(textChunk.content());
      chunk.setTokenCount(textChunk.tokenCount());
      chunk.setEmbeddingProvider("ai-gateway");
      chunk.setEmbeddingModel("configured-by-ai-gateway");
      chunk.setVectorStatus("pending");
      chunk.setMetadataJson("{}");
      chunk.setDeleted(0);
      chunkMapper.insert(chunk);
    }
  }

  private List<String> queryTerms(String query) {
    if (!StringUtils.hasText(query)) {
      return List.of();
    }
    String normalized = query.toLowerCase(Locale.ROOT).trim();
    String[] parts = normalized.split("[\\s,，。；;]+");
    List<String> terms = new ArrayList<>();
    for (String part : parts) {
      if (StringUtils.hasText(part)) {
        terms.add(part);
      }
    }
    return terms.isEmpty() ? List.of(normalized) : terms;
  }

  private double score(String query, List<String> terms, String content) {
    String lower = content.toLowerCase(Locale.ROOT);
    double score = lower.contains(query.toLowerCase(Locale.ROOT)) ? 5.0 : 0.0;
    for (String term : terms) {
      score += occurrences(lower, term);
    }
    return score;
  }

  private int occurrences(String content, String term) {
    int count = 0;
    int index = content.indexOf(term);
    while (index >= 0) {
      count++;
      index = content.indexOf(term, index + term.length());
    }
    return count;
  }

  private int sanitizeTopK(Integer topK) {
    if (topK == null) {
      return 5;
    }
    return Math.min(Math.max(topK, 1), 20);
  }

  private String buildGroundedAnswer(String question, List<KnowledgeSearchResultVO> citations) {
    StringBuilder answer = new StringBuilder();
    answer.append("根据当前可访问知识库资料，问题“").append(question).append("”可参考以下内容：");
    int index = 1;
    for (KnowledgeSearchResultVO citation : citations.stream().limit(3).toList()) {
      answer.append("\n").append(index++).append(". ")
          .append(snippet(citation.content(), 180))
          .append("（").append(citation.citationLabel()).append("）");
    }
    answer.append("\n以上为知识库检索片段生成的草稿，采纳前需人工复核。");
    return answer.toString();
  }

  private String insufficientEvidenceAnswer() {
    return "未找到可支撑回答的知识库资料，不能基于当前知识库给出确定结论。";
  }

  private String answerWithCitationText(String answer, List<KnowledgeSearchResultVO> citations) {
    if (citations.isEmpty()) {
      return answer;
    }
    StringBuilder builder = new StringBuilder(answer).append("\n\n引用来源：");
    for (KnowledgeSearchResultVO citation : citations) {
      builder.append("\n- ").append(citation.citationLabel()).append("，chunkId=").append(citation.chunkId());
    }
    return builder.toString();
  }

  private String citationLabel(KnowledgeDocumentEntity document, KnowledgeChunkEntity chunk) {
    StringBuilder builder = new StringBuilder(document.getTitle())
        .append(" / 片段 ")
        .append(chunk.getChunkNo());
    if (chunk.getPageNo() != null) {
      builder.append(" / 页码 ").append(chunk.getPageNo());
    }
    return builder.toString();
  }

  private String snippet(String content, int maxChars) {
    String compact = content.replaceAll("\\s+", " ").trim();
    if (compact.length() <= maxChars) {
      return compact;
    }
    return compact.substring(0, maxChars) + "...";
  }

  private String sha256(String text) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 digest unavailable", ex);
    }
  }

  private String truncate(String message, int maxLength) {
    if (message == null || message.length() <= maxLength) {
      return message;
    }
    return message.substring(0, maxLength);
  }

  private <T> PageResponse<T> paginate(List<T> items, long page, long pageSize) {
    int fromIndex = (int) Math.min(items.size(), (page - 1) * pageSize);
    int toIndex = (int) Math.min(items.size(), fromIndex + pageSize);
    return new PageResponse<>(items.subList(fromIndex, toIndex), page, pageSize, items.size());
  }

  private KnowledgeSpaceVO toSpaceVO(KnowledgeSpaceEntity entity) {
    return new KnowledgeSpaceVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getCode(),
        entity.getName(),
        entity.getScopeType(),
        entity.getOwnerUserId(),
        entity.getCategoryCode(),
        entity.getDescription(),
        entity.getStatus(),
        visibleRoleCodes(entity.getTenantId(), entity.getId()),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  private KnowledgeDocumentVO toDocumentVO(KnowledgeDocumentEntity entity) {
    return new KnowledgeDocumentVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getSpaceId(),
        entity.getFileId(),
        entity.getTitle(),
        entity.getCategoryCode(),
        entity.getSourceType(),
        entity.getAccessScope(),
        entity.getSensitiveLevel(),
        entity.getParserStatus(),
        entity.getIndexStatus(),
        entity.getChunkCount(),
        entity.getTextSha256(),
        entity.getErrorMessage(),
        entity.getStatus(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }

  private KnowledgeIndexTaskVO toIndexTaskVO(KnowledgeIndexTaskEntity entity) {
    return new KnowledgeIndexTaskVO(
        entity.getId(),
        entity.getTenantId(),
        entity.getSpaceId(),
        entity.getDocumentId(),
        entity.getTaskType(),
        entity.getStatus(),
        entity.getRetryCount(),
        entity.getErrorMessage(),
        entity.getStartedAt(),
        entity.getFinishedAt(),
        entity.getCreatedAt(),
        entity.getUpdatedAt()
    );
  }
}
