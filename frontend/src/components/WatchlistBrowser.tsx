import { Check, ExternalLink, Eye, ListVideo, Star } from 'lucide-react';
import type { WatchlistItemResponse } from '../types/api';

interface WatchlistBrowserProps {
  items: WatchlistItemResponse[];
  hasUser: boolean;
  loading: boolean;
  onToggleSeen: (imdbId: string, currentlySeen: boolean) => Promise<void>;
}

export function WatchlistBrowser({ items, hasUser, loading, onToggleSeen }: WatchlistBrowserProps) {
  return (
    <section className="neo-panel-purple p-4 space-y-3">
      <div className="flex items-center justify-between border-b-2 border-purple-500/40 pb-2.5">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-purple-200">
          <ListVideo className="h-4 w-4 text-[#FF1E8A]" />
          <span>Watchlist Vault</span>
          <span className="rounded bg-[#39FF14] px-2 py-0.5 text-xs font-extrabold text-[#090118]">
            {items.length}
          </span>
        </div>
        {loading && <span className="text-xs font-bold text-[#FF9E44]">Loading...</span>}
      </div>

      {items.length === 0 ? (
        <p className="py-6 text-center text-sm font-semibold text-purple-300/70">
          No matching titles in this watchlist.
        </p>
      ) : (
        <div className="max-h-[450px] divide-y divide-purple-800/60 overflow-y-auto pr-1">
          {items.map(({ item, seen }) => (
            <div key={item.imdbId} className="flex items-center justify-between gap-3 py-3">
              <div className="min-w-0 space-y-0.5">
                <a
                  href={item.imdbUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex max-w-full items-center gap-1.5 text-sm font-bold text-white transition hover:text-[#39FF14]"
                >
                  <span className="truncate">{item.title}</span>
                  <ExternalLink className="h-3 w-3 shrink-0 text-purple-400" />
                </a>
                <div className="flex flex-wrap items-center gap-2 text-xs font-semibold text-purple-200/70">
                  {item.releaseYear && <span>{item.releaseYear}</span>}
                  {item.rating !== null && (
                    <span className="inline-flex items-center gap-0.5 font-extrabold text-[#FF9E44]">
                      <Star className="h-3 w-3 fill-[#FF9E44]" />
                      {item.rating}
                    </span>
                  )}
                  <span className="text-[#FF5CA8]">{item.mediaType}</span>
                  {item.director && <span className="truncate">{item.director}</span>}
                </div>
              </div>

              {hasUser && (
                <button
                  type="button"
                  onClick={() => onToggleSeen(item.imdbId, seen)}
                  className={`inline-flex shrink-0 items-center gap-1.5 rounded border-2 px-3 py-1 text-xs font-bold transition ${
                    seen
                      ? 'border-[#39FF14] bg-[#39FF14] text-[#090118]'
                      : 'border-purple-500/60 bg-[#090118] text-purple-100 hover:border-[#39FF14]'
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
