import type { WatchlistItemResponse } from '../types/api';

interface WatchlistBrowserProps {
  items: WatchlistItemResponse[];
  hasUser: boolean;
  loading: boolean;
  onToggleSeen: (imdbId: string, currentlySeen: boolean) => Promise<void>;
}

export function WatchlistBrowser({ items, hasUser, loading, onToggleSeen }: WatchlistBrowserProps) {
  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-4 space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">
          Matching Titles ({items.length})
        </h2>
        {loading && <span className="text-xs text-slate-500">Loading...</span>}
      </div>

      {items.length === 0 ? (
        <p className="text-sm text-slate-500 py-4 text-center">No matching titles in this watchlist.</p>
      ) : (
        <div className="divide-y divide-slate-800/80 max-h-[480px] overflow-y-auto pr-1">
          {items.map(({ item, seen }) => (
            <div key={item.imdbId} className="py-3 flex items-center justify-between gap-3">
              <div className="min-w-0">
                <a
                  href={item.imdbUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="block truncate text-sm font-medium text-slate-100 hover:text-amber-300"
                >
                  {item.title}
                </a>
                <p className="truncate text-xs text-slate-400">
                  {[
                    item.releaseYear,
                    item.rating !== null ? `★ ${item.rating}` : null,
                    item.mediaType,
                    item.director,
                  ]
                    .filter(Boolean)
                    .join(' • ')}
                </p>
              </div>
              {hasUser && (
                <button
                  type="button"
                  onClick={() => onToggleSeen(item.imdbId, seen)}
                  className={`shrink-0 rounded-lg px-2.5 py-1.5 text-xs font-semibold transition ${
                    seen
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/40'
                      : 'bg-slate-800 text-slate-300 hover:bg-slate-700'
                  }`}
                >
                  {seen ? '✓ Seen' : 'Mark Seen'}
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
