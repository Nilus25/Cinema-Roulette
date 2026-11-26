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
  return (
    <section className="rounded-2xl border border-slate-800 bg-gradient-to-br from-slate-900 to-slate-950 p-5 space-y-4">
      <button
        type="button"
        disabled={!canPick || loading}
        onClick={onPick}
        className="w-full rounded-xl bg-amber-400 px-5 py-3.5 text-base font-bold text-slate-950 shadow-lg shadow-amber-400/10 transition hover:bg-amber-300 active:scale-[0.99] disabled:opacity-40"
      >
        {loading ? 'Picking...' : '🎲 Pick Random Title'}
      </button>

      {picked && (
        <div className="rounded-xl border border-slate-800 bg-slate-900/90 p-4 space-y-3">
          <div className="flex items-start justify-between gap-3">
            <div>
              <h3 className="text-lg font-bold text-slate-100">{picked.title}</h3>
              <p className="text-xs text-slate-400">
                {[picked.mediaType, picked.releaseYear, picked.director].filter(Boolean).join(' • ')}
              </p>
            </div>
            {picked.rating !== null && (
              <span className="shrink-0 rounded-lg bg-amber-400/15 px-2.5 py-1 text-sm font-bold text-amber-300">
                ★ {picked.rating}
              </span>
            )}
          </div>

          {picked.genres.length > 0 && (
            <div className="flex flex-wrap gap-1.5">
              {picked.genres.map((g) => (
                <span key={g} className="rounded-md bg-slate-800 px-2 py-0.5 text-xs text-slate-300">
                  {g}
                </span>
              ))}
            </div>
          )}

          <div className="flex flex-wrap gap-2 pt-1">
            <a
              href={picked.imdbUrl}
              target="_blank"
              rel="noreferrer"
              className="flex-1 rounded-lg border border-slate-700 px-3 py-2 text-center text-xs font-semibold text-slate-200 hover:bg-slate-800"
            >
              Open on IMDb ↗
            </a>
            {hasUser && (
              <button
                type="button"
                onClick={() => onMarkSeen(picked.imdbId)}
                className="flex-1 rounded-lg bg-emerald-500/20 border border-emerald-500/40 px-3 py-2 text-xs font-semibold text-emerald-300 hover:bg-emerald-500/30"
              >
                ✓ Mark as Seen
              </button>
            )}
          </div>
        </div>
      )}
    </section>
  );
}
