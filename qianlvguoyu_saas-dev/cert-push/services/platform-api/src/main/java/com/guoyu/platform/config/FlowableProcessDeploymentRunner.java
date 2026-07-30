package com.guoyu.platform.config;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

/**
 * 受控部署 Flowable 流程定义。
 * <p>
 * Spring Boot 默认 auto-deploy 在流程版本已存在时可能触发 ACT_UNIQ_PROCDEF 冲突；
 * 此处仅在 BPMN 内容变更时部署新版本。
 */
@Component
@Slf4j
public class FlowableProcessDeploymentRunner {

  static final String PROCESS_KEY = "certificationDocumentReview";
  static final String BPMN_CLASSPATH = "workflows/bpmn/certification-document-review.bpmn20.xml";
  static final String BPMN_RESOURCE_NAME = "certification-document-review.bpmn20.xml";

  private final RepositoryService repositoryService;

  public FlowableProcessDeploymentRunner(RepositoryService repositoryService) {
    this.repositoryService = repositoryService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void deployIfChanged() {
    byte[] bpmnBytes = readClasspathBytes(BPMN_CLASSPATH);
    String checksum = sha256Hex(bpmnBytes);

    ProcessDefinition latest = repositoryService.createProcessDefinitionQuery()
        .processDefinitionKey(PROCESS_KEY)
        .latestVersion()
        .singleResult();

    if (latest != null && checksum.equals(readDeploymentChecksum(latest.getDeploymentId()))) {
      log.info("Flowable process {} unchanged (v{}), skip deployment", PROCESS_KEY, latest.getVersion());
      return;
    }

    Deployment deployment = repositoryService.createDeployment()
        .name("auto-" + PROCESS_KEY)
        .addBytes(BPMN_RESOURCE_NAME, bpmnBytes)
        .deploy();

    ProcessDefinition deployed = repositoryService.createProcessDefinitionQuery()
        .deploymentId(deployment.getId())
        .singleResult();

    if (deployed != null) {
      log.info("Flowable process {} deployed as v{} (deploymentId={})",
          PROCESS_KEY, deployed.getVersion(), deployment.getId());
    } else {
      log.warn("Flowable deployment {} finished without process definition", deployment.getId());
    }
  }

  private String readDeploymentChecksum(String deploymentId) {
    List<String> resourceNames = repositoryService.getDeploymentResourceNames(deploymentId);
    for (String resourceName : resourceNames) {
      if (!resourceName.endsWith(".bpmn20.xml") && !resourceName.endsWith(".bpmn")) {
        continue;
      }
      try (InputStream in = repositoryService.getResourceAsStream(deploymentId, resourceName)) {
        if (in != null) {
          return sha256Hex(StreamUtils.copyToByteArray(in));
        }
      } catch (IOException ex) {
        log.warn("Failed to read Flowable deployment resource {}: {}", resourceName, ex.getMessage());
      }
    }
    return "";
  }

  private static byte[] readClasspathBytes(String classpath) {
    ClassPathResource resource = new ClassPathResource(classpath);
    try (InputStream in = resource.getInputStream()) {
      return StreamUtils.copyToByteArray(in);
    } catch (IOException ex) {
      throw new IllegalStateException("Missing Flowable BPMN resource: " + classpath, ex);
    }
  }

  private static String sha256Hex(byte[] bytes) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }
}
