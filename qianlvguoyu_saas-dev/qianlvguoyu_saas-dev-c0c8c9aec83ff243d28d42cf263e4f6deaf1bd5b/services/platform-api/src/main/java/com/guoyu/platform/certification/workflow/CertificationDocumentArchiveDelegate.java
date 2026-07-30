package com.guoyu.platform.certification.workflow;

import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("certificationDocumentArchiveDelegate")
public class CertificationDocumentArchiveDelegate implements JavaDelegate {

  @Override
  public void execute(DelegateExecution execution) {
    // Business archiving is synchronized by CertificationProjectApplicationService
    // after Flowable completes this service task.
  }
}
