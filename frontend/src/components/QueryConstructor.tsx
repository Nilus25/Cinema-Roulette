import { useState } from 'react';
import { AlertTriangle, CheckCircle2, Code2, GripVertical, Plus, Sparkles, Trash2, X } from 'lucide-react';
import type { FilterMetadata, QueryFlavor } from '../types/api';
import {
  appendClauseToQuery,
  chipsToQuery,
  cleanAdjacentOperators,
  formatChipLabel,
  formatOperatorLabel,
  getFlavorDescriptor,
  parseQueryChips,
  toPlainEnglishPreview,
  validateQuerySyntax,
  type ChipKind,
} from '../utils/queryBuilder';

interface QueryConstructorProps {
  query: string;
  metadata: FilterMetadata | null;
  flavor: QueryFlavor;
  onChange: (nextQuery: string) => void;
}

const COMPARATORS = ['>=', '>', '<=', '<', '==', '!='] as const;
const NUMERIC_FIELDS = [
  { value: 'rating', label: 'rating', placeholder: '8.0', step: '0.1' },
  { value: 'year', label: 'year', placeholder: '2000', step: '1' },
  { value: 'runtime', label: 'runtime (min)', placeholder: '110', step: '5' },
  { value: 'votes', label: 'votes', placeholder: '50000', step: '1000' },
] as const;

function chipClassName(kind: ChipKind): string {
  switch (kind) {
    case 'genre':
      return 'border-[#39FF14] bg-[#39FF14]/15 text-[#39FF14]';
    case 'mediaType':
      return 'border-[#FF1E8A] bg-[#FF1E8A]/20 text-[#FF72B6]';
    case 'operator':
      return 'border-[#00E5FF] bg-[#00E5FF]/15 text-[#00E5FF]';
    case 'not':
      return 'border-[#FF1E56] bg-[#FF1E56]/20 text-[#FF6B8A]';
    case 'paren':
      return 'border-purple-400 bg-purple-500/20 text-purple-100';
    case 'comparison':
      return 'border-[#FFE600] bg-[#FFE600]/15 text-[#FFE600]';
    case 'text':
      return 'border-[#FF7A00] bg-[#FF7A00]/20 text-[#FF9E44]';
    case 'seen':
      return 'border-emerald-400 bg-emerald-500/20 text-emerald-300';
    default:
      return 'border-rose-400 bg-rose-500/20 text-rose-200';
  }
}

export function QueryConstructor({ query, metadata, flavor, onChange }: QueryConstructorProps) {
  const [textMode, setTextMode] = useState(false);
  const [dragIndex, setDragIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);

  const [numField, setNumField] = useState<(typeof NUMERIC_FIELDS)[number]['value']>('rating');
  const [numOp, setNumOp] = useState<(typeof COMPARATORS)[number]>('>=');
  const [numVal, setNumVal] = useState('');

  const [textField, setTextField] = useState<'title' | 'director' | 'keyword'>('director');
  const [textVal, setTextVal] = useState('');

  const validation = validateQuerySyntax(query, metadata);
  const chips = parseQueryChips(query, metadata);
  const flavorDesc = getFlavorDescriptor(flavor);
  const englishPreview = validation.valid ? toPlainEnglishPreview(query, metadata) : '';

  const insertToken = (rawToken: string) => {
    const current = query.trim();
    if (!current) {
      onChange(rawToken);
      return;
    }
    if (rawToken === ')') {
      onChange(`${current})`);
      return;
    }
    if (current.endsWith('(')) {
      onChange(`${current}${rawToken}`);
      return;
    }
    onChange(`${current} ${rawToken}`);
  };

  const handleAddNumericRule = () => {
    const cleaned = numVal.trim();
    if (!cleaned || Number.isNaN(Number(cleaned)) || Number(cleaned) < 0) return;
    const clause = `${numField} ${numOp} ${cleaned}`;
    onChange(appendClauseToQuery(query, clause, 'AND', metadata));
    setNumVal('');
  };

  const handleAddTextRule = () => {
    const cleaned = textVal.trim().replace(/"/g, '');
    if (!cleaned) return;
    const clause = `${textField}:"${cleaned}"`;
    onChange(appendClauseToQuery(query, clause, 'AND', metadata));
    setTextVal('');
  };

  const handleRemoveChip = (index: number) => {
    const remaining = chips.filter((_, idx) => idx !== index);
    onChange(chipsToQuery(cleanAdjacentOperators(remaining)));
  };

  const handleDrop = (targetIndex: number) => {
    if (dragIndex === null || dragIndex === targetIndex) {
      setDragIndex(null);
      setDragOverIndex(null);
      return;
    }
    const reordered = [...chips];
    const [moved] = reordered.splice(dragIndex, 1);
    reordered.splice(targetIndex, 0, moved);
    onChange(chipsToQuery(reordered));
    setDragIndex(null);
    setDragOverIndex(null);
  };

  const selectedNumMeta = NUMERIC_FIELDS.find((f) => f.value === numField) ?? NUMERIC_FIELDS[0];

  return (
    <div className="space-y-3 rounded border-2 border-[#00E5FF]/50 bg-[#0D0321] p-3 shadow-[0_0_16px_rgba(0,229,255,0.12)]">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex flex-wrap items-center gap-2">
          <Sparkles className="h-3.5 w-3.5 text-[#00E5FF]" />
          <span className="font-display text-xs font-extrabold tracking-wide text-[#00E5FF]">
            Query Constructor
          </span>
          <span className="rounded border border-purple-400/40 bg-purple-500/15 px-1.5 py-0.5 text-[10px] font-bold text-purple-200">
            {flavorDesc.name}
          </span>
          {query.trim() && (
            validation.valid ? (
              <span className="inline-flex items-center gap-1 rounded bg-[#39FF14]/15 px-1.5 py-0.5 text-[10px] font-extrabold text-[#39FF14]">
                <CheckCircle2 className="h-3 w-3" />
                Valid
              </span>
            ) : (
              <span className="inline-flex items-center gap-1 rounded bg-[#FF1E56]/20 px-1.5 py-0.5 text-[10px] font-extrabold text-[#FF6B8A]">
                <AlertTriangle className="h-3 w-3" />
                Syntax
              </span>
            )
          )}
        </div>

        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => setTextMode((v) => !v)}
            className={`inline-flex items-center gap-1 rounded border px-2 py-0.5 text-[11px] font-bold transition ${
              textMode
                ? 'border-[#00E5FF] bg-[#00E5FF] text-[#090118]'
                : 'border-purple-400/50 bg-[#14062B] text-purple-200 hover:border-[#00E5FF] hover:text-[#00E5FF]'
            }`}
          >
            <Code2 className="h-3 w-3" />
            <span>{textMode ? 'Chips' : 'Edit text'}</span>
          </button>

          {query.trim() && (
            <button
              type="button"
              onClick={() => onChange('')}
              className="inline-flex items-center gap-1 rounded border border-[#FF7A00]/50 bg-[#14062B] px-2 py-0.5 text-[11px] font-bold text-[#FF9E44] hover:border-[#FF7A00] hover:bg-[#FF7A00]/15"
            >
              <Trash2 className="h-3 w-3" />
              <span>Clear</span>
            </button>
          )}
        </div>
      </div>

      {/* Operator Palette */}
      <div className="flex flex-wrap items-center gap-1.5">
        <span className="text-[10px] font-bold uppercase tracking-wider text-purple-300/70">Logic:</span>
        {(['AND', 'OR'] as const).map((op) => (
          <button
            key={op}
            type="button"
            onClick={() => insertToken(op)}
            title={`Insert ${op}`}
            className="rounded border border-[#00E5FF]/60 bg-[#00E5FF]/10 px-2 py-0.5 font-mono text-[11px] font-extrabold text-[#00E5FF] transition hover:bg-[#00E5FF] hover:text-[#090118]"
          >
            {formatOperatorLabel(op, flavor)}
          </button>
        ))}
        <button
          type="button"
          onClick={() => insertToken('NOT')}
          title="Insert NOT"
          className="rounded border border-[#FF1E56]/70 bg-[#FF1E56]/15 px-2 py-0.5 font-mono text-[11px] font-extrabold text-[#FF6B8A] transition hover:bg-[#FF1E56] hover:text-white"
        >
          {formatOperatorLabel('NOT', flavor)}
        </button>
        {(['(', ')'] as const).map((p) => (
          <button
            key={p}
            type="button"
            onClick={() => insertToken(p)}
            className="rounded border border-purple-400/60 bg-purple-500/15 px-2 py-0.5 font-mono text-[11px] font-extrabold text-purple-100 transition hover:bg-purple-400 hover:text-[#090118]"
          >
            {p}
          </button>
        ))}
        <button
          type="button"
          onClick={() => onChange(appendClauseToQuery(query, 'unseen', 'AND', metadata))}
          className="rounded border border-emerald-400/60 bg-emerald-500/10 px-2 py-0.5 font-mono text-[11px] font-extrabold text-emerald-300 transition hover:bg-emerald-400 hover:text-[#090118]"
        >
          unseen
        </button>
        <button
          type="button"
          onClick={() => onChange(appendClauseToQuery(query, 'seen', 'AND', metadata))}
          className="rounded border border-emerald-400/40 bg-emerald-500/10 px-2 py-0.5 font-mono text-[11px] font-bold text-emerald-200/80 transition hover:bg-emerald-400 hover:text-[#090118]"
        >
          seen
        </button>
      </div>

      {/* Numeric & Text Comparator Builder */}
      <div className="grid grid-cols-1 gap-2 sm:grid-cols-2">
        <div className="flex items-center gap-1 rounded border border-[#FFE600]/35 bg-[#14062B] p-1">
          <select
            aria-label="Numeric field"
            value={numField}
            onChange={(e) => setNumField(e.target.value as (typeof NUMERIC_FIELDS)[number]['value'])}
            className="rounded bg-[#090118] px-1.5 py-1 font-mono text-[11px] font-bold text-[#FFE600] focus:outline-none"
          >
            {NUMERIC_FIELDS.map((f) => (
              <option key={f.value} value={f.value}>
                {f.label}
              </option>
            ))}
          </select>
          <select
            aria-label="Comparator"
            value={numOp}
            onChange={(e) => setNumOp(e.target.value as (typeof COMPARATORS)[number])}
            className="rounded bg-[#090118] px-1.5 py-1 font-mono text-[11px] font-extrabold text-[#00E5FF] focus:outline-none"
          >
            {COMPARATORS.map((op) => (
              <option key={op} value={op}>
                {op}
              </option>
            ))}
          </select>
          <input
            type="number"
            min="0"
            step={selectedNumMeta.step}
            placeholder={selectedNumMeta.placeholder}
            value={numVal}
            onChange={(e) => setNumVal(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                handleAddNumericRule();
              }
            }}
            className="w-full min-w-0 rounded bg-[#090118] px-2 py-1 font-mono text-[11px] font-bold text-[#FFE600] placeholder-purple-300/35 focus:outline-none"
          />
          <button
            type="button"
            onClick={handleAddNumericRule}
            title="Add numeric comparison"
            className="inline-flex shrink-0 items-center justify-center rounded bg-[#FFE600] px-2 py-1 text-[#090118] transition hover:bg-[#39FF14]"
          >
            <Plus className="h-3.5 w-3.5" />
          </button>
        </div>

        <div className="flex items-center gap-1 rounded border border-[#FF7A00]/35 bg-[#14062B] p-1">
          <select
            aria-label="Text field"
            value={textField}
            onChange={(e) => setTextField(e.target.value as 'title' | 'director' | 'keyword')}
            className="rounded bg-[#090118] px-1.5 py-1 font-mono text-[11px] font-bold text-[#FF9E44] focus:outline-none"
          >
            <option value="director">director:</option>
            <option value="title">title:</option>
            <option value="keyword">keyword:</option>
          </select>
          <input
            type="text"
            placeholder="e.g. Nolan"
            value={textVal}
            onChange={(e) => setTextVal(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                handleAddTextRule();
              }
            }}
            className="w-full min-w-0 rounded bg-[#090118] px-2 py-1 font-mono text-[11px] font-bold text-[#FF9E44] placeholder-purple-300/35 focus:outline-none"
          />
          <button
            type="button"
            onClick={handleAddTextRule}
            title="Add text rule"
            className="inline-flex shrink-0 items-center justify-center rounded bg-[#FF7A00] px-2 py-1 text-[#090118] transition hover:bg-[#39FF14]"
          >
            <Plus className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {/* Draggable Chip Canvas OR Raw Text Input */}
      {textMode ? (
        <input
          type="text"
          value={query}
          onChange={(e) => onChange(e.target.value)}
          placeholder='e.g. (Movie AND (Horror AND Drama) AND NOT Romance) OR "TV Series"'
          className={`w-full rounded border-2 bg-[#090118] px-3 py-2 font-mono text-xs font-bold focus:outline-none ${
            validation.valid
              ? 'border-[#00E5FF]/60 text-[#39FF14] focus:border-[#39FF14]'
              : 'border-[#FF1E56] text-[#FF9E44] focus:border-[#FF1E56]'
          }`}
        />
      ) : (
        <div
          className={`min-h-[46px] rounded border-2 bg-[#090118] p-2 transition ${
            validation.valid ? 'border-purple-500/45' : 'border-[#FF1E56]/80'
          }`}
        >
          {chips.length === 0 ? (
            <p className="py-1 text-center font-mono text-[11px] text-purple-300/45">
              Click genres/types above (adds as {flavorDesc.orLabel}), add comparators, or switch to Edit text...
            </p>
          ) : (
            <div className="flex flex-wrap items-center gap-1.5">
              {chips.map((chip, idx) => (
                <div
                  key={`${chip.id}-${idx}`}
                  draggable
                  onDragStart={() => setDragIndex(idx)}
                  onDragOver={(e) => {
                    e.preventDefault();
                    setDragOverIndex(idx);
                  }}
                  onDragLeave={() => setDragOverIndex(null)}
                  onDrop={() => handleDrop(idx)}
                  onDragEnd={() => {
                    setDragIndex(null);
                    setDragOverIndex(null);
                  }}
                  className={`inline-flex cursor-grab items-center gap-1 rounded border px-2 py-0.5 font-mono text-xs font-extrabold transition active:cursor-grabbing ${chipClassName(chip.kind)} ${
                    dragOverIndex === idx ? 'scale-105 ring-2 ring-[#FFE600]' : ''
                  }`}
                >
                  <GripVertical className="h-3 w-3 opacity-50" />
                  <span>{formatChipLabel(chip, flavor)}</span>
                  <button
                    type="button"
                    aria-label={`Remove ${chip.raw}`}
                    onClick={() => handleRemoveChip(idx)}
                    className="ml-0.5 rounded hover:bg-white/20"
                  >
                    <X className="h-3 w-3" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {englishPreview && (
        <p className="rounded border border-purple-500/25 bg-[#14062B]/70 px-2.5 py-1.5 text-[11px] italic text-purple-200/85">
          {englishPreview}
        </p>
      )}

      {!validation.valid && validation.error && (
        <p className="font-mono text-[11px] font-bold text-[#FF6B8A]">
          {validation.error}
        </p>
      )}
    </div>
  );
}
