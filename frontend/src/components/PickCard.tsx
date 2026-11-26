import { Check, Dices, ExternalLink, Film, Star, Tv } from 'lucide-react';
import type { MediaItemResponse } from '../types/api';

interface PickCardProps {
  canPick: boolean;
  loading: boolean;
  picked: MediaItemResponse | null;
  hasUser: boolean;
  onPick: () => Promise<void>;
  onMarkSeen: (imdbId: string) => Promise<void>;
}

export function PickCard({ canPick, loading, picked, hasUser, onPick, onMarkSeen }: PickCardProps) {
  const isSeries = picked?.mediaType.toLowerCase().includes('tv');

  return (
    <div className="space-y-5">
      <button
        type="button"
        disabled={!canPick || loading}
        onClick={onPick}
        className="gel-btn-green flex w-full items-center justify-center gap-3 rounded-full px-6 py-4 font-display text-lg font-bold tracking-wide disabled:cursor-not-allowed"
      >
        <Dices className={`h-6 w-6 ${loading ? 'animate-spin' : ''}`} />
        <span>{loading ? 'Rolling the Rainbow...' : 'Roll the Pick!'}</span>
      </button>

      {picked && (
        <div
          key={picked.imdbId}
          className="animate-pop-in overflow-hidden rounded-3xl border-3 border-purple-500 bg-white shadow-[0_14px_30px_rgba(168,85,247,0.28)]"
        >
          <div className="rainbow-bar h-2 w-full" />
          <div className="p-5 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div className="space-y-1.5">
                <div className="inline-flex items-center gap-1.5 rounded-full bg-purple-100 px-3 py-0.5 text-xs font-extrabold text-purple-700">
                  {isSeries ? <Tv className="h-3.5 w-3.5" /> : <Film className="h-3.5 w-3.5" />}
                  <span>{picked.mediaType}</span>
                  {picked.releaseYear && <span>• {picked.releaseYear}</span>}
                </div>
                <h3 className="font-display text-2xl font-bold text-slate-900 leading-tight">
                  {picked.title}
                </h3>
                {picked.director && (
                  <p className="text-xs font-semibold text-slate-500">Directed by {picked.director}</p>
                )}
              </div>

              {picked.rating !== null && (
                <div className="flex shrink-0 items-center gap-1 rounded-2xl border-2 border-orange-400 bg-gradient-to-b from-amber-300 to-orange-400 px-3 py-1.5 text-sm font-extrabold text-orange-950 shadow-sm">
                  <Star className="h-4 w-4 fill-orange-950" />
                  <span>{picked.rating}</span>
                </div>
              )}
            </div>

            {picked.genres.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {picked.genres.map((g) => (
                  <span
                    key={g}
                    className="rounded-full border border-pink-300 bg-pink-50 px-3 py-0.5 text-xs font-bold text-[#D91B7A]"
                  >
                    {g}
                  </span>
                ))}
              </div>
            )}

            <div className="flex flex-wrap gap-2.5 pt-1">
              <a
                href={picked.imdbUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex flex-1 items-center justify-center gap-2 rounded-full border-2 border-purple-500 bg-purple-600 px-4 py-2.5 text-xs font-extrabold text-white shadow-sm transition hover:bg-purple-500 active:scale-95"
              >
                <span>Open IMDb</span>
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
              {hasUser && (
                <button
                  type="button"
                  onClick={() => onMarkSeen(picked.imdbId)}
                  className="inline-flex flex-1 items-center justify-center gap-2 rounded-full border-2 border-green-600 bg-[#39FF14] px-4 py-2.5 text-xs font-extrabold text-green-950 shadow-sm transition hover:brightness-105 active:scale-95"
                >
                  <Check className="h-4 w-4" />
                  <span>Mark as Seen</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
