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
            className="inline-flex items-center gap-2 border-2 border-purple-500 bg-[#05010D] px-3.5 py-1.5 text-xs font-bold uppercase text-purple-300 shadow-[0_0_10px_rgba(168,85,247,0.4)] hover:border-[#39FF14] hover:text-[#39FF14]"
          >
            <SlidersHorizontal className="h-3.5 w-3.5" />
            <span>Filters</span>
            {activeCount > 0 && (
              <span className="bg-[#39FF14] px-1.5 py-0.2 text-[10px] font-bold text-black">
                {activeCount}
              </span>
            )}
            {open ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>

          {activeCount > 0 && (
            <button
              type="button"
              onClick={onReset}
              className="inline-flex items-center gap-1 text-xs uppercase text-[#FF7A59] hover:text-white"
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
            className={`inline-flex items-center gap-1.5 border-2 px-3.5 py-1.5 text-xs font-bold uppercase transition ${
              filter.excludeSeen
                ? 'border-[#FF007F] bg-[#FF007F] text-white shadow-[0_0_18px_rgba(255,0,127,0.7)]'
                : 'border-purple-500/60 bg-[#05010D] text-slate-300 hover:border-[#FF007F]'
            }`}
          >
            <EyeOff className="h-3.5 w-3.5" />
            <span>{filter.excludeSeen ? 'Unseen Only' : 'All Matching'}</span>
          </button>
        )}
      </div>

      {open && (
        <div className="space-y-4 border-2 border-purple-500/60 bg-[#05010D] p-4 shadow-[inset_0_0_20px_rgba(168,85,247,0.18)]">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="MIN RATING"
              value={filter.minRating}
              onChange={(e) => onChange({ ...filter, minRating: e.target.value })}
              className="border border-purple-500/50 bg-[#0C041A] px-3 py-2 text-xs text-[#FF9E44] placeholder-slate-600 focus:border-[#FF9E44] focus:outline-none"
            />
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="MAX RATING"
              value={filter.maxRating}
              onChange={(e) => onChange({ ...filter, maxRating: e.target.value })}
              className="border border-purple-500/50 bg-[#0C041A] px-3 py-2 text-xs text-[#FF9E44] placeholder-slate-600 focus:border-[#FF9E44] focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="MIN YEAR"
              value={filter.minYear}
              onChange={(e) => onChange({ ...filter, minYear: e.target.value })}
              className="border border-purple-500/50 bg-[#0C041A] px-3 py-2 text-xs text-purple-200 placeholder-slate-600 focus:border-purple-400 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="MAX YEAR"
              value={filter.maxYear}
              onChange={(e) => onChange({ ...filter, maxYear: e.target.value })}
              className="border border-purple-500/50 bg-[#0C041A] px-3 py-2 text-xs text-purple-200 placeholder-slate-600 focus:border-purple-400 focus:outline-none"
            />
          </div>

          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-3.5 w-3.5 text-[#39FF14]" />
            <input
              type="text"
              placeholder="KEYWORDS (TITLE OR DIRECTOR)..."
              value={filter.keywords}
              onChange={(e) => onChange({ ...filter, keywords: e.target.value })}
              className="w-full border border-purple-500/50 bg-[#0C041A] pl-8 pr-3 py-2 text-xs text-[#39FF14] placeholder-slate-600 focus:border-[#39FF14] focus:outline-none"
            />
          </div>

          {metadata && (
            <>
              <div className="space-y-1.5">
                <p className="font-display text-[11px] uppercase tracking-wider text-[#39FF14]">Genres</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.genres.map((g) => {
                    const active = filter.genres.includes(g);
                    return (
                      <button
                        key={g}
                        type="button"
                        onClick={() => onChange({ ...filter, genres: toggleItem(filter.genres, g) })}
                        className={`border px-2.5 py-1 text-[11px] font-bold uppercase transition ${
                          active
                            ? 'border-[#39FF14] bg-[#39FF14] text-black shadow-[0_0_10px_rgba(57,255,20,0.65)]'
                            : 'border-purple-500/40 bg-[#0C041A] text-slate-300 hover:border-[#39FF14]'
                        }`}
                      >
                        {g}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-1.5">
                <p className="font-display text-[11px] uppercase tracking-wider text-[#FF5CA0]">Media Types</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.mediaTypes.map((t) => {
                    const active = filter.mediaTypes.includes(t);
                    return (
                      <button
                        key={t}
                        type="button"
                        onClick={() => onChange({ ...filter, mediaTypes: toggleItem(filter.mediaTypes, t) })}
                        className={`border px-2.5 py-1 text-[11px] font-bold uppercase transition ${
                          active
                            ? 'border-[#FF007F] bg-[#FF007F] text-white shadow-[0_0_10px_rgba(255,0,127,0.65)]'
                            : 'border-purple-500/40 bg-[#0C041A] text-slate-300 hover:border-[#FF007F]'
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
