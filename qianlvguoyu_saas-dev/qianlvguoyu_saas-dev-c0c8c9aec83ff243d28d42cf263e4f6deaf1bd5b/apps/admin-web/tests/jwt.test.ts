import assert from 'node:assert/strict';
import { describe, it } from 'node:test';
import { isJwtExpired } from '../src/utils/jwt.ts';

function encodeBase64Url(value: unknown): string {
  return Buffer.from(JSON.stringify(value), 'utf8')
    .toString('base64url');
}

function createJwt(payload: unknown): string {
  return [
    encodeBase64Url({ alg: 'none', typ: 'JWT' }),
    encodeBase64Url(payload),
    '',
  ].join('.');
}

describe('isJwtExpired', () => {
  it('does not treat opaque Sa-Token values as expired JWTs', () => {
    assert.equal(isJwtExpired('J0_b1oDkNFhLLpssl_a8mEkcfQe1G9pr9s__'), false);
  });

  it('still detects expired JWTs', () => {
    assert.equal(isJwtExpired(createJwt({ exp: 1 })), true);
  });
});
