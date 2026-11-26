import { Check, ExternalLink, Eye, Star } from 'lucide-react';
import type { WatchlistItemResponse } from '../types/api';

interface WatchlistBrowserProps {
  items: WatchlistItemResponse[];
  hasUser: boolean;
  loading: boolean;
  onToggleSeen: (imdbId: string, currentlySeen: boolean) => Promise<void>;
}

export function WatchlistBrowser({ items, hasUser, loading, onToggleSeen }: WatchlistBrowserProps) {
  return (
    <section className="rounded-2xl border border-[#311B52] bg-[#150C26]/90 p-4 space-y-3">
      <div className="flex items-center justify-between">
        <h2 className="font-display text-sm font-bold uppercase tracking-wider text-purple-300">
          Watchlist Vault <span className="text-[#39FF14]">({items.length})</span>
        </h2>
        {loading && <span className="text-xs text-purple-300/70">Updating...</span>}
      </div>

      {items.length === 0 ? (
        <p className="py-6 text-center text-sm text-slate-400">
          No matching titles in this watchlist.
        </p>
      ) : (
        <div className="max-h-[460px] divide-y divide-[#2B174A] overflow-y-auto pr-1">
          {items.map(({ item, seen }) => (
            <div key={item.imdbId} className="flex items-center justify-between gap-3 py-3">
              <div className="min-w-0 space-y-0.5">
                <a
                  href={item.imdbUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex max-w-full items-center gap-1.5 text-sm font-medium text-slate-100 transition hover:text-[#39FF14]"
                >
                  <span className="truncate">{item.title}</span>
                  <ExternalLink className="h-3 w-3 shrink-0 text-slate-500" />
                </a>
                <div className="flex flex-wrap items-center gap-2 text-xs text-slate-400">
                  {item.releaseYear && <span>{item.releaseYear}</span>}
                  {item.rating !== null && (
                    <span className="inline-flex items-center gap-0.5 font-medium text-[#FF9E44]">
                      <Star className="h-3 w-3 fill-[#FF9E44]" />
                      {item.rating}
                    </span>
                  )}
                  <span className="text-purple-300/80">{item.mediaType}</span>
                  {item.director && <span className="truncate">{item.director}</span>}
                </div>
              </div>

              {hasUser && (
                <button
                  type="button"
                  onClick={() => onToggleSeen(item.imdbId, seen)}
                  className={`inline-flex shrink-0 items-center gap-1.5 rounded-xl px-3 py-1.5 text-xs font-semibold transition ${
                    seen
                      ? 'border border-[#39FF14]/50 bg-[#39FF14]/15 text-[#39FF14]'
                      : 'border border-[#3E2468] bg-[#0B0614] text-slate-300 hover:border-purple-400'
                  }`}
                >
                  {seen ? <Check className="h-3.5 w-3.5" /> : <Eye className="h-3.5 w-3.5" />}
                  <span>{seen ? 'Seen' : 'Mark'}</span>
                </button>
              )}
            </div>
          ))}
        </div>
      )}
    </section>
  );
}
