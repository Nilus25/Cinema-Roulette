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
            className="inline-flex items-center gap-2 rounded-full border-2 border-purple-300 bg-white px-4 py-1.5 text-xs font-extrabold text-purple-700 shadow-sm transition hover:border-purple-500 active:scale-95"
          >
            <SlidersHorizontal className="h-3.5 w-3.5 text-purple-600" />
            <span>Filters</span>
            {activeCount > 0 && (
              <span className="rounded-full bg-[#39FF14] px-2 py-0.2 text-[11px] font-extrabold text-green-950">
                {activeCount}
              </span>
            )}
            {open ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>

          {activeCount > 0 && (
            <button
              type="button"
              onClick={onReset}
              className="inline-flex items-center gap-1 rounded-full bg-orange-100 px-2.5 py-1 text-xs font-bold text-orange-700 hover:bg-orange-200"
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
            className={`inline-flex items-center gap-1.5 rounded-full border-2 px-4 py-1.5 text-xs font-extrabold transition active:scale-95 ${
              filter.excludeSeen
                ? 'border-[#D91B7A] bg-[#FF49B0] text-white shadow-[0_4px_14px_rgba(255,73,176,0.45)]'
                : 'border-slate-300 bg-white text-slate-600 hover:border-pink-400'
            }`}
          >
            <EyeOff className="h-3.5 w-3.5" />
            <span>{filter.excludeSeen ? 'Unseen Only' : 'All Matching'}</span>
          </button>
        )}
      </div>

      {open && (
        <div className="animate-pop-in space-y-4 rounded-3xl border-2 border-purple-200 bg-white/95 p-4 shadow-inner">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Min Rating"
              value={filter.minRating}
              onChange={(e) => onChange({ ...filter, minRating: e.target.value })}
              className="rounded-2xl border-2 border-orange-200 bg-orange-50/40 px-3 py-2 text-xs font-bold text-slate-800 placeholder-slate-400 focus:border-[#FF7A59] focus:outline-none"
            />
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Max Rating"
              value={filter.maxRating}
              onChange={(e) => onChange({ ...filter, maxRating: e.target.value })}
              className="rounded-2xl border-2 border-orange-200 bg-orange-50/40 px-3 py-2 text-xs font-bold text-slate-800 placeholder-slate-400 focus:border-[#FF7A59] focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Min Year"
              value={filter.minYear}
              onChange={(e) => onChange({ ...filter, minYear: e.target.value })}
              className="rounded-2xl border-2 border-purple-200 bg-purple-50/40 px-3 py-2 text-xs font-bold text-slate-800 placeholder-slate-400 focus:border-purple-500 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Max Year"
              value={filter.maxYear}
              onChange={(e) => onChange({ ...filter, maxYear: e.target.value })}
              className="rounded-2xl border-2 border-purple-200 bg-purple-50/40 px-3 py-2 text-xs font-bold text-slate-800 placeholder-slate-400 focus:border-purple-500 focus:outline-none"
            />
          </div>

          <div className="relative">
            <Search className="pointer-events-none absolute left-3.5 top-2.5 h-3.5 w-3.5 text-purple-500" />
            <input
              type="text"
              placeholder="Keywords (title or director)..."
              value={filter.keywords}
              onChange={(e) => onChange({ ...filter, keywords: e.target.value })}
              className="w-full rounded-2xl border-2 border-purple-200 bg-white pl-9 pr-3 py-2 text-xs font-semibold text-slate-800 placeholder-slate-400 focus:border-[#39FF14] focus:outline-none"
            />
          </div>

          {metadata && (
            <>
              <div className="space-y-1.5">
                <p className="font-display text-xs font-bold text-green-700">Genres</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.genres.map((g) => {
                    const active = filter.genres.includes(g);
                    return (
                      <button
                        key={g}
                        type="button"
                        onClick={() => onChange({ ...filter, genres: toggleItem(filter.genres, g) })}
                        className={`rounded-full border-2 px-3 py-1 text-xs font-bold transition ${
                          active
                            ? 'border-green-600 bg-[#39FF14] text-green-950 shadow-sm'
                            : 'border-slate-200 bg-slate-50 text-slate-600 hover:border-green-400'
                        }`}
                      >
                        {g}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-1.5">
                <p className="font-display text-xs font-bold text-[#D91B7A]">Media Types</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.mediaTypes.map((t) => {
                    const active = filter.mediaTypes.includes(t);
                    return (
                      <button
                        key={t}
                        type="button"
                        onClick={() => onChange({ ...filter, mediaTypes: toggleItem(filter.mediaTypes, t) })}
                        className={`rounded-full border-2 px-3 py-1 text-xs font-bold transition ${
                          active
                            ? 'border-[#D91B7A] bg-[#FF49B0] text-white shadow-sm'
                            : 'border-slate-200 bg-slate-50 text-slate-600 hover:border-pink-400'
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
