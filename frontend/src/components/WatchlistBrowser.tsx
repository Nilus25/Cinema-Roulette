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
    <section className="aero-card rounded-3xl p-4 sm:p-5 space-y-3">
      <div className="flex items-center justify-between border-b-2 border-purple-100 pb-2.5">
        <h2 className="font-display text-base font-bold text-purple-800">
          Watchlist Vault{' '}
          <span className="ml-1 rounded-full bg-[#39FF14] px-2.5 py-0.5 text-xs font-extrabold text-green-950">
            {items.length}
          </span>
        </h2>
        {loading && <span className="text-xs font-bold text-pink-500">Loading...</span>}
      </div>

      {items.length === 0 ? (
        <p className="py-6 text-center text-sm font-semibold text-slate-500">
          No matching titles in this watchlist.
        </p>
      ) : (
        <div className="max-h-[460px] divide-y divide-purple-100 overflow-y-auto pr-1">
          {items.map(({ item, seen }) => (
            <div key={item.imdbId} className="flex items-center justify-between gap-3 py-3">
              <div className="min-w-0 space-y-0.5">
                <a
                  href={item.imdbUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex max-w-full items-center gap-1.5 text-sm font-bold text-slate-800 transition hover:text-purple-600"
                >
                  <span className="truncate">{item.title}</span>
                  <ExternalLink className="h-3 w-3 shrink-0 text-purple-400" />
                </a>
                <div className="flex flex-wrap items-center gap-2 text-xs font-semibold text-slate-500">
                  {item.releaseYear && <span>{item.releaseYear}</span>}
                  {item.rating !== null && (
                    <span className="inline-flex items-center gap-0.5 font-extrabold text-orange-500">
                      <Star className="h-3 w-3 fill-orange-400" />
                      {item.rating}
                    </span>
                  )}
                  <span className="text-[#D91B7A]">{item.mediaType}</span>
                  {item.director && <span className="truncate">{item.director}</span>}
                </div>
              </div>

              {hasUser && (
                <button
                  type="button"
                  onClick={() => onToggleSeen(item.imdbId, seen)}
                  className={`inline-flex shrink-0 items-center gap-1.5 rounded-full border-2 px-3 py-1 text-xs font-extrabold transition active:scale-95 ${
                    seen
                      ? 'border-green-600 bg-[#39FF14] text-green-950'
                      : 'border-purple-200 bg-white text-slate-600 hover:border-purple-400'
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
