import { useState } from 'react';
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

  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-4 space-y-3">
      <div className="flex items-center justify-between gap-2">
        <button
          type="button"
          onClick={() => setOpen((v) => !v)}
          className="flex items-center gap-2 text-sm font-semibold uppercase tracking-wider text-slate-300"
        >
          <span>Filters</span>
          <span className="text-xs text-slate-500">{open ? '▲ Hide' : '▼ Show'}</span>
        </button>
        <div className="flex items-center gap-3">
          {hasUser && (
            <label className="flex items-center gap-1.5 text-xs text-slate-300 cursor-pointer">
              <input
                type="checkbox"
                checked={filter.excludeSeen}
                onChange={(e) => onChange({ ...filter, excludeSeen: e.target.checked })}
                className="accent-amber-400"
              />
              Not seen only
            </label>
          )}
          <button
            type="button"
            onClick={onReset}
            className="text-xs text-slate-400 hover:text-slate-200"
          >
            Reset
          </button>
        </div>
      </div>

      {open && (
        <div className="space-y-4 pt-2 border-t border-slate-800">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Min Rating"
              value={filter.minRating}
              onChange={(e) => onChange({ ...filter, minRating: e.target.value })}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-400 focus:outline-none"
            />
            <input
              type="number"
              step="0.1"
              min="0"
              max="10"
              placeholder="Max Rating"
              value={filter.maxRating}
              onChange={(e) => onChange({ ...filter, maxRating: e.target.value })}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-400 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Min Year"
              value={filter.minYear}
              onChange={(e) => onChange({ ...filter, minYear: e.target.value })}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-400 focus:outline-none"
            />
            <input
              type="number"
              min={metadata?.minYear ?? 1888}
              placeholder="Max Year"
              value={filter.maxYear}
              onChange={(e) => onChange({ ...filter, maxYear: e.target.value })}
              className="rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-400 focus:outline-none"
            />
          </div>

          <input
            type="text"
            placeholder="Keywords (comma-separated title or director)..."
            value={filter.keywords}
            onChange={(e) => onChange({ ...filter, keywords: e.target.value })}
            className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-400 focus:outline-none"
          />

          {metadata && (
            <>
              <div className="space-y-1.5">
                <p className="text-xs font-medium text-slate-400">Genres (leave empty for all)</p>
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
                            ? 'bg-amber-400 text-slate-950'
                            : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                        }`}
                      >
                        {g}
                      </button>
                    );
                  })}
                </div>
              </div>

              <div className="space-y-1.5">
                <p className="text-xs font-medium text-slate-400">Media Types (leave empty for all)</p>
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
                            ? 'bg-amber-400 text-slate-950'
                            : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
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
    </section>
  );
}
