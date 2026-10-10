import crypto from 'node:crypto';
import bcrypt from 'bcryptjs';

export function sha256Hex(value) {
  return crypto.createHash('sha256').update(String(value), 'utf8').digest('hex');
}

export function bcryptHash(plain, rounds = 10) {
  return bcrypt.hashSync(plain, rounds);
}

export function idCardCheckCode(body17) {
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
  const codes = ['1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'];
  let sum = 0;
  for (let i = 0; i < 17; i += 1) {
    sum += Number(body17[i]) * weights[i];
  }
  return codes[sum % 11];
}

export function syntheticIdCard(index) {
  const seq = String(Math.max(1, index)).padStart(4, '0').slice(-4);
  const body17 = `1101011990010${seq}`;
  return `${body17}${idCardCheckCode(body17)}`;
}

export function syntheticMobile(index) {
  return `199${String(100000000 + index).slice(-8)}`;
}

export function fullJitter(maxMs, random = Math.random) {
  return Math.floor(random() * Math.max(0, maxMs));
}
