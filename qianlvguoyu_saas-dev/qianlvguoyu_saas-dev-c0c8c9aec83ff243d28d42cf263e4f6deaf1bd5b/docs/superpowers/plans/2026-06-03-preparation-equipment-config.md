# Preparation Equipment Config Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add preparation task equipment configuration linked to information-management equipment statuses.

**Architecture:** Reuse the existing certification information equipment table and APIs, adding only the equipment quantity field and status semantics. Add a task-side `certification_task_equipment` table and preparation APIs that bind/unbind approved idle equipment while writing back master equipment status.

**Tech Stack:** Spring Boot, MyBatis-Plus, Flyway, React, Ant Design, TanStack Query, TypeScript.

---

### Task 1: Backend Data Model

**Files:**
- Create: `services/platform-api/src/main/resources/db/migration/V20260603_001__add_preparation_equipment_config.sql`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/entity/CertificationInfoExamEquipmentEntity.java`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/dto/ExamEquipmentInfoSaveReq.java`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/vo/ExamEquipmentInfoVO.java`
- Create: `services/platform-api/src/main/java/com/guoyu/platform/certification/entity/CertificationTaskEquipmentEntity.java`
- Create: `services/platform-api/src/main/java/com/guoyu/platform/certification/mapper/CertificationTaskEquipmentMapper.java`
- Create: `services/platform-api/src/main/java/com/guoyu/platform/certification/dto/PreparationTaskEquipmentSaveReq.java`
- Create: `services/platform-api/src/main/java/com/guoyu/platform/certification/vo/PreparationTaskEquipmentVO.java`

- [ ] Add Flyway migration with `equipment_quantity INT NOT NULL DEFAULT 1`, normalize old `normal` equipment rows to `idle`, create `certification_task_equipment`, and seed preparation equipment permissions.
- [ ] Add Java entity, DTO, and VO fields for equipment quantity.
- [ ] Add task equipment entity, mapper, save request, and VO.

### Task 2: Backend Behavior

**Files:**
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/application/CertificationInfoApplicationService.java`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/service/CertificationPreparationApplicationService.java`
- Modify: `services/platform-api/src/main/java/com/guoyu/platform/certification/controller/CertificationPreparationController.java`

- [ ] Change equipment review pass and enable flows to set `equipmentStatus = idle`.
- [ ] Change available equipment filtering to `auditStatus = approved` and `equipmentStatus = idle`.
- [ ] Inject `CertificationInfoExamEquipmentMapper` and `CertificationTaskEquipmentMapper` into preparation service.
- [ ] Add list, assign, and remove task equipment methods.
- [ ] Add preparation controller endpoints for task equipment.
- [ ] Recalculate referenced count and master status after assign/remove.

### Task 3: Backend Tests

**Files:**
- Create or modify focused tests under `services/platform-api/src/test/java/com/guoyu/platform/certification/`

- [ ] Write failing tests for equipment review status, available list filtering, assign status writeback, and remove release behavior.
- [ ] Run the focused tests and verify they fail for missing behavior.
- [ ] Implement backend behavior.
- [ ] Run focused tests again and verify they pass.

### Task 4: Frontend API And Types

**Files:**
- Modify: `apps/admin-web/src/types/certificationInfo.ts`
- Modify: `apps/admin-web/src/types/certificationPreparation.ts`
- Modify: `apps/admin-web/src/api/certificationInfo.ts`
- Modify: `apps/admin-web/src/api/certificationPreparation.ts`

- [ ] Add `equipmentQuantity` to information equipment types and payloads.
- [ ] Add preparation task equipment types.
- [ ] Add list, assign, and remove preparation equipment API helpers.

### Task 5: Frontend Pages

**Files:**
- Modify: `apps/admin-web/src/pages/certification/info/ExamEquipmentInfoPage.tsx`
- Modify: `apps/admin-web/src/pages/certification/PreparationPage.tsx`
- Create: `apps/admin-web/src/pages/certification/preparation/EquipmentConfig.tsx`

- [ ] Update information equipment labels to equipment name, number, quantity, and status.
- [ ] Add `EquipmentConfig` matching existing config module patterns.
- [ ] Add a "配置设备" tab to preparation page.
- [ ] Refresh related queries after assign/remove.

### Task 6: Verification

**Files:**
- Read and verify all modified files.

- [ ] Run backend compile or focused tests.
- [ ] Run frontend build or TypeScript check.
- [ ] Report any command that cannot run and why.
