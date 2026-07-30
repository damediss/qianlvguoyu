# 园区学院 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the SaaS “工业互联网平台 / 园区学院” module with tenant-isolated CRUD, statistics, menu permissions, React page, and tests.

**Architecture:** Add an independent `industrial` backend package under `platform-api`, backed by a new `industrial_college` table and Flyway menu/permission seed data. Add a focused React page under `apps/admin-web/src/pages/industrial`, using existing `apiClient`, Ant Design, TanStack Query, and current page conventions.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, React 18, TypeScript, Ant Design, TanStack Query, Zustand.

---

## File Structure

Backend files:

- Create `services/platform-api/src/main/resources/db/migration/V20260526_001__create_industrial_college.sql`: table schema.
- Create `services/platform-api/src/main/resources/db/migration/V20260526_002__seed_industrial_college_permissions.sql`: menu, permissions, demo admin grants.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/entity/IndustrialCollegeEntity.java`: MyBatis-Plus entity.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/mapper/IndustrialCollegeMapper.java`: mapper.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/dto/IndustrialCollegeQueryReq.java`: pagination and filters.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/dto/IndustrialCollegeCreateReq.java`: create payload and validation.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/dto/IndustrialCollegeUpdateReq.java`: update payload and validation.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/vo/IndustrialCollegeVO.java`: detail/list response.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/vo/IndustrialCollegeStatsVO.java`: statistics response.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/service/IndustrialCollegeApplicationService.java`: application service.
- Create `services/platform-api/src/main/java/com/guoyu/platform/industrial/controller/IndustrialCollegeController.java`: REST controller.
- Create `services/platform-api/src/test/java/com/guoyu/platform/industrial/service/IndustrialCollegeApplicationServiceTest.java`: service tests.

Frontend files:

- Create `apps/admin-web/src/types/industrial.ts`: TypeScript types.
- Create `apps/admin-web/src/api/industrial.ts`: API functions.
- Create `apps/admin-web/src/pages/industrial/CollegePage.tsx`: page.
- Modify `apps/admin-web/src/layouts/AppShell.tsx`: add component mapping for `industrial/College` if needed.
- Modify `apps/admin-web/src/components/AuthGate.tsx`: fix current React Query build error.
- Modify `apps/admin-web/src/directives/permission.tsx`: fix invalid React imports or replace with a valid helper.

---

### Task 1: Fix Current Frontend Build Blockers

**Files:**
- Modify: `apps/admin-web/src/components/AuthGate.tsx`
- Modify: `apps/admin-web/src/directives/permission.tsx`

- [ ] **Step 1: Run current frontend build and confirm blocker**

Run in `apps/admin-web`:

```powershell
npm run build
```

Expected: FAIL with errors in `AuthGate.tsx` and `permission.tsx`.

- [ ] **Step 2: Fix `AuthGate.tsx` query function**

Use `getCurrentUser` from `apps/admin-web/src/api/auth.ts` as the `queryFn`, not the current user object. Keep the existing behavior: local token hydrates UI first, `/auth/me` verifies in background, 401 clears auth in the interceptor.

The query should have this shape:

```tsx
const meQuery = useQuery({
  queryKey: ['auth', 'me', token],
  queryFn: getCurrentUser,
  enabled: Boolean(token),
  retry: false,
});
```

When `meQuery.data` exists, call `setCurrentUser(meQuery.data)`.

- [ ] **Step 3: Fix `permission.tsx`**

React has no `Directive` or `DirectiveBinding` exports. Replace the Vue-style directive with plain helpers:

```tsx
import { useAuthStore } from '../stores/authStore';

export function hasPermission(permission?: string): boolean {
  if (!permission) {
    return true;
  }
  const currentUser = useAuthStore.getState().currentUser;
  return Boolean(currentUser?.permissions?.includes(permission));
}

export function PermissionGuard({
  permission,
  children,
}: {
  permission?: string;
  children: React.ReactNode;
}) {
  if (!hasPermission(permission)) {
    return null;
  }
  return <>{children}</>;
}
```

- [ ] **Step 4: Verify frontend build passes the old blockers**

Run:

```powershell
npm run build
```

Expected: the previous `AuthGate.tsx` and `permission.tsx` errors are gone. If new unrelated TypeScript errors appear, fix them before continuing.

---

### Task 2: Write Failing Backend Service Tests

**Files:**
- Create: `services/platform-api/src/test/java/com/guoyu/platform/industrial/service/IndustrialCollegeApplicationServiceTest.java`

- [ ] **Step 1: Add tests for create, update, delete, stats, and tenant isolation**

Use Mockito-style unit tests following existing service tests. The tests should prove:

- `create` fills `tenantId`, `deleted=0`, default `status=enabled`, default `sortOrder=1`.
- `page` filters by current tenant and ignores deleted rows.
- `getById` refuses records outside current tenant.
- `update` only changes allowed fields.
- `delete` performs logical delete.
- `stats` returns total count, type counts, and recent count.

- [ ] **Step 2: Run the test and verify RED**

Run:

```powershell
C:\Users\pc\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd -f services/platform-api/pom.xml -Dtest=IndustrialCollegeApplicationServiceTest test
```

Expected: FAIL because `IndustrialCollegeApplicationService` and related classes do not exist.

---

### Task 3: Add Database Migrations

**Files:**
- Create: `services/platform-api/src/main/resources/db/migration/V20260526_001__create_industrial_college.sql`
- Create: `services/platform-api/src/main/resources/db/migration/V20260526_002__seed_industrial_college_permissions.sql`

- [ ] **Step 1: Create `industrial_college` table migration**

Create table with all SaaS audit and tenant fields from the design. Use `IF NOT EXISTS` and `utf8mb4`.

- [ ] **Step 2: Create menu and permission migration**

Seed menu IDs in a range not used by existing migrations, for example:

- `5100`: 工业互联网平台 directory
- `5101`: 园区学院 page
- `5700` to `5703`: view/create/update/delete permissions

Grant all to demo platform admin role `2000` for tenant `1`.

- [ ] **Step 3: Run backend compile**

Run:

```powershell
C:\Users\pc\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd -f services/platform-api/pom.xml -DskipTests compile
```

Expected: BUILD SUCCESS.

---

### Task 4: Implement Backend Industrial Module

**Files:**
- Create all backend Java files listed in File Structure.

- [ ] **Step 1: Implement entity and mapper**

`IndustrialCollegeEntity` maps to `industrial_college`, uses `@TableName`, `@TableId`, and fields matching the migration.

- [ ] **Step 2: Implement DTO and VO classes**

Create request DTOs with Jakarta validation annotations:

```java
@NotBlank(message = "机构名称不能为空")
@Size(max = 128, message = "机构名称不能超过128个字符")
private String name;
```

Use Java records for VO classes where consistent with existing code; use Lombok DTOs where existing modules do.

- [ ] **Step 3: Implement application service**

Use `SecurityContext.currentUser().getTenantId()` and `getUserId()`.

Use MyBatis-Plus `LambdaQueryWrapper` for tenant filtering:

```java
new LambdaQueryWrapper<IndustrialCollegeEntity>()
  .eq(IndustrialCollegeEntity::getTenantId, tenantId)
  .eq(IndustrialCollegeEntity::getDeleted, 0)
```

The `stats` method returns:

- total count in current tenant.
- map/list grouped by `tag`.
- records created in the last 30 days.

- [ ] **Step 4: Implement controller**

Use:

```java
@RestController
@RequestMapping("/industrial/college")
```

Add `@PreAuthorize` permissions and `@AuditLog` on write operations.

- [ ] **Step 5: Run service test and verify GREEN**

Run:

```powershell
C:\Users\pc\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd -f services/platform-api/pom.xml -Dtest=IndustrialCollegeApplicationServiceTest test
```

Expected: PASS.

---

### Task 5: Implement Frontend API and Page

**Files:**
- Create: `apps/admin-web/src/types/industrial.ts`
- Create: `apps/admin-web/src/api/industrial.ts`
- Create: `apps/admin-web/src/pages/industrial/CollegePage.tsx`
- Modify: `apps/admin-web/src/layouts/AppShell.tsx`

- [ ] **Step 1: Add TypeScript types**

Types must mirror backend fields:

```ts
export interface IndustrialCollege {
  id: number;
  name: string;
  tag: string;
  businessScope?: string;
  description?: string;
  trainingQualification?: string;
  certificationQualification?: string;
  address?: string;
  phone?: string;
  status: 'enabled' | 'disabled';
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
}
```

- [ ] **Step 2: Add API functions**

Use `apiClient` and `unwrap`:

```ts
export function pageIndustrialColleges(params: IndustrialCollegeQuery) {
  return unwrap<PageResponse<IndustrialCollege>>(
    apiClient.get('/industrial/college', { params }),
  );
}
```

Add `get`, `stats`, `create`, `update`, and `delete` functions.

- [ ] **Step 3: Add College page**

Use Ant Design components:

- `Card` or small statistic blocks for totals.
- `Input`, `Select`, `Button` for filters.
- `Table` for records.
- `Modal` for create/edit.
- `Drawer` for details.

Operations:

- `新增院校` opens empty form.
- `详情` opens read-only drawer.
- `编辑` fills form from selected record.
- `删除` uses `Modal.confirm`.

- [ ] **Step 4: Add page mapping**

Find the menu component map in `AppShell.tsx` and add:

```tsx
'industrial/College': <CollegePage />,
```

If the map uses lazy imports or route objects, follow that local pattern.

- [ ] **Step 5: Run frontend build**

Run in `apps/admin-web`:

```powershell
npm run build
```

Expected: PASS.

---

### Task 6: Final Verification

**Files:**
- No new files unless verification exposes issues.

- [ ] **Step 1: Run backend tests**

Run:

```powershell
C:\Users\pc\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd -f services/platform-api/pom.xml test
```

Expected: BUILD SUCCESS.

- [ ] **Step 2: Run frontend build**

Run in `apps/admin-web`:

```powershell
npm run build
```

Expected: build succeeds and produces `dist`.

- [ ] **Step 3: Summarize changed files and any residual risk**

Mention:

- Design doc and implementation plan paths.
- Backend module and migrations.
- Frontend page and API.
- Verification commands and results.

