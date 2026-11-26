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
        className="swing-btn-neon flex w-full items-center justify-center gap-3 px-6 py-4 font-display text-base sm:text-lg font-bold uppercase tracking-wider disabled:cursor-not-allowed"
      >
        <Dices className={`h-6 w-6 ${loading ? 'animate-spin' : ''}`} />
        <span>{loading ? 'ROLLING...' : 'ROLL THE PICK!'}</span>
      </button>

      {picked && (
        <div key={picked.imdbId} className="swing-window bg-[#120428]">
          <div className="rainbow-strip h-2 w-full" />
          <div className="p-4 space-y-3.5">
            <div className="flex items-start justify-between gap-3">
              <div className="space-y-1">
                <div className="inline-flex items-center gap-1.5 border border-[#FF0080] bg-[#FF0080]/20 px-2 py-0.5 text-[11px] font-bold uppercase text-[#FF6BB0]">
                  {isSeries ? <Tv className="h-3 w-3" /> : <Film className="h-3 w-3" />}
                  <span>{picked.mediaType}</span>
                  {picked.releaseYear && <span>// {picked.releaseYear}</span>}
                </div>
                <h3 className="font-display text-lg sm:text-2xl font-bold text-[#39FF14] drop-shadow-[0_0_12px_rgba(57,255,20,0.65)]">
                  {picked.title}
                </h3>
                {picked.director && (
                  <p className="text-xs uppercase text-purple-300">DIR: {picked.director}</p>
                )}
              </div>

              {picked.rating !== null && (
                <div className="swing-btn flex shrink-0 items-center gap-1 bg-[#FF6B00] px-2.5 py-1 text-sm font-bold text-black shadow-[0_0_12px_rgba(255,107,0,0.7)]">
                  <Star className="h-4 w-4 fill-black" />
                  <span>{picked.rating}</span>
                </div>
              )}
            </div>

            {picked.genres.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {picked.genres.map((g) => (
                  <span
                    key={g}
                    className="border border-purple-400/60 bg-[#250C4A] px-2 py-0.5 text-[11px] uppercase text-purple-100"
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
                className="swing-btn inline-flex flex-1 items-center justify-center gap-2 bg-[#7B2CBF] px-4 py-2 text-xs font-bold uppercase text-white hover:bg-[#9D4EDD]"
              >
                <span>Open IMDb</span>
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
              {hasUser && (
                <button
                  type="button"
                  onClick={() => onMarkSeen(picked.imdbId)}
                  className="swing-btn inline-flex flex-1 items-center justify-center gap-2 bg-[#39FF14] px-4 py-2 text-xs font-bold uppercase text-black"
                >
                  <Check className="h-4 w-4" />
                  <span>Mark Seen</span>
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
