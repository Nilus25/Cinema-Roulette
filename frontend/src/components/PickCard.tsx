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
        className="neon-arcade-btn flex w-full items-center justify-center gap-3 px-6 py-4 font-display text-base sm:text-lg font-bold uppercase tracking-wider disabled:cursor-not-allowed"
      >
        <Dices className={`h-6 w-6 ${loading ? 'animate-spin' : ''}`} />
        <span>{loading ? 'Spinning...' : 'Roll the Pick'}</span>
      </button>

      {picked && (
        <div className="border-2 border-[#39FF14] bg-[#05010D] p-5 space-y-4 shadow-[0_0_24px_rgba(57,255,20,0.35),inset_0_0_20px_rgba(168,85,247,0.25)]">
          <div className="flex items-start justify-between gap-3">
            <div className="space-y-1.5">
              <div className="inline-flex items-center gap-1.5 border border-[#FF007F] bg-[#FF007F]/20 px-2.5 py-0.5 text-[11px] font-bold uppercase text-[#FF5CA0]">
                {isSeries ? <Tv className="h-3 w-3" /> : <Film className="h-3 w-3" />}
                <span>{picked.mediaType}</span>
                {picked.releaseYear && <span>// {picked.releaseYear}</span>}
              </div>
              <h3 className="font-display text-xl sm:text-2xl font-bold text-[#39FF14] drop-shadow-[0_0_10px_rgba(57,255,20,0.5)]">
                {picked.title}
              </h3>
              {picked.director && (
                <p className="text-xs uppercase tracking-wide text-purple-300">DIR: {picked.director}</p>
              )}
            </div>

            {picked.rating !== null && (
              <div className="flex shrink-0 items-center gap-1.5 border-2 border-[#FF7A59] bg-[#FF7A59]/15 px-3 py-1.5 text-sm font-bold text-[#FF9E44] shadow-[0_0_12px_rgba(255,122,89,0.5)]">
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
                  className="border border-purple-500/60 bg-[#14072A] px-2.5 py-0.5 text-[11px] uppercase text-purple-200"
                >
                  {g}
                </span>
              ))}
            </div>
          )}

          <div className="flex flex-wrap gap-3 pt-1">
            <a
              href={picked.imdbUrl}
              target="_blank"
              rel="noreferrer"
              className="inline-flex flex-1 items-center justify-center gap-2 border-2 border-[#FF7A59] bg-[#0C041A] px-4 py-2.5 text-xs font-bold uppercase text-[#FF9E44] shadow-[0_0_12px_rgba(255,122,89,0.3)] hover:bg-[#FF7A59] hover:text-black"
            >
              <span>Open IMDb</span>
              <ExternalLink className="h-3.5 w-3.5" />
            </a>
            {hasUser && (
              <button
                type="button"
                onClick={() => onMarkSeen(picked.imdbId)}
                className="inline-flex flex-1 items-center justify-center gap-2 border-2 border-[#39FF14] bg-[#39FF14]/15 px-4 py-2.5 text-xs font-bold uppercase text-[#39FF14] shadow-[0_0_12px_rgba(57,255,20,0.35)] hover:bg-[#39FF14] hover:text-black"
              >
                <Check className="h-4 w-4" />
                <span>Mark Seen</span>
              </button>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
