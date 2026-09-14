import test from 'node:test';
import assert from 'node:assert/strict';
import { nextAnswerVersion, versionMap } from '../lib/ops.mjs';

test('answer versions increment independently', () => {
  const versions = {};
  versions.q1 = nextAnswerVersion(versions.q1);
  versions.q1 = nextAnswerVersion(versions.q1);
  versions.q2 = nextAnswerVersion(versions.q2);
  assert.equal(versions.q1, 2);
  assert.equal(versions.q2, 1);
  const mapped = versionMap(
    [{ questionSnapshotId: 'q1' }, { questionSnapshotId: 'q2' }],
    versions,
  );
  assert.deepEqual(mapped, [
    { questionSnapshotId: 'q1', version: 2 },
    { questionSnapshotId: 'q2', version: 1 },
  ]);
});
