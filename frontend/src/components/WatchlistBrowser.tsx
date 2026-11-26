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
    <section className="neo-panel-orange p-4 space-y-3">
      <div className="flex items-center justify-between border-b-2 border-[#FF7A00]/40 pb-2.5">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-[#FF9E44]">
          <ListVideo className="h-4 w-4" />
          <span>Cinematheque</span>
          <span className="rounded bg-[#39FF14] px-2 py-0.5 text-xs font-extrabold text-[#090118]">
            {items.length}
          </span>
        </div>
        {loading && <span className="text-xs font-bold text-[#FFE600]">Loading...</span>}
      </div>

      {items.length === 0 ? (
        <p className="py-6 text-center text-sm font-semibold text-orange-200/70">
          No matching titles in this watchlist.
        </p>
      ) : (
        <div className="max-h-[420px] space-y-2 overflow-y-auto pr-1">
          {items.map(({ item, seen }) => {
            return (
              <div
                key={item.imdbId}
                className={`film-cell flex items-stretch ${
                  seen ? 'film-cell-seen' : ''
                }`}
              >
                <div aria-hidden="true" className="film-cell-rail border-r border-orange-500/35" />

                <div className="flex min-w-0 flex-1 flex-col gap-1.5 px-3 py-2.5">
                  <a
                    href={item.imdbUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="inline-flex max-w-full items-center gap-1.5 text-sm font-bold text-white transition hover:text-[#39FF14]"
                  >
                    <span className="truncate">{item.title}</span>
                    <ExternalLink className="h-3 w-3 shrink-0 text-[#FF9E44]" />
                  </a>

                  <div className="flex items-end justify-between gap-2">
                    <div className="flex min-w-0 flex-wrap items-center gap-2 text-xs font-semibold text-orange-100/70">
                      {item.releaseYear && <span>{item.releaseYear}</span>}
                      {item.rating !== null && (
                        <span className="inline-flex items-center gap-0.5 font-extrabold text-[#FFE600]">
                          <Star className="h-3 w-3 fill-[#FFE600]" />
                          {item.rating}
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
