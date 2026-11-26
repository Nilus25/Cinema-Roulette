import { Check, Disc3, ExternalLink, Film, Star, Tv } from 'lucide-react';
import type { MediaItemResponse } from '../types/api';

interface PickCardProps {
  canPick: boolean;
  candidateCount: number;
  loading: boolean;
  picked: MediaItemResponse | null;
  hasUser: boolean;
  onPick: () => Promise<void>;
  onMarkSeen: (imdbId: string) => Promise<void>;
}

export function PickCard({
  canPick,
  candidateCount,
  loading,
  picked,
  hasUser,
  onPick,
  onMarkSeen,
}: PickCardProps) {
  const isSeries = picked?.mediaType.toLowerCase().includes('tv');

  return (
    <div className="space-y-5">
      <button
        type="button"
        disabled={!canPick || loading || candidateCount === 0}
        onClick={onPick}
        className="neo-btn-primary flex w-full items-center justify-center gap-3 px-6 py-4 font-display text-lg font-extrabold tracking-wide disabled:cursor-not-allowed"
      >
        <Disc3 className={`h-6 w-6 ${loading ? 'animate-spin' : ''}`} />
        <span>
          {loading
            ? 'Spinning...'
            : canPick
              ? `Spin from ${candidateCount} ${candidateCount === 1 ? 'title' : 'titles'}!`
              : 'Pick a watchlist first'}
        </span>
      </button>

      {picked && (
        <div key={picked.imdbId} className="animate-pop-card rainbow-frame">
          <div className="rounded-[5px] bg-[#090118] p-5 space-y-4">
            <div className="flex items-start justify-between gap-3">
              <div className="space-y-1.5">
                <div className="inline-flex items-center gap-1.5 rounded border border-[#FF1E8A] bg-[#FF1E8A]/20 px-2.5 py-0.5 text-xs font-bold text-[#FF72B6]">
                  {isSeries ? <Tv className="h-3.5 w-3.5" /> : <Film className="h-3.5 w-3.5" />}
                  <span>{picked.mediaType}</span>
                  {picked.releaseYear && <span>• {picked.releaseYear}</span>}
                </div>
                <h3 className="font-display text-2xl font-extrabold text-[#39FF14] drop-shadow-[0_0_12px_rgba(57,255,20,0.55)]">
                  {picked.title}
                </h3>
                {picked.director && (
                  <p className="text-xs font-semibold text-purple-200/85">Directed by {picked.director}</p>
                )}
              </div>

              {picked.rating !== null && (
                <div className="flex shrink-0 items-center gap-1 rounded border-2 border-[#FF7A00] bg-[#FF7A00] px-3 py-1 text-sm font-extrabold text-[#090118] shadow-[3px_3px_0px_#FFE600]">
                  <Star className="h-4 w-4 fill-[#090118]" />
                  <span>{picked.rating}</span>
                </div>
              )}
            </div>

            {picked.genres.length > 0 && (
              <div className="flex flex-wrap gap-1.5">
                {picked.genres.map((g) => (
                  <span
                    key={g}
                    className="rounded border border-purple-400/50 bg-[#1B0938] px-2.5 py-0.5 text-xs font-semibold text-purple-100"
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
                className="inline-flex flex-1 items-center justify-center gap-2 rounded border-2 border-purple-400 bg-[#14062B] px-4 py-2.5 text-xs font-extrabold text-purple-100 transition hover:border-[#FF7A00] hover:text-[#FF9E44]"
              >
                <span>Open IMDb</span>
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
              {hasUser && (
                <button
                  type="button"
                  onClick={() => onMarkSeen(picked.imdbId)}
                  className="inline-flex flex-1 items-center justify-center gap-2 rounded border-2 border-[#39FF14] bg-[#39FF14]/15 px-4 py-2.5 text-xs font-extrabold text-[#39FF14] transition hover:bg-[#39FF14] hover:text-[#090118]"
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
