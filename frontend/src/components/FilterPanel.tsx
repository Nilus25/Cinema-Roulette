import { useState } from 'react';
import { ChevronDown, ChevronUp, EyeOff, RotateCcw, Search, SlidersHorizontal } from 'lucide-react';
import type { FilterMetadata, FilterState } from '../types/api';

interface FilterPanelProps {
  metadata: FilterMetadata | null;
  filter: FilterState;
  hasUser: boolean;
  onChange: (next: FilterState) => void;
  onReset: () => void;
}

export function FilterPanel({ metadata, filter, hasUser, onChange, onReset }: FilterPanelProps) {
  const [open, setOpen] = useState(false);

  const toggleItem = (list: string[], value: string) =>
    list.includes(value) ? list.filter((v) => v !== value) : [...list, value];

  const activeCount =
    (filter.minRating ? 1 : 0) +
    (filter.maxRating ? 1 : 0) +
    (filter.minYear ? 1 : 0) +
    (filter.maxYear ? 1 : 0) +
    (filter.keywords.trim() ? 1 : 0) +
    filter.genres.length +
    filter.mediaTypes.length;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setOpen((v) => !v)}
            className="inline-flex items-center gap-2 rounded-full border border-[#3E2468] bg-[#0B0614] px-3.5 py-1.5 text-xs font-semibold text-purple-200 transition hover:border-purple-400"
          >
            <SlidersHorizontal className="h-3.5 w-3.5 text-purple-400" />
            <span>Filters</span>
            {activeCount > 0 && (
              <span className="rounded-full bg-[#39FF14] px-1.5 py-0.2 text-[10px] font-bold text-[#07120A]">
                {activeCount}
              </span>
            )}
            {open ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>

          {activeCount > 0 && (
            <button
              type="button"
              onClick={onReset}
              className="inline-flex items-center gap-1 text-xs text-slate-400 hover:text-[#FF7A59]"
            >
              <RotateCcw className="h-3 w-3" />
              <span>Reset</span>
            </button>
          )}
        </div>

        {hasUser && (
          <button
            type="button"
            onClick={() => onChange({ ...filter, excludeSeen: !filter.excludeSeen })}
            className={`inline-flex items-center gap-1.5 rounded-full border px-3.5 py-1.5 text-xs font-semibold transition ${
              filter.excludeSeen
                ? 'border-[#FF2A85] bg-[#FF2A85]/20 text-[#FF5CA0] shadow-[0_0_14px_rgba(255,42,133,0.3)]'
                : 'border-[#3E2468] bg-[#0B0614] text-slate-400 hover:text-slate-200'
            }`}
          >
            <EyeOff className="h-3.5 w-3.5" />
            <span>{filter.excludeSeen ? 'Unseen Only' : 'All Matching'}</span>
          </button>
        )}
      </div>

      {open && (
        <div className="space-y-4 rounded-2xl border border-[#3E2468] bg-[#0B0614]/80 p-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Min Rating"
              value={filter.minRating}
              onChange={(e) => onChange({ ...filter, minRating: e.target.value })}
              className="rounded-xl border border-[#3E2468] bg-[#150C26] px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-[#FF9E44] focus:outline-none"
            />
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Max Rating"
              value={filter.maxRating}
              onChange={(e) => onChange({ ...filter, maxRating: e.target.value })}
              className="rounded-xl border border-[#3E2468] bg-[#150C26] px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-[#FF9E44] focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Min Year"
              value={filter.minYear}
              onChange={(e) => onChange({ ...filter, minYear: e.target.value })}
              className="rounded-xl border border-[#3E2468] bg-[#150C26] px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-purple-400 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Max Year"
              value={filter.maxYear}
              onChange={(e) => onChange({ ...filter, maxYear: e.target.value })}
              className="rounded-xl border border-[#3E2468] bg-[#150C26] px-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-purple-400 focus:outline-none"
            />
          </div>

          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-3.5 w-3.5 text-slate-500" />
            <input
              type="text"
              placeholder="Keywords (title or director, comma-separated)..."
              value={filter.keywords}
              onChange={(e) => onChange({ ...filter, keywords: e.target.value })}
              className="w-full rounded-xl border border-[#3E2468] bg-[#150C26] pl-8 pr-3 py-2 text-xs text-slate-100 placeholder-slate-500 focus:border-[#39FF14] focus:outline-none"
            />
          </div>

          {metadata && (
            <>
              <div className="space-y-1.5">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-purple-300">Genres</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.genres.map((g) => {
                    const active = filter.genres.includes(g);
                    return (
                      <button
                        key={g}
                        type="button"
                        onClick={() => onChange({ ...filter, genres: toggleItem(filter.genres, g) })}
                        className={`rounded-full px-2.5 py-1 text-xs font-medium transition ${
                          active
                            ? 'bg-[#39FF14] text-[#07120A] font-semibold'
                            : 'border border-[#3E2468] bg-[#150C26] text-slate-300 hover:border-purple-400'
                        }`}
                      >
                        {g}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-1.5">
                <p className="text-[11px] font-semibold uppercase tracking-wider text-purple-300">Media Types</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.mediaTypes.map((t) => {
                    const active = filter.mediaTypes.includes(t);
                    return (
                      <button
                        key={t}
                        type="button"
                        onClick={() => onChange({ ...filter, mediaTypes: toggleItem(filter.mediaTypes, t) })}
                        className={`rounded-full px-2.5 py-1 text-xs font-medium transition ${
                          active
                            ? 'bg-[#FF2A85] text-white font-semibold'
                            : 'border border-[#3E2468] bg-[#150C26] text-slate-300 hover:border-[#FF2A85]/60'
                        }`}
                      >
                        {t}
                      </button>
                    );
                  })}
                </div>
              </div>
            </>
          )}
        </div>
      )}
    </div>
  );
}
