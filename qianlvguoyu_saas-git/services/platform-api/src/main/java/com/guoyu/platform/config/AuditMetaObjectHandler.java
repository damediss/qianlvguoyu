package com.guoyu.platform.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.guoyu.platform.security.SecurityContext;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

@Component
public class AuditMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictInsertFill(metaObject, "createdBy", Long.class, currentUserId());
        strictInsertFill(metaObject, "updatedBy", Long.class, currentUserId());
        strictInsertFill(metaObject, "tenantId", Long.class, currentTenantId());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
        strictUpdateFill(metaObject, "updatedBy", Long.class, currentUserId());
    }

    private Long currentUserId() {
        try {
            return SecurityContext.currentUser().getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private Long currentTenantId() {
        try {
            return SecurityContext.currentUser().getTenantId();
        } catch (Exception e) {
            return null;
        }
    }
}
