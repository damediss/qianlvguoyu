export const BLOCKED_FRAGMENTS = ['43.138.163.7', 'gjjddb2026', 'gjjddbpro'];
export const REQUIRED_SCHEMA = 'gjjddb_load';
export const ISOLATED_TOKEN = 'I_UNDERSTAND';

export function normalizeBaseUrl(raw) {
  const trimmed = (raw || 'http://127.0.0.1:18080/api').replace(/\/$/, '');
  if (trimmed.endsWith('/api')) return trimmed;
  return `${trimmed}/api`;
}

export function schemaName(jdbcUrl) {
  if (!jdbcUrl) return '';
  const scheme = jdbcUrl.indexOf('://');
  const rest = scheme >= 0 ? jdbcUrl.slice(scheme + 3) : jdbcUrl;
  const slash = rest.indexOf('/');
  if (slash < 0) return '';
  const after = rest.slice(slash + 1);
  const query = after.indexOf('?');
  return (query >= 0 ? after.slice(0, query) : after).split('/')[0].trim();
}

export function hostName(jdbcUrl) {
  if (!jdbcUrl) return '';
  const scheme = jdbcUrl.indexOf('://');
  let rest = scheme >= 0 ? jdbcUrl.slice(scheme + 3) : jdbcUrl;
  const slash = rest.indexOf('/');
  rest = slash >= 0 ? rest.slice(0, slash) : rest;
  const at = rest.lastIndexOf('@');
  if (at >= 0) rest = rest.slice(at + 1);
  if (!rest.startsWith('[') && rest.includes(':')) {
    rest = rest.slice(0, rest.lastIndexOf(':'));
  }
  return rest.trim();
}

export function sanitizeJdbcUrl(jdbcUrl) {
  if (!jdbcUrl) return '';
  return jdbcUrl.replace(/(password=)[^&;]*/ig, '$1***');
}

export function containsBlockedFragment(value) {
  if (!value) return false;
  const lower = String(value).toLowerCase();
  return BLOCKED_FRAGMENTS.some((fragment) => lower.includes(fragment.toLowerCase()));
}

export function confirmations(env = process.env) {
  return {
    allowLarge: env.FORMAL_EXAM_LOAD_ALLOW_LARGE === '1',
    isolated: env.FORMAL_EXAM_LOAD_ISOLATED === ISOLATED_TOKEN,
  };
}

export function evaluateIsolation(input) {
  const reasons = [];
  const jdbcUrl = input.jdbcUrl || '';
  const schema = input.jdbcSchema || schemaName(jdbcUrl);
  const host = input.jdbcHost || hostName(jdbcUrl);
  const profiles = String(input.profiles || '');
  const confirm = confirmations(input.env || process.env);
  const writeOrLarge = Boolean(input.writeAnswers) || Number(input.pads) > 10;

  if (!jdbcUrl) reasons.push('missing process JDBC URL');
  if (containsBlockedFragment(jdbcUrl) || containsBlockedFragment(host) || containsBlockedFragment(schema)) {
    reasons.push('JDBC still contains a blocked shared/prod fragment');
  }
  if (schema.toLowerCase() !== REQUIRED_SCHEMA) {
    reasons.push(`schema must be ${REQUIRED_SCHEMA}, got ${schema || '(empty)'}`);
  }
  if (!profiles.split(/[,\s]+/).includes('loadtest')) {
    reasons.push('API profile is not loadtest');
  }
  if (writeOrLarge && !confirm.allowLarge) {
    reasons.push('FORMAL_EXAM_LOAD_ALLOW_LARGE=1 is required for pads>10 or writes');
  }
  if (writeOrLarge && !confirm.isolated) {
    reasons.push('FORMAL_EXAM_LOAD_ISOLATED=I_UNDERSTAND is required for pads>10 or writes');
  }
  if (writeOrLarge && !input.isolatedInfra) {
    reasons.push('MySQL/Redis/RabbitMQ were not proven to be the isolated compose instances');
  }

  return {
    ok: reasons.length === 0,
    writeOrLarge,
    jdbcHost: host,
    jdbcSchema: schema,
    jdbcUrlSanitized: sanitizeJdbcUrl(jdbcUrl),
    profiles,
    pads: Number(input.pads) || 0,
    writeAnswers: Boolean(input.writeAnswers),
    confirm,
    reasons,
  };
}

export function assertSeedMysql(config) {
  const url = `jdbc:mysql://${config.host}:${config.port}/${config.database}`;
  if (containsBlockedFragment(url) || containsBlockedFragment(config.host) || containsBlockedFragment(config.database)) {
    throw new Error(`seed refused blocked MySQL target ${sanitizeJdbcUrl(url)}`);
  }
  if (String(config.database).toLowerCase() !== REQUIRED_SCHEMA) {
    throw new Error(`seed refused schema ${config.database}; required ${REQUIRED_SCHEMA}`);
  }
}
