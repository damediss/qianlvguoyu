package com.guoyu.platform.knowledge.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.guoyu.platform.common.domain.BaseEntity;

@TableName("kb_document_acl")
public class KnowledgeDocumentAclEntity extends BaseEntity {

  private Long documentId;
  private String principalType;
  private String principalValue;

  public Long getDocumentId() {
    return documentId;
  }

  public void setDocumentId(Long documentId) {
    this.documentId = documentId;
  }

  public String getPrincipalType() {
    return principalType;
  }

  public void setPrincipalType(String principalType) {
    this.principalType = principalType;
  }

  public String getPrincipalValue() {
    return principalValue;
  }

  public void setPrincipalValue(String principalValue) {
    this.principalValue = principalValue;
  }
}
