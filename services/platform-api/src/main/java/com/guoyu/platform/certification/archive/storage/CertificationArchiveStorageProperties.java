package com.guoyu.platform.certification.archive.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "certification.archive")
public class CertificationArchiveStorageProperties {
  private String storageType = "local";
  private String storageRoot = "D:/qianlvguoyu/storage";

  public String getStorageType() {
    return storageType;
  }

  public void setStorageType(String storageType) {
    this.storageType = storageType;
  }

  public String getStorageRoot() {
    return storageRoot;
  }

  public void setStorageRoot(String storageRoot) {
    this.storageRoot = storageRoot;
  }
}
