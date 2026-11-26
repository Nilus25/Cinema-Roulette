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

  const hasAnyFilter = activeCount > 0 || filter.excludeSeen;

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => setOpen((v) => !v)}
            className="inline-flex items-center gap-2 rounded border-2 border-purple-400 bg-[#090118] px-3.5 py-1.5 text-xs font-bold text-purple-200 transition hover:border-[#39FF14] hover:text-[#39FF14]"
          >
            <SlidersHorizontal className="h-3.5 w-3.5" />
            <span>Filters</span>
            {activeCount > 0 && (
              <span className="rounded bg-[#39FF14] px-1.5 text-[11px] font-extrabold text-[#090118]">
                {activeCount}
              </span>
            )}
            {open ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>

          {hasUser && (
            <button
              type="button"
              onClick={() => onChange({ ...filter, excludeSeen: !filter.excludeSeen })}
              className={`inline-flex items-center gap-1.5 rounded border-2 px-3.5 py-1.5 text-xs font-bold transition ${
                filter.excludeSeen
                  ? 'border-[#FF1E8A] bg-[#FF1E8A] text-white shadow-[0_0_16px_rgba(255,30,138,0.65)]'
                  : 'border-purple-500/60 bg-[#090118] text-purple-200 hover:border-[#FF1E8A]'
              }`}
            >
              <EyeOff className="h-3.5 w-3.5" />
              <span>{filter.excludeSeen ? 'Unseen Only' : 'Include Seen'}</span>
            </button>
          )}
        </div>

        {hasAnyFilter && (
          <button
            type="button"
            onClick={onReset}
            className="inline-flex items-center gap-1 rounded border-2 border-[#FF7A00]/60 bg-[#FF7A00]/15 px-2.5 py-1.5 text-xs font-bold text-[#FF9E44] hover:bg-[#FF7A00] hover:text-[#090118]"
          >
            <RotateCcw className="h-3 w-3" />
            <span>Reset Filters</span>
          </button>
        )}
      </div>

      {open && (
        <div className="animate-pop-card space-y-4 rounded border-2 border-purple-500/70 bg-[#090118] p-4">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Min Rating"
              value={filter.minRating}
              onChange={(e) => onChange({ ...filter, minRating: e.target.value })}
              className="rounded border-2 border-orange-500/50 bg-[#14062B] px-3 py-2 text-xs font-semibold text-[#FF9E44] placeholder-purple-300/40 focus:border-[#FF7A00] focus:outline-none"
            />
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Max Rating"
              value={filter.maxRating}
              onChange={(e) => onChange({ ...filter, maxRating: e.target.value })}
              className="rounded border-2 border-orange-500/50 bg-[#14062B] px-3 py-2 text-xs font-semibold text-[#FF9E44] placeholder-purple-300/40 focus:border-[#FF7A00] focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Min Year"
              value={filter.minYear}
              onChange={(e) => onChange({ ...filter, minYear: e.target.value })}
              className="rounded border-2 border-purple-500/50 bg-[#14062B] px-3 py-2 text-xs font-semibold text-purple-100 placeholder-purple-300/40 focus:border-purple-400 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Max Year"
              value={filter.maxYear}
              onChange={(e) => onChange({ ...filter, maxYear: e.target.value })}
              className="rounded border-2 border-purple-500/50 bg-[#14062B] px-3 py-2 text-xs font-semibold text-purple-100 placeholder-purple-300/40 focus:border-purple-400 focus:outline-none"
            />
          </div>

          <div className="relative">
            <Search className="pointer-events-none absolute left-3 top-2.5 h-3.5 w-3.5 text-[#39FF14]" />
            <input
              type="text"
              placeholder="Keywords (title or director, comma-separated)..."
              value={filter.keywords}
              onChange={(e) => onChange({ ...filter, keywords: e.target.value })}
              className="w-full rounded border-2 border-purple-500/50 bg-[#14062B] pl-9 pr-3 py-2 text-xs font-semibold text-[#39FF14] placeholder-purple-300/40 focus:border-[#39FF14] focus:outline-none"
            />
          </div>

          {metadata && (
            <>
              <div className="space-y-1.5">
                <p className="font-display text-xs font-bold text-[#39FF14]">Genres</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.genres.map((g) => {
                    const active = filter.genres.includes(g);
                    return (
                      <button
                        key={g}
                        type="button"
                        onClick={() => onChange({ ...filter, genres: toggleItem(filter.genres, g) })}
                        className={`rounded-full px-3 py-1 text-xs font-extrabold transition ${
                          active
                            ? 'casino-chip-green'
                            : 'border-2 border-purple-500/40 bg-[#14062B] text-purple-100 hover:border-[#39FF14]'
                        }`}
                      >
                        {g}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-1.5">
                <p className="font-display text-xs font-bold text-[#FF5CA8]">Media Types</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.mediaTypes.map((t) => {
                    const active = filter.mediaTypes.includes(t);
                    return (
                      <button
                        key={t}
                        type="button"
                        onClick={() => onChange({ ...filter, mediaTypes: toggleItem(filter.mediaTypes, t) })}
                        className={`rounded-full px-3 py-1 text-xs font-extrabold transition ${
                          active
                            ? 'casino-chip-pink'
                            : 'border-2 border-purple-500/40 bg-[#14062B] text-purple-100 hover:border-[#FF1E8A]'
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
