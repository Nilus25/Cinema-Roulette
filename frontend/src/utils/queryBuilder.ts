import type { FilterMetadata, QueryFlavor } from '../types/api';

export type ChipKind =
  | 'operator'
  | 'not'
  | 'paren'
  | 'genre'
  | 'mediaType'
  | 'comparison'
  | 'text'
  | 'seen'
  | 'unknown';

export interface QueryChip {
  id: string;
  kind: ChipKind;
  raw: string;
}

export interface QueryValidationResult {
  valid: boolean;
  error: string | null;
}

type RawTokenType =
  | 'LPAREN'
  | 'RPAREN'
  | 'AND'
  | 'OR'
  | 'NOT'
  | 'COMP'
  | 'COLON'
  | 'NUMBER'
  | 'STRING'
  | 'WORD'
  | 'EOF';

interface RawToken {
  type: RawTokenType;
  text: string;
  raw: string;
  position: number;
}

const MAX_LENGTH = 1000;
const MAX_TOKENS = 100;
const MAX_DEPTH = 10;

const NUMERIC_FIELDS = new Set([
  'rating',
  'imdb_rating',
  'imdbrating',
  'year',
  'release_year',
  'releaseyear',
  'runtime',
  'runtime_mins',
  'runtimemins',
  'duration',
  'mins',
  'votes',
  'num_votes',
  'numvotes',
]);

const TEXT_FIELDS = new Set(['title', 'director', 'directors', 'keyword', 'keywords', 'text']);

export function normalizeKey(raw: string): string {
  return raw.toLowerCase().replace(/[^a-z0-9]/g, '');
}

export function formatTerm(label: string): string {
  return /\s/.test(label) ? `"${label}"` : label;
}

function tokenizeRaw(input: string): { tokens: RawToken[]; error: string | null } {
  if (input.length > MAX_LENGTH) {
    return { tokens: [], error: `Query exceeds maximum length of ${MAX_LENGTH} characters` };
  }
  const tokens: RawToken[] = [];
  let i = 0;
  const len = input.length;

  while (i < len) {
    const c = input[i];
    if (/\s/.test(c)) {
      i++;
      continue;
    }
    if (tokens.length >= MAX_TOKENS) {
      return { tokens: [], error: `Query exceeds maximum token limit of ${MAX_TOKENS}` };
    }
    if (c === '(') {
      tokens.push({ type: 'LPAREN', text: '(', raw: '(', position: i });
      i++;
    } else if (c === ')') {
      tokens.push({ type: 'RPAREN', text: ')', raw: ')', position: i });
      i++;
    } else if (c === ':') {
      tokens.push({ type: 'COLON', text: ':', raw: ':', position: i });
      i++;
    } else if (c === '<') {
      if (input[i + 1] === '=' || input[i + 1] === '>') {
        const s = input.slice(i, i + 2);
        tokens.push({ type: 'COMP', text: s, raw: s, position: i });
        i += 2;
      } else {
        tokens.push({ type: 'COMP', text: '<', raw: '<', position: i });
        i++;
      }
    } else if (c === '>') {
      if (input[i + 1] === '=') {
        tokens.push({ type: 'COMP', text: '>=', raw: '>=', position: i });
        i += 2;
      } else {
        tokens.push({ type: 'COMP', text: '>', raw: '>', position: i });
        i++;
      }
    } else if (c === '=') {
      if (input[i + 1] === '=') {
        tokens.push({ type: 'COMP', text: '==', raw: '==', position: i });
        i += 2;
      } else {
        tokens.push({ type: 'COMP', text: '=', raw: '=', position: i });
        i++;
      }
    } else if (c === '!') {
      if (input[i + 1] === '=') {
        tokens.push({ type: 'COMP', text: '!=', raw: '!=', position: i });
        i += 2;
      } else {
        tokens.push({ type: 'NOT', text: 'NOT', raw: '!', position: i });
        i++;
      }
    } else if (c === '&' && input[i + 1] === '&') {
      tokens.push({ type: 'AND', text: 'AND', raw: '&&', position: i });
      i += 2;
    } else if (c === '|' && input[i + 1] === '|') {
      tokens.push({ type: 'OR', text: 'OR', raw: '||', position: i });
      i += 2;
    } else if (c === '\u2227') {
      tokens.push({ type: 'AND', text: 'AND', raw: '\u2227', position: i });
      i++;
    } else if (c === '\u2228') {
      tokens.push({ type: 'OR', text: 'OR', raw: '\u2228', position: i });
      i++;
    } else if (c === '\u00AC') {
      tokens.push({ type: 'NOT', text: 'NOT', raw: '\u00AC', position: i });
      i++;
    } else if (c === '"' || c === "'") {
      const start = i;
      const quote = c;
      i++;
      let value = '';
      let closed = false;
      while (i < len) {
        if (input[i] === '\\' && i + 1 < len) {
          value += input[i + 1];
          i += 2;
        } else if (input[i] === quote) {
          closed = true;
          i++;
          break;
        } else {
          value += input[i];
          i++;
        }
      }
      if (!closed) {
        return { tokens: [], error: `Unterminated quote at position ${start}` };
      }
      tokens.push({ type: 'STRING', text: value, raw: input.slice(start, i), position: start });
    } else if (/[0-9]/.test(c)) {
      const start = i;
      let seenDot = false;
      while (i < len) {
        if (/[0-9]/.test(input[i])) {
          i++;
        } else if (input[i] === '.' && !seenDot && i + 1 < len && /[0-9]/.test(input[i + 1])) {
          seenDot = true;
          i++;
        } else {
          break;
        }
      }
      const num = input.slice(start, i);
      tokens.push({ type: 'NUMBER', text: num, raw: num, position: start });
    } else if (/[a-zA-Z_]/.test(c)) {
      const start = i;
      while (i < len && /[a-zA-Z0-9_-]/.test(input[i])) {
        i++;
      }
      const word = input.slice(start, i);
      const upper = word.toUpperCase();
      if (upper === 'AND') {
        tokens.push({ type: 'AND', text: 'AND', raw: word, position: start });
      } else if (upper === 'OR') {
        tokens.push({ type: 'OR', text: 'OR', raw: word, position: start });
      } else if (upper === 'NOT') {
        tokens.push({ type: 'NOT', text: 'NOT', raw: word, position: start });
      } else {
        tokens.push({ type: 'WORD', text: word, raw: word, position: start });
      }
    } else {
      return { tokens: [], error: `Unexpected character '${c}' at position ${i}` };
    }
  }

  tokens.push({ type: 'EOF', text: '', raw: '', position: len });
  return { tokens, error: null };
}

function buildLookupSets(metadata: FilterMetadata | null) {
  const genreKeys = new Set((metadata?.genres ?? []).map(normalizeKey));
  const typeKeys = new Set((metadata?.mediaTypes ?? []).map(normalizeKey));
  return { genreKeys, typeKeys };
}

export function validateQuerySyntax(query: string, metadata: FilterMetadata | null): QueryValidationResult {
  const trimmed = query.trim();
  if (!trimmed) {
    return { valid: true, error: null };
  }

  const { tokens, error } = tokenizeRaw(trimmed);
  if (error) {
    return { valid: false, error };
  }

  const { genreKeys, typeKeys } = buildLookupSets(metadata);
  const checkMetadata = genreKeys.size > 0 || typeKeys.size > 0;
  let cursor = 0;
  let depth = 0;

  const cur = () => tokens[cursor];
  const peek = () => tokens[Math.min(cursor + 1, tokens.length - 1)];
  const adv = () => {
    if (cursor < tokens.length - 1) cursor++;
  };

  function parseOr(): string | null {
    const err = parseAnd();
    if (err) return err;
    while (cur().type === 'OR') {
      adv();
      const nextErr = parseAnd();
      if (nextErr) return nextErr;
    }
    return null;
  }

  function parseAnd(): string | null {
    const err = parseNot();
    if (err) return err;
    while (cur().type === 'AND') {
      adv();
      const nextErr = parseNot();
      if (nextErr) return nextErr;
    }
    return null;
  }

  function parseNot(): string | null {
    if (cur().type === 'NOT') {
      depth++;
      if (depth > MAX_DEPTH) return `Nesting exceeds maximum depth of ${MAX_DEPTH}`;
      adv();
      const err = parseNot();
      depth--;
      return err;
    }
    return parsePrimary();
  }

  function parsePrimary(): string | null {
    const tok = cur();
    if (tok.type === 'LPAREN') {
      depth++;
      if (depth > MAX_DEPTH) return `Nesting exceeds maximum depth of ${MAX_DEPTH}`;
      adv();
      const err = parseOr();
      if (err) return err;
      if (cur().type !== 'RPAREN') {
        return `Expected closing ')' at position ${cur().position}`;
      }
      adv();
      depth--;
      return null;
    }

    if (tok.type === 'STRING') {
      adv();
      const key = normalizeKey(tok.text);
      if (checkMetadata && !genreKeys.has(key) && !typeKeys.has(key)) {
        return `Unknown genre or media type "${tok.text}"`;
      }
      return null;
    }

    if (tok.type === 'WORD') {
      const lower = tok.text.toLowerCase();
      if (NUMERIC_FIELDS.has(lower) && peek().type === 'COMP') {
        adv();
        adv();
        if (cur().type !== 'NUMBER') {
          return `Expected number after comparator at position ${cur().position}`;
        }
        adv();
        return null;
      }
      if (TEXT_FIELDS.has(lower) && (peek().type === 'COLON' || peek().text === '=' || peek().text === '==')) {
        adv();
        adv();
        if (cur().type !== 'STRING' && cur().type !== 'WORD' && cur().type !== 'NUMBER') {
          return `Expected text value at position ${cur().position}`;
        }
        if (!cur().text.trim()) {
          return `Search value cannot be empty at position ${cur().position}`;
        }
        adv();
        return null;
      }
      if ((lower === 'genre' || lower === 'type' || lower === 'mediatype' || lower === 'media_type')
        && (peek().type === 'COLON' || peek().text === '=' || peek().text === '==')) {
        const isGenre = lower === 'genre';
        adv();
        adv();
        const phrase = readPhrase();
        if (!phrase) return `Expected value at position ${cur().position}`;
        const key = normalizeKey(phrase);
        if (checkMetadata) {
          if (isGenre && !genreKeys.has(key)) return `Unknown genre "${phrase}"`;
          if (!isGenre && !typeKeys.has(key)) return `Unknown media type "${phrase}"`;
        }
        return null;
      }
      if (lower === 'seen') {
        adv();
        if (cur().type === 'COMP' && (cur().text === '=' || cur().text === '==' || cur().text === '!=')) {
          adv();
          const v = cur().text.toLowerCase();
          if (cur().type !== 'WORD' || (v !== 'true' && v !== 'false')) {
            return `Expected 'true' or 'false' at position ${cur().position}`;
          }
          adv();
        }
        return null;
      }
      if (lower === 'unseen') {
        adv();
        return null;
      }

      const phrase = readPhrase();
      if (!phrase) return `Unexpected token at position ${tok.position}`;
      const key = normalizeKey(phrase);
      if (checkMetadata && !genreKeys.has(key) && !typeKeys.has(key)) {
        return `Unknown term "${phrase}" — use a genre, media type, or field (e.g. rating >= 8, title:"...")`;
      }
      return null;
    }

    if (tok.type === 'EOF') {
      return `Unexpected end of query at position ${tok.position}`;
    }
    return `Unexpected token '${tok.raw}' at position ${tok.position}`;
  }

  function readPhrase(): string | null {
    if (cur().type === 'STRING') {
      const val = cur().text;
      adv();
      return val;
    }
    if (cur().type !== 'WORD') return null;
    const parts: string[] = [cur().text];
    adv();
    while (cur().type === 'WORD') {
      parts.push(cur().text);
      adv();
    }
    return parts.join(' ');
  }

  const err = parseOr();
  if (err) {
    return { valid: false, error: err };
  }
  if (cur().type !== 'EOF') {
    return { valid: false, error: `Unexpected token '${cur().raw}' at position ${cur().position}` };
  }
  return { valid: true, error: null };
}

export function parseQueryChips(query: string, metadata: FilterMetadata | null): QueryChip[] {
  const trimmed = query.trim();
  if (!trimmed) return [];
  const { tokens, error } = tokenizeRaw(trimmed);
  if (error) {
    return [{ id: 'raw-0', kind: 'unknown', raw: trimmed }];
  }

  const { genreKeys, typeKeys } = buildLookupSets(metadata);
  const chips: QueryChip[] = [];
  let i = 0;

  while (i < tokens.length && tokens[i].type !== 'EOF') {
    const tok = tokens[i];
    const next = tokens[i + 1];

    if (tok.type === 'AND' || tok.type === 'OR') {
      chips.push({ id: `c-${i}`, kind: 'operator', raw: tok.type });
      i++;
      continue;
    }
    if (tok.type === 'NOT') {
      chips.push({ id: `c-${i}`, kind: 'not', raw: 'NOT' });
      i++;
      continue;
    }
    if (tok.type === 'LPAREN' || tok.type === 'RPAREN') {
      chips.push({ id: `c-${i}`, kind: 'paren', raw: tok.raw });
      i++;
      continue;
    }
    if (tok.type === 'WORD') {
      const lower = tok.text.toLowerCase();
      if (NUMERIC_FIELDS.has(lower) && next?.type === 'COMP' && tokens[i + 2]?.type === 'NUMBER') {
        chips.push({
          id: `c-${i}`,
          kind: 'comparison',
          raw: `${tok.text} ${next.raw} ${tokens[i + 2].raw}`,
        });
        i += 3;
        continue;
      }
      if (TEXT_FIELDS.has(lower) && (next?.type === 'COLON' || next?.text === '=' || next?.text === '==') && tokens[i + 2] && tokens[i + 2].type !== 'EOF') {
        chips.push({
          id: `c-${i}`,
          kind: 'text',
          raw: `${tok.text}:${tokens[i + 2].raw}`,
        });
        i += 3;
        continue;
      }
      if ((lower === 'genre' || lower === 'type' || lower === 'mediatype' || lower === 'media_type')
        && (next?.type === 'COLON' || next?.text === '=' || next?.text === '==')
        && tokens[i + 2] && tokens[i + 2].type !== 'EOF') {
        const isGenre = lower === 'genre';
        chips.push({
          id: `c-${i}`,
          kind: isGenre ? 'genre' : 'mediaType',
          raw: `${tok.text}:${tokens[i + 2].raw}`,
        });
        i += 3;
        continue;
      }
      if (lower === 'seen') {
        if (next?.type === 'COMP' && tokens[i + 2]?.type === 'WORD') {
          chips.push({ id: `c-${i}`, kind: 'seen', raw: `seen ${next.raw} ${tokens[i + 2].raw}` });
          i += 3;
        } else {
          chips.push({ id: `c-${i}`, kind: 'seen', raw: 'seen' });
          i++;
        }
        continue;
      }
      if (lower === 'unseen') {
        chips.push({ id: `c-${i}`, kind: 'seen', raw: 'unseen' });
        i++;
        continue;
      }

      const startIdx = i;
      const words: string[] = [tok.text];
      i++;
      while (i < tokens.length && tokens[i].type === 'WORD') {
        words.push(tokens[i].text);
        i++;
      }
      const phrase = words.join(' ');
      const key = normalizeKey(phrase);
      const kind: ChipKind = typeKeys.has(key)
        ? 'mediaType'
        : genreKeys.has(key)
          ? 'genre'
          : 'unknown';
      chips.push({ id: `c-${startIdx}`, kind, raw: formatTerm(phrase) });
      continue;
    }
    if (tok.type === 'STRING') {
      const key = normalizeKey(tok.text);
      const kind: ChipKind = typeKeys.has(key)
        ? 'mediaType'
        : genreKeys.has(key)
          ? 'genre'
          : 'unknown';
      chips.push({ id: `c-${i}`, kind, raw: tok.raw });
      i++;
      continue;
    }

    chips.push({ id: `c-${i}`, kind: 'unknown', raw: tok.raw });
    i++;
  }

  return chips;
}

export function chipsToQuery(chips: QueryChip[]): string {
  let out = '';
  for (let i = 0; i < chips.length; i++) {
    const raw = chips[i].raw;
    const prev = i > 0 ? chips[i - 1].raw : null;
    if (i === 0) {
      out = raw;
    } else if (prev === '(' || raw === ')') {
      out += raw;
    } else {
      out += ` ${raw}`;
    }
  }
  return out;
}

export function cleanAdjacentOperators(chips: QueryChip[]): QueryChip[] {
  const result: QueryChip[] = [];
  for (const chip of chips) {
    if (chip.kind === 'operator') {
      const prev = result[result.length - 1];
      if (!prev || prev.kind === 'operator' || prev.raw === '(') {
        continue;
      }
    }
    if (chip.raw === ')' && result.length > 0 && result[result.length - 1].kind === 'operator') {
      result.pop();
    }
    result.push(chip);
  }
  while (result.length > 0 && result[result.length - 1].kind === 'operator') {
    result.pop();
  }
  return result;
}

function isValueEndingChip(chip: QueryChip | undefined): boolean {
  if (!chip) return false;
  return chip.kind !== 'operator' && chip.kind !== 'not' && chip.raw !== '(';
}

export function appendClauseToQuery(
  currentQuery: string,
  clauseRaw: string,
  joinOp: 'AND' | 'OR',
  metadata: FilterMetadata | null,
): string {
  const chips = parseQueryChips(currentQuery, metadata);
  const next = [...chips];
  if (isValueEndingChip(next[next.length - 1])) {
    next.push({ id: `op-${Date.now()}`, kind: 'operator', raw: joinOp });
  }
  next.push({ id: `cl-${Date.now()}`, kind: 'unknown', raw: clauseRaw });
  return chipsToQuery(next);
}

export function toggleTermInQuery(
  currentQuery: string,
  label: string,
  kind: 'genre' | 'mediaType',
  metadata: FilterMetadata | null,
): string {
  const targetKey = normalizeKey(label);
  const chips = parseQueryChips(currentQuery, metadata);
  const formatted = formatTerm(label);

  const existingIndices: number[] = [];
  chips.forEach((chip, idx) => {
    if (chip.kind === kind || chip.kind === 'genre' || chip.kind === 'mediaType') {
      const stripped = chip.raw.replace(/^(genre|type|mediatype|media_type)\s*[:=]\s*/i, '').replace(/^["']|["']$/g, '');
      if (normalizeKey(stripped) === targetKey) {
        existingIndices.push(idx);
      }
    }
  });

  if (existingIndices.length > 0) {
    const kept = chips.filter((_, idx) => !existingIndices.includes(idx));
    return chipsToQuery(cleanAdjacentOperators(kept));
  }

  return appendClauseToQuery(currentQuery, formatted, 'OR', metadata);
}

export function getActiveTermsFromQuery(query: string, metadata: FilterMetadata | null): Set<string> {
  const active = new Set<string>();
  const chips = parseQueryChips(query, metadata);
  for (const chip of chips) {
    if (chip.kind === 'genre' || chip.kind === 'mediaType') {
      const stripped = chip.raw.replace(/^(genre|type|mediatype|media_type)\s*[:=]\s*/i, '').replace(/^["']|["']$/g, '');
      active.add(normalizeKey(stripped));
    }
  }
  return active;
}

export interface FlavorDescriptor {
  id: QueryFlavor;
  name: string;
  tagline: string;
  andLabel: string;
  orLabel: string;
  notLabel: string;
}

export const QUERY_FLAVORS: FlavorDescriptor[] = [
  {
    id: 'NETFLIX_AND_CHILL',
    name: 'Netflix & Chill',
    tagline: 'Clean & classic boolean keywords',
    andLabel: 'AND',
    orLabel: 'OR',
    notLabel: 'NOT',
  },
  {
    id: 'MOVIE_GEEK',
    name: 'Movie Geek',
    tagline: 'Computer science operators',
    andLabel: '&&',
    orLabel: '||',
    notLabel: '!',
  },
  {
    id: 'CINEPHILE',
    name: 'Cinephile',
    tagline: 'Formal philosophy & propositional logic',
    andLabel: '\u2227',
    orLabel: '\u2228',
    notLabel: '\u00AC',
  },
  {
    id: 'VO_ENTHUSIAST',
    name: 'V.O. Enthusiast',
    tagline: 'Natural prose connectors',
    andLabel: 'and',
    orLabel: 'or',
    notLabel: 'without',
  },
];

export function getFlavorDescriptor(flavor: QueryFlavor | undefined): FlavorDescriptor {
  return QUERY_FLAVORS.find((f) => f.id === flavor) ?? QUERY_FLAVORS[0];
}

export function formatOperatorLabel(op: 'AND' | 'OR' | 'NOT', flavor: QueryFlavor | undefined): string {
  const desc = getFlavorDescriptor(flavor);
  if (op === 'AND') return desc.andLabel;
  if (op === 'OR') return desc.orLabel;
  return desc.notLabel;
}

export function formatChipLabel(chip: QueryChip, flavor: QueryFlavor | undefined): string {
  if (chip.kind === 'operator') {
    if (chip.raw === 'AND' || chip.raw === 'OR') {
      return formatOperatorLabel(chip.raw, flavor);
    }
  }
  if (chip.kind === 'not') {
    return formatOperatorLabel('NOT', flavor);
  }
  return chip.raw;
}

function unquote(raw: string): string {
  return raw.replace(/^["']|["']$/g, '');
}

function formatComparisonEnglish(raw: string): string {
  const match = raw.match(/^([a-zA-Z_]+)\s*(<=|>=|!=|==|<|>|=)\s*(.+)$/);
  if (!match) return raw;
  const [, fieldRaw, op, val] = match;
  const field = fieldRaw.toLowerCase();
  const opText =
    op === '>=' ? 'at least'
      : op === '<=' ? 'at most'
        : op === '>' ? 'above'
          : op === '<' ? 'under'
            : op === '!=' ? 'not'
              : 'exactly';
  if (field.startsWith('runtime') || field === 'duration' || field === 'mins') {
    return `runtime is ${opText} ${val} min`;
  }
  if (field.startsWith('vote') || field === 'num_votes') {
    return `${opText} ${Number(val).toLocaleString()} votes`;
  }
  return `${field} is ${opText} ${val}`;
}

export function toPlainEnglishPreview(query: string, metadata: FilterMetadata | null): string {
  const trimmed = query.trim();
  if (!trimmed) return '';
  const chips = parseQueryChips(trimmed, metadata);
  if (chips.length === 0) return '';

  const parts: string[] = [];
  for (let i = 0; i < chips.length; i++) {
    const chip = chips[i];
    const next = chips[i + 1];
    if (chip.kind === 'operator') {
      parts.push(chip.raw === 'AND' ? 'and' : 'or');
    } else if (chip.kind === 'not') {
      if (next && (next.kind === 'genre' || next.kind === 'mediaType')) {
        parts.push(`not ${unquote(next.raw)}`);
        i++;
      } else {
        parts.push('not');
      }
    } else if (chip.kind === 'paren') {
      parts.push(chip.raw);
    } else if (chip.kind === 'genre' || chip.kind === 'mediaType') {
      const cleaned = chip.raw.replace(/^(genre|type|mediatype|media_type)\s*[:=]\s*/i, '');
      parts.push(`is ${unquote(cleaned)}`);
    } else if (chip.kind === 'comparison') {
      parts.push(formatComparisonEnglish(chip.raw));
    } else if (chip.kind === 'text') {
      const colonIdx = chip.raw.indexOf(':');
      if (colonIdx > 0) {
        const field = chip.raw.slice(0, colonIdx).toLowerCase();
        const val = unquote(chip.raw.slice(colonIdx + 1));
        parts.push(`${field} matches "${val}"`);
      } else {
        parts.push(chip.raw);
      }
    } else if (chip.kind === 'seen') {
      parts.push(chip.raw === 'unseen' ? 'unseen' : chip.raw);
    } else {
      parts.push(unquote(chip.raw));
    }
  }

  const joined = parts.join(' ').replace(/\(\s+/g, '(').replace(/\s+\)/g, ')');
  return `Titles where ${joined}`;
}
