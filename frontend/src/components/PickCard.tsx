import { Check, ExternalLink, Eye, Film, History, Star, Ticket, Tv } from 'lucide-react';
import type { MediaItemResponse } from '../types/api';
import { FilmstripReel } from './FilmstripReel';

function FilmReelIcon({ className }: { className?: string }) {
  return (
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
      aria-hidden="true"
    >
      <circle cx="12" cy="12" r="9.5" />
      <circle cx="12" cy="12" r="2.2" />
      <circle cx="12" cy="6.2" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="17.5" cy="10.2" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="15.4" cy="16.7" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="8.6" cy="16.7" r="1.4" fill="currentColor" stroke="none" />
      <circle cx="6.5" cy="10.2" r="1.4" fill="currentColor" stroke="none" />
    </svg>
  );
}

interface PickCardProps {
  canPick: boolean;
  candidates: MediaItemResponse[];
  candidateCount: number;
  loading: boolean;
  spinning: boolean;
  spinKey: number;
  reelWinner: MediaItemResponse | null;
  picked: MediaItemResponse | null;
  pickedIsSeen: boolean;
  spinHistory: MediaItemResponse[];
  hasUser: boolean;
  onPick: () => Promise<void>;
  onSpinComplete: () => void;
  onSelectFromHistory: (item: MediaItemResponse) => void;
  onToggleSeen: (imdbId: string, currentlySeen: boolean) => Promise<void>;
}

export function PickCard({
  canPick,
  candidates,
  candidateCount,
  loading,
  spinning,
  spinKey,
  reelWinner,
  picked,
  pickedIsSeen,
  spinHistory,
  hasUser,
  onPick,
  onSpinComplete,
  onSelectFromHistory,
  onToggleSeen,
}: PickCardProps) {
  const isSeries = picked?.mediaType.toLowerCase().includes('tv');

  return (
    <div className="space-y-5">
      <button
        type="button"
        disabled={!canPick || loading || spinning || candidateCount === 0}
        onClick={onPick}
        className="neo-btn-primary flex w-full items-center justify-center gap-3 px-6 py-4 font-display text-lg font-extrabold tracking-wide disabled:cursor-not-allowed"
      >
        <FilmReelIcon className={`h-6 w-6 ${loading || spinning ? 'animate-spin' : ''}`} />
        <span>
          {loading || spinning
            ? 'Spinning the 35mm Reel...'
            : canPick
              ? 'Spin the reel!'
              : 'Pick a watchlist first'}
        </span>
      </button>

      {spinning && reelWinner && (
        <FilmstripReel
          candidates={candidates}
          winner={reelWinner}
          spinKey={spinKey}
          onSpinComplete={onSpinComplete}
        />
      )}

      {!spinning && picked && (
        <div key={picked.imdbId} className="animate-pop-card rainbow-frame overflow-hidden">
          <div className="ticket-stub p-5 space-y-4">
            <div className="flex items-center justify-between border-b border-purple-500/35 pb-2 font-mono text-[10px] font-bold uppercase tracking-widest text-purple-300/80">
              <span className="inline-flex items-center gap-1.5 text-[#FFE600]">
                <Ticket className="h-3.5 w-3.5" />
                ADMIT ONE &middot; NOW SHOWING
              </span>
              <span>SCREEN 01 &middot; {picked.imdbId}</span>
            </div>

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

            <div className="ticket-perforation pt-4">
              <div className="flex flex-wrap gap-3">
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
                    onClick={() => onToggleSeen(picked.imdbId, pickedIsSeen)}
                    className={`inline-flex flex-1 items-center justify-center gap-2 rounded border-2 px-4 py-2.5 text-xs font-extrabold transition ${
                      pickedIsSeen
                        ? 'border-[#39FF14] bg-[#39FF14] text-[#090118]'
                        : 'border-[#FF7A00]/60 bg-[#090118] text-orange-100 hover:border-[#39FF14] hover:text-[#39FF14]'
                    }`}
                  >
                    {pickedIsSeen ? <Check className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    <span>{pickedIsSeen ? 'Seen' : 'Mark Seen'}</span>
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {spinHistory.length > 0 && (
        <div className="rounded border-2 border-purple-500/50 bg-[#090118]/95 px-3.5 py-2.5 space-y-2 shadow-[0_0_16px_rgba(168,85,247,0.2)]">
          <div className="flex items-center justify-between font-mono text-[10px] font-bold uppercase tracking-widest text-purple-300/80">
            <span className="inline-flex items-center gap-1.5">
              <History className="h-3 w-3 text-[#FF5CA8]" />
              <span>Marquee &middot; Last Spins</span>
            </span>
            <span className="text-[#39FF14]">{spinHistory.length} ON BOARD</span>
          </div>
          <div className="flex flex-wrap gap-1.5">
            {spinHistory.map((item, idx) => {
              const isCurrent = picked?.imdbId === item.imdbId;
              const pocketStyle =
                idx % 2 === 0
                  ? 'border-[#39FF14]/60 text-[#39FF14] hover:bg-[#39FF14]/15'
                  : 'border-[#FF1E8A]/60 text-[#FF5CA8] hover:bg-[#FF1E8A]/15';
              return (
                <button
                  key={`${item.imdbId}-${idx}`}
                  type="button"
                  onClick={() => onSelectFromHistory(item)}
                  className={`inline-flex max-w-[200px] items-center gap-1.5 rounded border px-2 py-1 text-[11px] font-bold transition ${
                    isCurrent
                      ? 'border-[#FFE600] bg-[#FFE600]/15 text-[#FFE600]'
                      : `bg-[#13052B] ${pocketStyle}`
                  }`}
                >
                  <span className="font-mono text-[10px] opacity-80">
                    SLOT {String(idx + 1).padStart(2, '0')}
                  </span>
                  <span className="truncate">{item.title}</span>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
