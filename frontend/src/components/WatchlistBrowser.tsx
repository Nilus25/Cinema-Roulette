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
    <section className="neon-box-purple bg-[#0C041A]/95 p-4 space-y-3">
      <div className="flex items-center justify-between border-b border-purple-500/40 pb-2">
        <h2 className="font-display text-xs sm:text-sm uppercase tracking-wider text-purple-300">
          Watchlist Vault <span className="text-[#39FF14]">[{items.length}]</span>
        </h2>
        {loading && <span className="text-xs uppercase text-[#FF7A59]">Loading...</span>}
      </div>

      {items.length === 0 ? (
        <p className="py-6 text-center text-xs uppercase text-slate-400">
          No matching titles in this watchlist.
        </p>
      ) : (
        <div className="max-h-[460px] divide-y divide-purple-900/60 overflow-y-auto pr-1">
          {items.map(({ item, seen }) => (
            <div key={item.imdbId} className="flex items-center justify-between gap-3 py-3">
              <div className="min-w-0 space-y-0.5">
                <a
                  href={item.imdbUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex max-w-full items-center gap-1.5 text-xs sm:text-sm font-bold text-slate-100 hover:text-[#39FF14]"
                >
                  <span className="truncate">{item.title}</span>
                  <ExternalLink className="h-3 w-3 shrink-0 text-purple-400" />
                </a>
                <div className="flex flex-wrap items-center gap-2 text-[11px] text-slate-400">
                  {item.releaseYear && <span>{item.releaseYear}</span>}
                  {item.rating !== null && (
                    <span className="inline-flex items-center gap-0.5 font-bold text-[#FF9E44]">
                      <Star className="h-3 w-3 fill-[#FF9E44]" />
                      {item.rating}
                    </span>
                  )}
                  <span className="uppercase text-[#FF5CA0]">{item.mediaType}</span>
                  {item.director && <span className="truncate text-purple-300/75">{item.director}</span>}
                </div>
              </div>

              {hasUser && (
                <button
                  type="button"
                  onClick={() => onToggleSeen(item.imdbId, seen)}
                  className={`inline-flex shrink-0 items-center gap-1.5 border px-2.5 py-1 text-[11px] font-bold uppercase transition ${
                    seen
                      ? 'border-[#39FF14] bg-[#39FF14]/20 text-[#39FF14] shadow-[0_0_8px_rgba(57,255,20,0.4)]'
                      : 'border-purple-500/50 bg-[#05010D] text-slate-300 hover:border-[#39FF14]'
                  }`}
                >
                  {seen ? <Check className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
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
