import { useEffect, useRef, useState } from 'react';
import { useVirtualizer } from '@tanstack/react-virtual';
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
  totalItems: number;
  hasMore: boolean;
  hasUser: boolean;
  loading: boolean;
  loadingMore: boolean;
  sortBy: SortBy;
  sortDir: SortDirection;
  quickSearch: string;
  onQuickSearchChange: (next: string) => void;
  onChangeSort: (nextSortBy: SortBy, nextSortDir: SortDirection) => void;
  onLoadMore: () => void;
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
  totalItems,
  hasMore,
  hasUser,
  loading,
  loadingMore,
  sortBy,
  sortDir,
  quickSearch,
  onQuickSearchChange,
  onChangeSort,
  onLoadMore,
  onToggleSeen,
}: WatchlistBrowserProps) {
  const [sortMenuOpen, setSortMenuOpen] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [atBottom, setAtBottom] = useState(false);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const scrollParentRef = useRef<HTMLDivElement | null>(null);

  const rowVirtualizer = useVirtualizer({
    count: items.length,
    getScrollElement: () => scrollParentRef.current,
    estimateSize: () => 74,
    gap: 8,
    overscan: 5,
    getItemKey: (index) => items[index]?.item.imdbId ?? index,
  });

  const virtualRows = rowVirtualizer.getVirtualItems();

  useEffect(() => {
    if (!hasMore || loading || loadingMore || virtualRows.length === 0) {
      return;
    }
    const lastVirtual = virtualRows[virtualRows.length - 1];
    if (lastVirtual.index >= items.length - 6) {
      onLoadMore();
    }
  }, [virtualRows, items.length, hasMore, loading, loadingMore, onLoadMore]);

  const updateScrollCue = () => {
    const el = scrollParentRef.current;
    if (!el) return;
    const isNearBottom = el.scrollTop + el.clientHeight >= el.scrollHeight - 8;
    setAtBottom(isNearBottom);
  };

  useEffect(() => {
    updateScrollCue();
  }, [items.length, totalItems]);

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

  const activeSortLabel = SORT_OPTIONS.find((o) => o.value === sortBy)?.label ?? 'Default';

  return (
    <section className="neo-panel-orange p-4 space-y-3">
      <div className="flex items-center justify-between gap-2 border-b-2 border-[#FF7A00]/40 pb-2.5">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-[#FF9E44]">
          <ListVideo className="h-4 w-4" />
          <span>Cinematheque</span>
          <span
            title={`Loaded ${items.length} of ${totalItems}`}
            className="rounded bg-[#39FF14] px-2 py-0.5 text-xs font-extrabold text-[#090118]"
          >
            {totalItems}
          </span>
        </div>

        <div className="flex items-center gap-1.5">
          {loading && <span className="text-[11px] font-bold text-[#FFE600]">Loading...</span>}

          <button
            type="button"
            title="Quick search"
            onClick={() => {
              setSearchOpen((v) => !v);
              if (searchOpen && quickSearch) {
                onQuickSearchChange('');
              }
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
            onChange={(e) => onQuickSearchChange(e.target.value)}
            placeholder="Search all pages (title, original title, director)..."
            className="w-full rounded border border-[#FF7A00]/60 bg-[#090118] pl-8 pr-7 py-1.5 text-xs font-semibold text-white placeholder-orange-200/40 focus:border-[#39FF14] focus:outline-none"
          />
          {quickSearch && (
            <button
              type="button"
              aria-label="Clear quick search"
              onClick={() => onQuickSearchChange('')}
              className="absolute right-2 top-2 text-orange-200/70 hover:text-white"
            >
              <X className="h-3.5 w-3.5" />
            </button>
          )}
        </div>
      )}

      {items.length === 0 ? (
        <p className="py-6 text-center text-sm font-semibold text-orange-200/70">
          {loading ? 'Loading watchlist...' : 'No matching titles in this watchlist.'}
        </p>
      ) : (
        <div className="space-y-1.5">
          <div className="relative">
            <div
              ref={scrollParentRef}
              onScroll={updateScrollCue}
              className="scrollbar-none max-h-[420px] overflow-y-auto"
            >
              <div
                style={{
                  height: `${rowVirtualizer.getTotalSize()}px`,
                  width: '100%',
                  position: 'relative',
                }}
              >
                {virtualRows.map((virtualRow) => {
                  const { item, seen } = items[virtualRow.index];
                  const showOriginal = Boolean(
                    item.originalTitle && item.originalTitle.toLowerCase() !== item.title.toLowerCase(),
                  );
                  return (
                    <div
                      key={item.imdbId}
                      ref={rowVirtualizer.measureElement}
                      data-index={virtualRow.index}
                      style={{
                        position: 'absolute',
                        top: 0,
                        left: 0,
                        width: '100%',
                        transform: `translateY(${virtualRow.start}px)`,
                      }}
                    >
                      <div className={`film-cell flex items-stretch ${seen ? 'film-cell-seen' : ''}`}>
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
                    </div>
                  );
                })}
              </div>
            </div>

            {(hasMore || !atBottom) && (
              <div
                aria-hidden="true"
                className="pointer-events-none absolute inset-x-0 bottom-0 h-10 bg-gradient-to-t from-[#1E0B07] via-[#1E0B07]/70 to-transparent"
              />
            )}
          </div>

          {loadingMore ? (
            <p className="py-1 text-center font-mono text-[11px] font-bold text-[#FFE600]">
              Loading next reel...
            </p>
          ) : (
            (hasMore || !atBottom) && (
              <div className="flex items-center justify-center gap-1 pt-0.5 font-mono text-[10px] font-bold text-orange-200/65">
                <ChevronDown className="h-3 w-3 animate-bounce text-[#FF9E44]" />
                <span>
                  Scroll for more ({items.length} / {totalItems})
                </span>
              </div>
            )
          )}
        </div>
      )}
    </section>
  );
}
