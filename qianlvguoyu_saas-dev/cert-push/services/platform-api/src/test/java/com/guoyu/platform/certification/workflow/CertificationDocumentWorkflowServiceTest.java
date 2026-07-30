package com.guoyu.platform.certification.workflow;

import static org.assertj.core.api.Assertions.assertThat;

import com.guoyu.platform.certification.entity.CertificationTaskDocumentEntity;
import com.guoyu.platform.certification.vo.CertificationDocumentWorkflowRecordVO;
import com.guoyu.platform.common.tenant.TenantContextHolder;
import com.guoyu.platform.security.PlatformUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CertificationDocumentWorkflowServiceTest {

  @Autowired
  private CertificationDocumentWorkflowService workflowService;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    TenantContextHolder.setTenantId(1L);
    jdbcTemplate.execute("DROP TABLE IF EXISTS certification_document_workflow_record");
    jdbcTemplate.execute("""
        CREATE TABLE certification_document_workflow_record (
          id BIGINT NOT NULL PRIMARY KEY,
          tenant_id BIGINT NOT NULL DEFAULT 1,
          task_id BIGINT NOT NULL,
          document_id BIGINT NOT NULL,
          document_type VARCHAR(40) NOT NULL,
          process_instance_id VARCHAR(128) NULL,
          task_id_in_engine VARCHAR(128) NULL,
          node_key VARCHAR(80) NULL,
          node_name VARCHAR(120) NULL,
          action VARCHAR(40) NOT NULL,
          action_result VARCHAR(40) NOT NULL,
          comment VARCHAR(1000) NULL,
          operator_user_id BIGINT NULL,
          operator_name VARCHAR(120) NULL,
          operated_at TIMESTAMP NOT NULL,
          created_at TIMESTAMP NULL,
          updated_at TIMESTAMP NULL,
          created_by BIGINT NULL,
          updated_by BIGINT NULL,
          deleted INT NOT NULL DEFAULT 0
        )
        """);
    authenticate(1001L, "business-user");
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
    TenantContextHolder.clear();
  }

  @Test
  void flowableProcessRunsRejectionResubmissionReviewAndArchive() {
    CertificationTaskDocumentEntity document = document();

    String firstProcessId = workflowService.startReview(document);
    document.setProcessInstanceId(firstProcessId);
    document.setStatus("initial_reviewing");
    assertThat(workflowService.getWorkflow(document).currentNodeKey()).isEqualTo("initialReviewTask");

    authenticate(2001L, "supervisor");
    workflowService.completeInitialReview(document, true, "initial ok");
    document.setStatus("final_reviewing");
    assertThat(workflowService.getWorkflow(document).currentNodeKey()).isEqualTo("finalReviewTask");

    workflowService.completeFinalReview(document, false, "needs more evidence");
    document.setStatus("final_rejected");
    assertThat(workflowService.getWorkflow(document).currentNodeKey()).isNull();

    authenticate(1001L, "business-user");
    document.setStatus("draft");
    String secondProcessId = workflowService.startReview(document);
    document.setProcessInstanceId(secondProcessId);
    document.setStatus("initial_reviewing");
    assertThat(secondProcessId).isNotEqualTo(firstProcessId);

    authenticate(2001L, "supervisor");
    workflowService.completeInitialReview(document, true, "initial ok again");
    document.setStatus("final_reviewing");
    workflowService.completeFinalReview(document, true, "final ok");
    document.setStatus("archived");
    workflowService.recordArchive(document, "archived");

    var workflow = workflowService.getWorkflow(document);
    assertThat(workflow.currentNodeKey()).isNull();
    assertThat(workflow.processInstanceId()).isEqualTo(secondProcessId);
    assertThat(workflow.records())
        .extracting(CertificationDocumentWorkflowRecordVO::action)
        .containsExactly(
            "submit",
            "initial_review",
            "final_review",
            "submit",
            "initial_review",
            "final_review",
            "archive"
        );
    assertThat(workflow.records())
        .extracting(CertificationDocumentWorkflowRecordVO::actionResult)
        .containsExactly("submitted", "approved", "rejected", "submitted", "approved", "approved", "archived");
    assertThat(workflow.records())
        .extracting(CertificationDocumentWorkflowRecordVO::processInstanceId)
        .containsExactly(
            firstProcessId,
            firstProcessId,
            firstProcessId,
            secondProcessId,
            secondProcessId,
            secondProcessId,
            secondProcessId
        );
    assertThat(workflow.records())
        .filteredOn(record -> "rejected".equals(record.actionResult()))
        .singleElement()
        .satisfies(record -> assertThat(record.comment()).isEqualTo("needs more evidence"));
  }

  private CertificationTaskDocumentEntity document() {
    CertificationTaskDocumentEntity document = new CertificationTaskDocumentEntity();
    document.setId(990001L);
    document.setTenantId(1L);
    document.setTaskId(990101L);
    document.setDocumentType("charter");
    document.setDocumentName("Project Charter");
    document.setVersionNo(1);
    document.setIsCurrent(1);
    document.setStatus("draft");
    document.setDeleted(0);
    return document;
  }

  private void authenticate(Long userId, String username) {
    PlatformUserPrincipal principal = new PlatformUserPrincipal(
        userId,
        1L,
        username,
        username,
        "admin",
        List.of("PLATFORM_ADMIN"),
        List.of(
            "certification.project.document.submit",
            "certification.project.document.initialReview",
            "certification.project.document.finalReview",
            "certification.project.document.workflowView"
        )
    );
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(principal, "token", principal.getAuthorities())
    );
  }
}
