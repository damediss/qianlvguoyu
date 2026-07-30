# Certification Document Workflow Design

Date: 2026-06-01
Status: Design approved by user for planning

## Goal

Implement the core data workflow for the certification project management module.

Each project document type has an independent approval workflow:

`business department edits -> submit -> supervisor initial review -> supervisor final review -> archive`

Initial review rejection and final review rejection both return the document to the business department for revision. After revision, the business department submits again and starts a new review round.

## Confirmed Decisions

- Use option 1: embed Flowable inside `services/platform-api`.
- Do not build or deploy a separate `workflow-service` in this phase.
- One Flowable process instance is created per project document type, not per project task.
- The workflow binds to `certification_task_document.process_instance_id`.
- The five existing document types are in scope: `charter`, `members`, `wbs`, `schedule`, and `risk`.
- Use two human review nodes: supervisor initial review and supervisor final review.
- The same user may complete both initial review and final review.
- Flowable is the source of truth for workflow task progression.
- Business tables remain the source of truth for business-facing document and project status.
- n8n is not used for core workflow status. It may later handle notifications or scheduled automation.

## Current Codebase Context

The project management module already has the core project and document tables:

- `certification_task`
- `certification_task_phase`
- `certification_task_document`
- document detail tables for members, WBS, schedule, and risks

`certification_task_document` already includes fields needed for workflow integration:

- `status`
- `submitted_at`
- `submitted_by`
- `approved_at`
- `approved_by`
- `rejected_at`
- `rejected_by`
- `reject_reason`
- `process_instance_id`

The current submit behavior only changes local business status. Flowable is not yet integrated:

- `services/platform-api/pom.xml` has no Flowable dependency.
- `application.yml` has no Flowable configuration.
- Java code has no `RuntimeService`, `TaskService`, or process engine usage.
- `workflows/bpmn/` is reserved by documentation but has no deployed BPMN flow.

Authentication is currently implemented with Sa-Token tokens bridged into Spring Security, with JWT-related configuration already present. Workflow endpoints should depend on the existing Spring Security principal and `@PreAuthorize` permissions. If token implementation changes to JWT later, workflow business code should not need to change.

## Architecture

Flowable will be integrated into `platform-api` for the first implementation.

Package boundary:

- `com.guoyu.platform.certification.workflow`

New classes:

- `CertificationDocumentWorkflowService`
  - starts document review processes
  - completes initial review tasks
  - completes final review tasks
  - reads current tasks and workflow history
  - synchronizes Flowable task results into business tables
- `CertificationDocumentWorkflowRecordEntity`
  - stores business-facing workflow audit records
- `CertificationDocumentWorkflowRecordMapper`
  - persists workflow records

`CertificationProjectApplicationService` should keep ownership of project document use cases and delegate workflow operations to `CertificationDocumentWorkflowService`.

This keeps the public API and business transaction boundary in the existing certification module while isolating Flowable details behind a small service. If workflow capabilities are later moved to `workflow-service`, this boundary can become the service client boundary.

## BPMN Design

Use one reusable BPMN process definition:

`certificationDocumentReview`

Process variables:

- `tenantId`
- `taskId`
- `documentId`
- `documentType`
- `submitterUserId`
- `submitterName`
- `initialReviewerUserId`
- `finalReviewerUserId`
- `initialReviewApproved`
- `finalReviewApproved`
- `reviewComment`

Flow:

1. Start event
2. Supervisor initial review user task
3. Initial review exclusive gateway
4. Supervisor final review user task
5. Final review exclusive gateway
6. Archive service task
7. End event

Initial review result:

- Approved: move to final review.
- Rejected: update business document to `initial_rejected`, record opinion, end this process instance.

Final review result:

- Approved: run archive service task, update business document to `archived`, end process.
- Rejected: update business document to `final_rejected`, record opinion, end this process instance.

Rejected documents are edited in business pages and submitted again. Re-submission starts a new Flowable process instance and replaces `process_instance_id` with the current review round. Historical records remain in the business workflow record table and Flowable history tables.

## Document Statuses

Use these business-facing document statuses:

- `draft`: business department editing
- `submitted`: submitted and process started
- `initial_reviewing`: waiting for or undergoing supervisor initial review
- `initial_rejected`: initial review rejected, waiting for business revision
- `final_reviewing`: waiting for or undergoing supervisor final review
- `final_rejected`: final review rejected, waiting for business revision
- `archived`: final review approved and archived

Status rules:

- `draft`, `initial_rejected`, and `final_rejected` are editable.
- `submitted`, `initial_reviewing`, `final_reviewing`, and `archived` are not editable.
- Only editable statuses can be submitted.
- Rejected documents must be saved before re-submission.
- Rejection requires a non-empty comment.
- Archiving is triggered only by final review approval.
- No public front-end API may bypass review and archive directly.

## Project Status Aggregation

`certification_task.status` remains a project-level aggregate display status.

Aggregation rules:

- If any current document is in `initial_reviewing`, `final_reviewing`, or `submitted`, project status is `pending_review`.
- Else if any current document is `initial_rejected` or `final_rejected`, project status is `rejected`.
- Else if all five current documents are `archived`, project status is `completed`.
- Else project status is `running`.

Document status remains the detailed workflow status. Project status should not replace document status.

## API Design

Extend the existing document route:

Base path:

`/certification/projects/tasks/{taskId}/documents/{documentType}`

Endpoints:

- `POST /submit`
  - starts a new Flowable process instance
  - sets document status to `submitted` and then `initial_reviewing`
  - stores `process_instance_id`
  - records submitter and timestamp

- `POST /initial-review`
  - request body: `{ "approved": true|false, "comment": "..." }`
  - completes the current initial review Flowable task
  - approved result sets document to `final_reviewing`
  - rejected result sets document to `initial_rejected`

- `POST /final-review`
  - request body: `{ "approved": true|false, "comment": "..." }`
  - completes the current final review Flowable task
  - approved result archives the document through the service task and sets document to `archived`
  - rejected result sets document to `final_rejected`

- `GET /workflow`
  - returns current workflow state, current pending node, and business-facing history records

Existing `PUT` save endpoint remains responsible for editing document content and detail tables.

## Permissions

Use Spring Security method authorization through existing token authentication.

New permissions:

- `certification.project.document.submit`
- `certification.project.document.initialReview`
- `certification.project.document.finalReview`
- `certification.project.document.workflowView`

The archive step is a backend service task and should not be exposed as a normal front-end permission.

Existing document-specific permissions such as `certification.project.charter.submit` can remain for backward compatibility on old buttons or routes. New workflow endpoints use the document workflow permissions listed above.

Candidate groups:

- Initial review: department supervisor group or users with `certification.project.document.initialReview`
- Final review: department supervisor group or users with `certification.project.document.finalReview`

The first phase validates permissions at API entry and completes the Flowable task as the current authenticated user. Flowable candidate group names are stored on the BPMN tasks, but business authorization is enforced by Spring Security permissions. More detailed organization-based assignment is outside this phase.

## Data Model Additions

Keep `certification_task_document` as the current document state table.

Add a business-facing workflow record table:

`certification_document_workflow_record`

Suggested fields:

- `id`
- `tenant_id`
- `task_id`
- `document_id`
- `document_type`
- `process_instance_id`
- `task_id_in_engine`
- `node_key`
- `node_name`
- `action`
- `action_result`
- `comment`
- `operator_user_id`
- `operator_name`
- `operated_at`
- `created_at`
- `updated_at`
- `created_by`
- `updated_by`
- `deleted`

Example action values:

- `submit`
- `initial_review`
- `final_review`
- `archive`

Example result values:

- `submitted`
- `approved`
- `rejected`
- `archived`

Flowable runtime and history tables remain available for engine-level diagnostics, but front-end workflow history should read the business record table.

## Front-End Design

Each document toolbar should show actions based on the current document status and the current user's permissions:

- `draft`, `initial_rejected`, `final_rejected`
  - show `Submit for Review`
- `initial_reviewing`
  - users with initial review permission see `Initial Review Approve` and `Initial Review Reject`
- `final_reviewing`
  - users with final review permission see `Final Review Approve` and `Final Review Reject`
- `archived`
  - show read-only `Archived`
- all states
  - show `Workflow Records`

Editing controls should be disabled for non-editable statuses.

The current project workbench should continue to show document progress, but status tags must be updated to display the new workflow states.

## Error Handling

Return business errors when:

- document does not exist
- document type is unsupported
- submit is requested from a non-editable status
- review is requested but no matching Flowable user task exists
- current user lacks the required permission
- rejection comment is empty
- final review approval fails to archive the business document

All write operations should remain transactional where possible. If Flowable and business table updates cannot be committed in a single local transaction, the service must fail loudly and leave enough audit data to diagnose the mismatch.

## Testing Strategy

Backend tests:

- submit from `draft` starts a Flowable process and sets document to `initial_reviewing`
- submit from `archived` fails
- initial review approval sets document to `final_reviewing`
- initial review rejection sets document to `initial_rejected` and records comment
- final review approval sets document to `archived` and refreshes project aggregate status
- final review rejection sets document to `final_rejected` and records comment
- rejection without comment fails
- users without review permission cannot review
- five archived documents aggregate project status to `completed`

Frontend tests:

- editable statuses show submit and edit controls
- review statuses disable edit controls
- initial review permission reveals initial review buttons
- final review permission reveals final review buttons
- archived status is read-only

Migration checks:

- Flowable tables can be created on an empty database
- new workflow record table migrates successfully
- existing certification project seed data still loads

## Out Of Scope

- Independent deployment of `workflow-service`
- n8n notification flows
- complex organization-based assignment rules
- forced separation between initial reviewer and final reviewer
- full project lifecycle workflow beyond project document approval
- manual archive bypass from the front end
