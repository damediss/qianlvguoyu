package com.guoyu.platform.certification.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.guoyu.platform.certification.collection.domain.CollectionEligibilityResult;
import com.guoyu.platform.certification.collection.domain.PracticalCollectionEligibilityService;
import com.guoyu.platform.certification.entity.CertificationCandidateDocumentEntity;
import com.guoyu.platform.certification.entity.CertificationCandidateEntity;
import com.guoyu.platform.certification.entity.CertificationTaskEntity;
import com.guoyu.platform.certification.mapper.CertificationCandidateDocumentMapper;
import com.guoyu.platform.certification.mapper.CertificationCandidateMapper;
import com.guoyu.platform.certification.mapper.CertificationTaskMapper;
import com.guoyu.platform.file.entity.FileEntity;
import com.guoyu.platform.file.mapper.FileMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PracticalCollectionEligibilityServiceTest {

  @Mock CertificationTaskMapper taskMapper;
  @Mock CertificationCandidateMapper candidateMapper;
  @Mock CertificationCandidateDocumentMapper documentMapper;
  @Mock FileMapper fileMapper;

  private PracticalCollectionEligibilityService service;
  private CertificationCandidateEntity candidate;
  private List<CertificationCandidateDocumentEntity> documents;
  private List<FileEntity> files;

  @BeforeEach
  void setUp() {
    service = new PracticalCollectionEligibilityService(taskMapper, candidateMapper, documentMapper, fileMapper);
    candidate = new CertificationCandidateEntity();
    candidate.setId(100L);
    candidate.setTenantId(1L);
    candidate.setTaskId(10L);
    candidate.setCandidateName("张三");
    candidate.setAdmissionTicketNo("20260001");
    candidate.setStatus("active");
    documents = PracticalCollectionEligibilityService.REQUIRED_DOCUMENT_CODES.stream().map(code -> {
      CertificationCandidateDocumentEntity document = new CertificationCandidateDocumentEntity();
      document.setId(Long.parseLong(code.substring(3)));
      document.setTenantId(1L);
      document.setTaskId(10L);
      document.setCandidateId(100L);
      document.setDocCode(code);
      document.setFileId(Long.parseLong(code.substring(3)));
      document.setStatus("approved");
      document.setDeleted(0);
      return document;
    }).toList();
    files = documents.stream().map(document -> {
      FileEntity file = new FileEntity();
      file.setId(document.getFileId());
      file.setTenantId(1L);
      file.setStatus("uploaded");
      file.setDeleted(0);
      return file;
    }).toList();
    lenient().when(documentMapper.selectList(any())).thenReturn(documents);
    lenient().when(fileMapper.selectList(any())).thenReturn(files);
  }

  @Test
  void allRequiredDocumentsAndFilesAreEligible() {
    CollectionEligibilityResult result = service.checkCandidates(1L, 10L, List.of(candidate)).get(100L);

    assertThat(result.eligible()).isTrue();
    assertThat(result.documentStatus()).isEqualTo("COMPLETED");
    assertThat(result.missingDocumentCodes()).isEmpty();
    assertThat(result.invalidDocumentCodes()).isEmpty();
    assertThat(result.avatarFileId()).isEqualTo(2L);
  }

  @Test
  void missingDocumentIsIncomplete() {
    when(documentMapper.selectList(any())).thenReturn(documents.subList(1, documents.size()));

    CollectionEligibilityResult result = service.checkCandidates(1L, 10L, List.of(candidate)).get(100L);

    assertThat(result.eligible()).isFalse();
    assertThat(result.missingDocumentCodes()).containsExactly("DOC001");
    assertThat(result.reasonMessage()).contains("DOC001");
  }

  @Test
  void pendingReviewOrDeletedFileIsInvalid() {
    documents.get(2).setStatus("pending_review");
    files = files.subList(0, 3);
    when(fileMapper.selectList(any())).thenReturn(files);

    CollectionEligibilityResult result = service.checkCandidates(1L, 10L, List.of(candidate)).get(100L);

    assertThat(result.eligible()).isFalse();
    assertThat(result.documentStatus()).isEqualTo("INVALID");
    assertThat(result.invalidDocumentCodes()).containsExactlyInAnyOrder("DOC003", "DOC004");
  }

  @Test
  void directCheckRejectsCandidateOutsideCurrentTask() {
    when(taskMapper.selectOne(any())).thenReturn(new CertificationTaskEntity());
    when(candidateMapper.selectOne(any())).thenReturn(null);

    CollectionEligibilityResult result = service.check(1L, 10L, 999L);

    assertThat(result.eligible()).isFalse();
    assertThat(result.reasonCode()).isEqualTo("CANDIDATE_NOT_FOUND");
  }
}
