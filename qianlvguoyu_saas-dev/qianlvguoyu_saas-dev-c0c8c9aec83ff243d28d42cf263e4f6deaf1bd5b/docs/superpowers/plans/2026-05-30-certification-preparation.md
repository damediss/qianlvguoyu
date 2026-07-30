# Certification Preparation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the certification preparation module from the supplied prompt and supporting requirement/database documents.

**Architecture:** Extend the existing certification project module in `services/platform-api` with preparation-specific tables, entities, mappers, controllers, and one application service that owns orchestration and validation. Add an admin-web preparation page under the existing certification menu, using TanStack Query and backend APIs only.

**Tech Stack:** Java 21, Spring Boot 3.5, MyBatis-Plus, Flyway, JUnit 5/Mockito, React, Vite, TypeScript, Ant Design, TanStack Query.

---

### Task 1: Backend Red Tests

**Files:**
- Create: `services/platform-api/src/test/java/com/guoyu/platform/certification/service/CertificationPreparationApplicationServiceTest.java`
- Create: `services/platform-api/src/test/java/com/guoyu/platform/certification/controller/CertificationPreparationControllerPermissionTest.java`
- Create: `services/platform-api/src/test/java/com/guoyu/platform/certification/db/CertificationPreparationMigrationTest.java`

- [ ] Write tests for candidate duplicate validation, document completeness, material generation preconditions, exam-site capacity, assessor validity/work-scope checks, assessor task scoring/status transitions, leader review permission, completion checks, controller route permissions, and migration coverage.
- [ ] Run only the new tests and verify they fail because the preparation classes/migration do not exist.

### Task 2: Database Migration

**Files:**
- Create: `services/platform-api/src/main/resources/db/migration/V20260530_001__create_certification_preparation_tables.sql`

- [ ] Add all required preparation tables, audit columns, tenant columns, indexes, DOC001-DOC010 template seed data, menu seed for `认定筹备期`, and listed permissions.
- [ ] Re-run migration test and verify the migration assertions pass.

### Task 3: Backend Domain and API

**Files:**
- Create preparation Entity, Mapper, DTO, and VO files under `services/platform-api/src/main/java/com/guoyu/platform/certification/`
- Create the seven preparation controllers named in the prompt.
- Create `CertificationPreparationApplicationService.java`

- [ ] Implement read/write APIs for tasks, overview, candidates/import errors, documents/review, materials/generation, exam sites, assessors/groups/members/dispatch, assessor workbench draft/submit/review, complete-check, and complete.
- [ ] Keep API responses under `ApiResponse`, pagination under `PageResponse`, writes annotated with `@AuditLog`, and endpoint permissions under `@PreAuthorize`.
- [ ] Re-run service and permission tests until green.

### Task 4: Frontend API and Types

**Files:**
- Create: `apps/admin-web/src/types/certificationPreparation.ts`
- Create: `apps/admin-web/src/api/certificationPreparation.ts`

- [ ] Add typed DTOs matching backend responses and TanStack Query-ready API functions for every page operation.

### Task 5: Frontend Pages

**Files:**
- Create: `apps/admin-web/src/pages/certification/PreparationPage.tsx`
- Create: `apps/admin-web/src/pages/certification/preparation/*.tsx`
- Modify: `apps/admin-web/src/main.tsx`
- Modify: `apps/admin-web/src/layouts/AppShell.tsx`

- [ ] Add the `认定筹备期` page and menu entry under `认定管理`.
- [ ] Implement the seven requested subviews with searchable tables, modals/forms, status feedback, refresh actions, and no localStorage business data.

### Task 6: Verification

- [ ] Run focused backend tests for the new preparation module.
- [ ] Run the frontend build for `apps/admin-web`.
- [ ] If possible, run the local app and inspect the preparation page.
