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
    <section className="swing-window">
      <div className="swing-titlebar-purple flex items-center justify-between px-2.5 py-1.5 text-white">
        <div className="flex items-center gap-1.5 font-display text-xs font-bold uppercase">
          <ListVideo className="h-3.5 w-3.5" />
          <span>Watchlist_Vault [{items.length}]</span>
        </div>
        {loading && <span className="text-[11px] font-bold uppercase text-[#FFE600]">Loading...</span>}
      </div>

      <div className="p-3">
        {items.length === 0 ? (
          <p className="swing-inset py-6 text-center text-xs uppercase text-purple-300">
            No matching titles in this watchlist.
          </p>
        ) : (
          <div className="swing-inset max-h-[440px] divide-y divide-purple-900/70 overflow-y-auto p-2">
            {items.map(({ item, seen }) => (
              <div key={item.imdbId} className="flex items-center justify-between gap-3 py-2.5 px-1">
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
                    className={`swing-btn inline-flex shrink-0 items-center gap-1 px-2.5 py-1 text-[11px] font-bold uppercase ${
                      seen ? 'bg-[#39FF14] text-black' : 'bg-[#2B0F54] text-slate-200'
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
      </div>
    </section>
  );
}
