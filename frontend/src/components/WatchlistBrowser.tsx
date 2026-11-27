import { useEffect, useMemo, useRef, useState } from 'react';
import {
  ArrowDown,
  ArrowUp,
  Check,
  ChevronDown,
  Clock,
  ExternalLink,
  Eye,
  ListVideo,
  Search,
  Star,
  Users,
  X,
} from 'lucide-react';
import type { SortBy, SortDirection, WatchlistItemResponse } from '../types/api';

interface WatchlistBrowserProps {
  items: WatchlistItemResponse[];
  hasUser: boolean;
  loading: boolean;
  sortBy: SortBy;
  sortDir: SortDirection;
  onChangeSort: (nextSortBy: SortBy, nextSortDir: SortDirection) => void;
  onToggleSeen: (imdbId: string, currentlySeen: boolean) => Promise<void>;
}

const SORT_OPTIONS: { value: SortBy; label: string; defaultDir: SortDirection }[] = [
  { value: 'DEFAULT', label: 'Default', defaultDir: 'DESC' },
  { value: 'RATING', label: 'IMDb Rating', defaultDir: 'DESC' },
  { value: 'YEAR', label: 'Release Year', defaultDir: 'DESC' },
  { value: 'RUNTIME', label: 'Runtime', defaultDir: 'ASC' },
  { value: 'VOTES', label: 'Popularity (Votes)', defaultDir: 'DESC' },
  { value: 'TITLE', label: 'Title (A–Z)', defaultDir: 'ASC' },
  { value: 'UNSEEN', label: 'Unseen First', defaultDir: 'DESC' },
];

function formatRuntime(mins: number): string {
  if (mins < 60) return `${mins}m`;
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return m === 0 ? `${h}h` : `${h}h ${m}m`;
}

function formatVotes(votes: number): string {
  if (votes >= 1_000_000) return `${(votes / 1_000_000).toFixed(1)}M`;
  if (votes >= 1_000) return `${(votes / 1_000).toFixed(votes >= 100_000 ? 0 : 1)}k`;
  return String(votes);
}

export function WatchlistBrowser({
  items,
  hasUser,
  loading,
  sortBy,
  sortDir,
  onChangeSort,
  onToggleSeen,
}: WatchlistBrowserProps) {
  const [sortMenuOpen, setSortMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [quickSearch, setQuickSearch] = useState('');
  const menuRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!sortMenuOpen) return;
    const handleOutside = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setSortMenuOpen(false);
      }
    };
    document.addEventListener('mousedown', handleOutside);
    return () => document.removeEventListener('mousedown', handleOutside);
  }, [sortMenuOpen]);

  const displayedItems = useMemo(() => {
    const q = quickSearch.trim().toLowerCase();
    if (!q) return items;
    return items.filter(({ item }) =>
      item.title.toLowerCase().includes(q)
      || (item.originalTitle?.toLowerCase().includes(q) ?? false)
      || item.director.toLowerCase().includes(q),
    );
  }, [items, quickSearch]);

  const activeSortLabel = SORT_OPTIONS.find((o) => o.value === sortBy)?.label ?? 'Default';

  return (
    <section className="neo-panel-orange p-4 space-y-3">
      <div className="flex items-center justify-between gap-2 border-b-2 border-[#FF7A00]/40 pb-2.5">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-[#FF9E44]">
          <ListVideo className="h-4 w-4" />
          <span>Cinematheque</span>
          <span className="rounded bg-[#39FF14] px-2 py-0.5 text-xs font-extrabold text-[#090118]">
            {displayedItems.length}
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          {loading && <span className="text-[11px] font-bold text-[#FFE600]">Loading...</span>}

          <button
            type="button"
            title="Quick search"
            onClick={() => {
              setSearchOpen((v) => !v);
              if (searchOpen) setQuickSearch('');
            }}
            className={`inline-flex items-center justify-center rounded border p-1 transition ${
              searchOpen || quickSearch
                ? 'border-[#39FF14] bg-[#39FF14]/20 text-[#39FF14]'
                : 'border-[#FF7A00]/45 bg-[#090118] text-orange-200/80 hover:border-[#FF9E44] hover:text-[#FF9E44]'
            }`}
          >
            <Search className="h-3.5 w-3.5" />
          </button>

          <div ref={menuRef} className="relative flex items-center gap-1">
            {sortBy !== 'DEFAULT' && (
              <button
                type="button"
                title={sortDir === 'ASC' ? 'Ascending (click to flip)' : 'Descending (click to flip)'}
                onClick={() => onChangeSort(sortBy, sortDir === 'ASC' ? 'DESC' : 'ASC')}
                className="inline-flex items-center justify-center rounded border border-[#FF7A00]/60 bg-[#090118] p-1 text-[#FFE600] transition hover:border-[#39FF14] hover:text-[#39FF14]"
              >
                {sortDir === 'ASC' ? <ArrowUp className="h-3.5 w-3.5" /> : <ArrowDown className="h-3.5 w-3.5" />}
              </button>
            )}

            <button
              type="button"
              aria-haspopup="listbox"
              aria-expanded={sortMenuOpen}
              title={`Sort: ${activeSortLabel}`}
              onClick={() => setSortMenuOpen((v) => !v)}
              className={`inline-flex items-center gap-1 rounded border px-2 py-1 text-[11px] font-bold transition ${
                sortBy !== 'DEFAULT'
                  ? 'border-[#FFE600]/70 bg-[#FF7A00]/20 text-[#FFE600]'
                  : 'border-[#FF7A00]/45 bg-[#090118] text-orange-200/85 hover:border-[#FF9E44] hover:text-[#FF9E44]'
              }`}
            >
              <span className="max-w-[78px] truncate">{sortBy === 'DEFAULT' ? 'Sort' : activeSortLabel}</span>
              <ChevronDown className="h-3 w-3 shrink-0" />
            </button>

            {sortMenuOpen && (
              <div
                role="listbox"
                className="absolute right-0 top-full z-20 mt-1.5 w-44 rounded border-2 border-[#FF7A00] bg-[#090118] p-1 shadow-[0_8px_24px_rgba(0,0,0,0.75)]"
              >
                {SORT_OPTIONS.map((opt) => {
                  const selected = sortBy === opt.value;
                  return (
                    <button
                      key={opt.value}
                      type="button"
                      role="option"
                      aria-selected={selected}
                      onClick={() => {
                        onChangeSort(opt.value, opt.defaultDir);
                        setSortMenuOpen(false);
                      }}
                      className={`flex w-full items-center justify-between rounded px-2.5 py-1.5 text-left text-xs font-bold transition ${
                        selected
                          ? 'bg-[#FF7A00] text-[#090118]'
                          : 'text-orange-100 hover:bg-[#1E0B3B] hover:text-[#39FF14]'
                      }`}
                    >
                      <span>{opt.label}</span>
                      {selected && <Check className="h-3.5 w-3.5" />}
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>

      {searchOpen && (
        <div className="relative">
          <Search className="pointer-events-none absolute left-2.5 top-2 h-3.5 w-3.5 text-[#39FF14]" />
          <input
            type="text"
            autoFocus
            value={quickSearch}
            onChange={(e) => setQuickSearch(e.target.value)}
            placeholder="Quick filter list (title, original title, director)..."
            className="w-full rounded border border-[#FF7A00]/60 bg-[#090118] pl-8 pr-7 py-1.5 text-xs font-semibold text-white placeholder-orange-200/40 focus:border-[#39FF14] focus:outline-none"
          />
          {quickSearch && (
            <button
              type="button"
              aria-label="Clear quick search"
              onClick={() => setQuickSearch('')}
              className="absolute right-2 top-2 text-orange-200/70 hover:text-white"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      )}

      {displayedItems.length === 0 ? (
        <p className="py-6 text-center text-sm font-semibold text-orange-200/70">
          No matching titles in this watchlist.
        </p>
      ) : (
        <div className="max-h-[420px] space-y-2 overflow-y-auto pr-1">
          {displayedItems.map(({ item, seen }) => {
            const showOriginal = Boolean(
              item.originalTitle && item.originalTitle.toLowerCase() !== item.title.toLowerCase(),
            );
            return (
              <div
                key={item.imdbId}
                className={`film-cell flex items-stretch ${
                  seen ? 'film-cell-seen' : ''
                }`}
              >
                <div aria-hidden="true" className="film-cell-rail border-r border-orange-500/35" />

                <div className="flex min-w-0 flex-1 flex-col gap-1.5 px-3 py-2.5">
                  <div className="min-w-0">
                    <a
                      href={item.imdbUrl}
                      target="_blank"
                      rel="noreferrer"
                      className="inline-flex max-w-full items-center gap-1.5 text-sm font-bold text-white transition hover:text-[#39FF14]"
                    >
                      <span className="truncate">{item.title}</span>
                      <ExternalLink className="h-3 w-3 shrink-0 text-[#FF9E44]" />
                    </a>
                    {showOriginal && (
                      <p className="truncate text-[11px] italic text-purple-200/55">{item.originalTitle}</p>
                    )}
                  </div>

                  <div className="flex items-end justify-between gap-2">
                    <div className="flex min-w-0 flex-wrap items-center gap-2 text-xs font-semibold text-orange-100/70">
                      {item.releaseYear && <span>{item.releaseYear}</span>}
                      {item.rating !== null && (
                        <span className="inline-flex items-center gap-0.5 font-extrabold text-[#FFE600]">
                          <Star className="h-3 w-3 fill-[#FFE600]" />
                          {item.rating}
                        </span>
                      )}
                      {item.runtimeMinutes !== null && (
                        <span className="inline-flex items-center gap-0.5 text-[#00E5FF]">
                          <Clock className="h-3 w-3" />
                          {formatRuntime(item.runtimeMinutes)}
                        </span>
                      )}
                      {item.numVotes !== null && (
                        <span className="inline-flex items-center gap-0.5 text-purple-200/70">
                          <Users className="h-3 w-3" />
                          {formatVotes(item.numVotes)}
                        </span>
                      )}
                      <span className="text-[#FF5CA8]">{item.mediaType}</span>
                      {item.director && <span className="truncate">{item.director}</span>}
                    </div>

                    {hasUser && (
                      <button
                        type="button"
                        onClick={() => onToggleSeen(item.imdbId, seen)}
                        className={`inline-flex shrink-0 items-center gap-1.5 rounded border-2 px-2.5 py-0.5 text-xs font-bold transition ${
                          seen
                            ? 'border-[#39FF14] bg-[#39FF14] text-[#090118]'
                            : 'border-[#FF7A00]/60 bg-[#090118] text-orange-100 hover:border-[#39FF14]'
                        }`}
                      >
                        {seen ? <Check className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                        <span>{seen ? 'Seen' : 'Mark'}</span>
                      </button>
                    )}
                  </div>
                </div>

                <div aria-hidden="true" className="film-cell-rail border-l border-orange-500/35" />
              </div>
            );
          })}
        </div>
      )}
    </section>
  );
}
