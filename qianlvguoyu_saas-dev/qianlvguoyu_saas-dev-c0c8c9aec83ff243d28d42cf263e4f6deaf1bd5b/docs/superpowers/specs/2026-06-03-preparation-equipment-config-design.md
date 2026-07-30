# Preparation Equipment Config Design

## Goal

Add a "configure equipment" module under certification preparation. Equipment is maintained in certification information management, becomes idle after approval, becomes in use when added to a preparation task, and returns to idle when removed from the task if no other active task binding exists.

## Information Management

The existing exam equipment master data is reused. The visible business fields are:

- Equipment name: stored in `equipment_type`
- Equipment number: stored in `asset_no`
- Equipment quantity: stored in new `equipment_quantity`

Equipment operational statuses are:

- `idle`: idle, shown as "空闲中"
- `in_use`: in use, shown as "使用中"
- `disabled`: disabled, toggled by the existing disable/enable actions

When equipment review passes, `audit_status` becomes `approved` and `equipment_status` becomes `idle`. Enabling disabled equipment also returns it to `idle`.

## Preparation Binding

Add a task-side binding table `certification_task_equipment`. Each row links one certification task to one approved information-management equipment record. The binding snapshots equipment name, number, quantity, and remark for display.

Adding equipment validates:

- The task exists.
- The equipment exists in information management.
- The equipment has `audit_status = approved`.
- The equipment has `equipment_status = idle`.
- The same equipment is not already bound to the task.

After successful binding, the information-management equipment status becomes `in_use` and `referenced_count` is recalculated.

Removing equipment deletes the task binding. If no other task binding references that equipment, the information-management equipment status returns to `idle`; otherwise it remains `in_use`.

## Frontend

Certification preparation gets a new tab labelled "配置设备". It follows the same shape as existing exam staff and exam site config:

- A table lists equipment already bound to the selected task.
- An "增加设备" button opens a modal.
- The modal selects from information-management equipment available list.
- The selected task equipment can be removed with a delete action.

Information management exam equipment page changes labels and fields to the new business wording.

## Testing

Backend tests cover:

- Reviewing equipment as passed sets status to `idle`.
- Available equipment only includes approved idle equipment.
- Assigning equipment to a task sets the master record to `in_use`.
- Removing the last binding sets the master record back to `idle`.

Frontend verification covers TypeScript build success for updated API/types/components.
