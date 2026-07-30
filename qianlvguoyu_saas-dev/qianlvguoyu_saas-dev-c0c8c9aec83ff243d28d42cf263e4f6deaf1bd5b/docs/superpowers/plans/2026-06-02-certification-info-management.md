# Certification Info Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the certification information management module for assessors, exam sites, and exam equipment, then verify it against the module testing requirements.

**Architecture:** Add a focused `certification.info` backend package with REST controllers, application services, entities, mappers, DTOs, and VOs. Use Flyway to create master-data tables, audit records, dynamic menus, and permissions; register React pages in the existing admin shell and call the new APIs through the existing `apiClient`.

**Tech Stack:** Java 21, Spring Boot 3.5, MyBatis-Plus, Flyway, JUnit 5, Mockito, React 18, TypeScript, Ant Design, TanStack Query, Vite.

---

### Task 1: Backend Behavior Tests

**Files:**
- Create: `services/platform-api/src/test/java/com/guoyu/platform/certification/info/CertificationInfoApplicationServiceTest.java`
- Cover: save draft, submit, reject validation, approve, duplicate blocking, delete protection, available-list filtering, phone masking.

- [ ] **Step 1: Write failing service tests**

Use Mockito mappers and real service methods. Tests should call the wished-for API:
`createAssessor`, `submitAssessor`, `reviewAssessor`, `deleteAssessor`, `listAvailableAssessors`, plus equivalent site and equipment uniqueness paths.

- [ ] **Step 2: Run tests and verify RED**

Run:
`mvn.cmd -pl services/platform-api -Dtest=CertificationInfoApplicationServiceTest test -s scripts/custom-maven-settings.xml`

Expected: compilation or test failures because the new service does not exist yet.

### Task 2: Backend Core Implementation

**Files:**
- Create package: `services/platform-api/src/main/java/com/guoyu/platform/certification/info/`
- Create entities: `entity/CertificationInfoAssessorEntity.java`, `entity/CertificationInfoExamSiteEntity.java`, `entity/CertificationInfoExamEquipmentEntity.java`, `entity/CertificationInfoAuditRecordEntity.java`
- Create mappers: matching `*Mapper.java`
- Create DTOs: query/save/review/disable request classes
- Create VOs: list/detail/audit/available list VOs
- Create service: `application/CertificationInfoApplicationService.java`
- Create controller: `controller/CertificationInfoController.java`

- [ ] **Step 1: Implement minimal entities, DTOs, and mappers**

Use `BaseEntity`, `@TableName`, MyBatis-Plus `BaseMapper`, and existing validation annotations.

- [ ] **Step 2: Implement service methods**

Rules:
- Draft and rejected records can be edited.
- Submit requires materials description length >= 8 and required identifiers present.
- Reject requires comment length >= 6.
- Approved or referenced records cannot be deleted.
- Disable writes audit status or runtime status consistently and records an audit row.
- Available lists only return `approved` and runtime-usable records, excluding expired/scrapped/disabled data.

- [ ] **Step 3: Implement controller permissions**

Use `@PreAuthorize` codes from the requirement document. Add `@AuditLog` on all write endpoints.

- [ ] **Step 4: Run service tests and compile**

Run the same service test command and then:
`mvn.cmd compile -f services/platform-api/pom.xml -s scripts/custom-maven-settings.xml`

### Task 3: Flyway Migration

**Files:**
- Create: `services/platform-api/src/main/resources/db/migration/V20260602_006__create_certification_info_management.sql`

- [ ] **Step 1: Create tables**

Create `certification_assessor`, `certification_exam_site`, `certification_exam_equipment`, and `certification_info_audit_record` with required common fields, indexes, unique keys, English status codes, and idempotent `CREATE TABLE IF NOT EXISTS`.

- [ ] **Step 2: Seed dynamic menus and permissions**

Insert menu group `certification_info` under existing `certification` menu and pages:
`certification-info-assessor`, `certification-info-site`, `certification-info-equipment`.
Insert all required permission codes and grant them to platform admin role `2000` using `INSERT IGNORE`.

- [ ] **Step 3: Verify migration naming and SQL syntax**

Run backend compile/test; inspect migration for duplicate version conflicts.

### Task 4: Frontend Types, API, and Pages

**Files:**
- Create: `apps/admin-web/src/types/certificationInfo.ts`
- Create: `apps/admin-web/src/api/certificationInfo.ts`
- Create: `apps/admin-web/src/pages/certification/info/AssessorInfoPage.tsx`
- Create: `apps/admin-web/src/pages/certification/info/ExamSiteInfoPage.tsx`
- Create: `apps/admin-web/src/pages/certification/info/ExamEquipmentInfoPage.tsx`
- Modify: `apps/admin-web/src/main.tsx`
- Modify: `apps/admin-web/src/layouts/AppShell.tsx`

- [ ] **Step 1: Add TypeScript models and API functions**

Use `apiClient`, `unwrap`, and existing `PageResponse` typing.

- [ ] **Step 2: Add reusable page behavior**

Each page supports list pagination, keyword/status filters, add/edit/view/review modal, submit, approve/reject, disable, delete, and export placeholder action.

- [ ] **Step 3: Register page map and visible shell entries**

Register page keys matching Flyway menu codes. Keep this compatible with existing static shell while backend dynamic menu work remains in progress.

- [ ] **Step 4: Build frontend**

Run:
`npm --prefix apps/admin-web run build`

### Task 5: Verification Against Testing Requirements

**Files:**
- Create: `outputs/certification-info-test-report.md`

- [ ] **Step 1: Run backend tests**

Run targeted unit tests and compile. Record pass/fail and coverage of normal, abnormal, boundary, permission, data, and security cases.

- [ ] **Step 2: Run frontend build**

Record build result and any warnings.

- [ ] **Step 3: Manual acceptance checklist**

Record evidence for menu visibility, create draft, submit, pending edit lock, review pass/reject, available-list filtering, duplicate blocking, delete protection, pagination/filtering, masking, audit records, and page layout.

