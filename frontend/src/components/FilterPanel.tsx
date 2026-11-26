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
            className="swing-btn inline-flex items-center gap-2 bg-[#2B0F54] px-3 py-1.5 text-xs font-bold uppercase text-[#FFE600]"
          >
            <SlidersHorizontal className="h-3.5 w-3.5" />
            <span>Filters</span>
            {activeCount > 0 && (
              <span className="bg-[#39FF14] px-1.5 text-[10px] font-bold text-black">
                {activeCount}
              </span>
            )}
            {open ? <ChevronUp className="h-3.5 w-3.5" /> : <ChevronDown className="h-3.5 w-3.5" />}
          </button>

          {activeCount > 0 && (
            <button
              type="button"
              onClick={onReset}
              className="swing-btn inline-flex items-center gap-1 bg-[#FF6B00] px-2.5 py-1.5 text-[11px] font-bold uppercase text-black"
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
            className={`swing-btn inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-bold uppercase ${
              filter.excludeSeen
                ? 'bg-[#FF0080] text-white shadow-[0_0_16px_rgba(255,0,128,0.8)]'
                : 'bg-[#2B0F54] text-purple-200'
            }`}
          >
            <EyeOff className="h-3.5 w-3.5" />
            <span>{filter.excludeSeen ? 'Unseen Only' : 'All Matching'}</span>
          </button>
        )}
      </div>

      {open && (
        <div className="swing-inset space-y-4 p-3.5">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="MIN RATING"
              value={filter.minRating}
              onChange={(e) => onChange({ ...filter, minRating: e.target.value })}
              className="swing-inset px-2.5 py-1.5 text-xs text-[#FF9E44] placeholder-purple-400/50 focus:outline-none"
            />
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="MAX RATING"
              value={filter.maxRating}
              onChange={(e) => onChange({ ...filter, maxRating: e.target.value })}
              className="swing-inset px-2.5 py-1.5 text-xs text-[#FF9E44] placeholder-purple-400/50 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="MIN YEAR"
              value={filter.minYear}
              onChange={(e) => onChange({ ...filter, minYear: e.target.value })}
              className="swing-inset px-2.5 py-1.5 text-xs text-purple-200 placeholder-purple-400/50 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="MAX YEAR"
              value={filter.maxYear}
              onChange={(e) => onChange({ ...filter, maxYear: e.target.value })}
              className="swing-inset px-2.5 py-1.5 text-xs text-purple-200 placeholder-purple-400/50 focus:outline-none"
            />
          </div>

          <div className="relative">
            <Search className="pointer-events-none absolute left-2.5 top-2 h-3.5 w-3.5 text-[#39FF14]" />
            <input
              type="text"
              placeholder="KEYWORDS (TITLE OR DIRECTOR)..."
              value={filter.keywords}
              onChange={(e) => onChange({ ...filter, keywords: e.target.value })}
              className="swing-inset w-full pl-8 pr-2.5 py-1.5 text-xs text-[#39FF14] placeholder-purple-400/50 focus:outline-none"
            />
          </div>

          {metadata && (
            <>
              <div className="space-y-1.5">
                <p className="font-display text-[11px] uppercase text-[#39FF14]">Genres</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.genres.map((g) => {
                    const active = filter.genres.includes(g);
                    return (
                      <button
                        key={g}
                        type="button"
                        onClick={() => onChange({ ...filter, genres: toggleItem(filter.genres, g) })}
                        className={`swing-btn px-2 py-0.5 text-[11px] font-bold uppercase ${
                          active
                            ? 'bg-[#39FF14] text-black shadow-[0_0_10px_rgba(57,255,20,0.7)]'
                            : 'bg-[#2B0F54] text-slate-200'
                        }`}
                      >
                        {g}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-1.5">
                <p className="font-display text-[11px] uppercase text-[#FF5CA0]">Media Types</p>
                <div className="flex flex-wrap gap-1.5">
                  {metadata.mediaTypes.map((t) => {
                    const active = filter.mediaTypes.includes(t);
                    return (
                      <button
                        key={t}
                        type="button"
                        onClick={() => onChange({ ...filter, mediaTypes: toggleItem(filter.mediaTypes, t) })}
                        className={`swing-btn px-2 py-0.5 text-[11px] font-bold uppercase ${
                          active
                            ? 'bg-[#FF0080] text-white shadow-[0_0_10px_rgba(255,0,128,0.7)]'
                            : 'bg-[#2B0F54] text-slate-200'
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
