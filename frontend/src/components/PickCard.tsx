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
    <div className="space-y-4">
      <button
        type="button"
        disabled={!canPick || loading}
        onClick={onPick}
        className="neon-green-glow flex w-full items-center justify-center gap-2.5 rounded-2xl bg-[#39FF14] px-6 py-4 font-display text-base font-bold tracking-wide text-[#07120A] transition active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-35 disabled:shadow-none"
      >
        <Dices className={`h-5 w-5 ${loading ? 'animate-spin' : ''}`} />
        <span>{loading ? 'Rolling...' : 'Roll the Pick'}</span>
      </button>

      {picked && (
        <div className="rounded-2xl border border-purple-500/45 bg-gradient-to-br from-[#1C1033] via-[#150C26] to-[#1A0B24] p-5 space-y-4 shadow-[0_0_30px_rgba(168,85,247,0.18)]">
          <div className="flex items-start justify-between gap-3">
            <div className="space-y-1">
              <div className="inline-flex items-center gap-1.5 rounded-full border border-[#FF2A85]/40 bg-[#FF2A85]/15 px-2.5 py-0.5 text-[11px] font-semibold text-[#FF6BB0]">
                {isSeries ? <Tv className="h-3 w-3" /> : <Film className="h-3 w-3" />}
                <span>{picked.mediaType}</span>
                {picked.releaseYear && <span>• {picked.releaseYear}</span>}
              </div>
              <h3 className="font-display text-xl font-bold text-white leading-snug">{picked.title}</h3>
              {picked.director && (
                <p className="text-xs text-purple-200/80">Directed by {picked.director}</p>
              )}
            </div>

            {picked.rating !== null && (
              <div className="flex shrink-0 items-center gap-1 rounded-xl border border-[#FF9E44]/40 bg-[#FF9E44]/15 px-3 py-1.5 text-sm font-bold text-[#FF9E44]">
                <Star className="h-4 w-4 fill-[#FF9E44]" />
                <span>{picked.rating}</span>
              </div>
            )}
          </div>

          {picked.genres.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {picked.genres.map((g) => (
                <span
                  key={g}
                  className="rounded-full border border-[#3E2468] bg-[#0B0614]/80 px-2.5 py-0.5 text-xs text-purple-200"
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
              className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl border border-purple-500/50 bg-[#0B0614] px-4 py-2.5 text-xs font-semibold text-purple-100 transition hover:border-[#FF7A59] hover:text-[#FF9E44]"
            >
              <span>Open IMDb</span>
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
            {hasUser && (
              <button
                type="button"
                onClick={() => onMarkSeen(picked.imdbId)}
                className="inline-flex flex-1 items-center justify-center gap-2 rounded-xl border border-[#39FF14]/50 bg-[#39FF14]/15 px-4 py-2.5 text-xs font-bold text-[#39FF14] transition hover:bg-[#39FF14]/25"
              >
                <Check className="h-4 w-4" />
                <span>Mark as Seen</span>
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
