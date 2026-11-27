import { Check, Clapperboard, LogOut, Popcorn, Settings, Sparkles } from 'lucide-react';
import type { MediaItemResponse } from '../types/api';

interface ProfileStats {
  totalSeen: number;
  seenInWatchlist: number;
  watchlistTotal: number;
  topGenre: string | null;
}

interface ProfileBarProps {
  username: string;
  onOpenAccount: () => void;
  onLogout: () => Promise<void>;
  lastGeneratedPrompt: MediaItemResponse | null;
  recentSeen: MediaItemResponse[];
  onMarkLastSeen: (imdbId: string) => Promise<void>;
  onDismissPrompt: () => void;
  stats: ProfileStats;
}

export function ProfileBar({
  username,
  onOpenAccount,
  onLogout,
  lastGeneratedPrompt,
  recentSeen,
  onMarkLastSeen,
  onDismissPrompt,
  stats,
}: ProfileBarProps) {
  const progressPct =
    stats.watchlistTotal > 0 ? Math.round((stats.seenInWatchlist / stats.watchlistTotal) * 100) : 0;

  return (
    <div className="space-y-3">
      <div className="neo-panel-green p-4 space-y-3">
        <div className="flex items-center justify-between gap-2 border-b border-[#39FF14]/30 pb-2">
          <div className="flex items-center gap-2 font-display text-sm font-bold text-[#39FF14] min-w-0">
            <Popcorn className="h-4 w-4 shrink-0 text-[#39FF14]" />
            <span className="truncate">{username}</span>
          </div>
          <div className="flex items-center gap-1.5 shrink-0">
            <button
              type="button"
              onClick={onOpenAccount}
              title="Account & Backup"
              aria-label="Account & Backup"
              className="inline-flex items-center justify-center rounded border border-[#39FF14]/50 bg-[#090118] p-1 text-green-200 transition hover:border-[#00E5FF] hover:text-[#00E5FF]"
            >
              <Settings className="h-3.5 w-3.5" />
            </button>
            <button
              type="button"
              onClick={onLogout}
              title="Sign out"
              className="inline-flex items-center gap-1 rounded border border-[#39FF14]/50 bg-[#090118] px-2 py-0.5 text-[11px] font-bold text-green-200 transition hover:border-[#FF2E54] hover:text-[#FF2E54]"
            >
              <LogOut className="h-3 w-3" />
              <span>Logout</span>
            </button>
          </div>
        </div>

        <div className="space-y-2.5 text-xs">
          <div className="grid grid-cols-2 gap-2">
            <div className="rounded border border-[#39FF14]/35 bg-[#090118]/80 p-2">
              <p className="text-[10px] uppercase tracking-wider text-green-300/70">Total Seen</p>
              <p className="font-display text-lg font-extrabold text-[#39FF14]">{stats.totalSeen}</p>
            </div>
            <div className="rounded border border-[#39FF14]/35 bg-[#090118]/80 p-2">
              <p className="text-[10px] uppercase tracking-wider text-green-300/70">Top Genre</p>
              <p className="truncate font-display text-sm font-bold text-white pt-0.5">
                {stats.topGenre ?? '—'}
              </p>
            </div>
          </div>

          {stats.watchlistTotal > 0 && (
            <div className="space-y-1">
              <div className="flex justify-between text-[11px] font-semibold text-green-200/80">
                <span>List Progress</span>
                <span className="text-[#39FF14]">
                  {stats.seenInWatchlist}/{stats.watchlistTotal} ({progressPct}%)
                </span>
              </div>
              <div className="h-2 w-full overflow-hidden rounded border border-[#39FF14]/40 bg-[#090118]">
                <div
                  className="h-full bg-[#39FF14] transition-all duration-300"
                  style={{ width: `${progressPct}%` }}
                />
              </div>
            </div>
          )}

          {recentSeen.length > 0 && (
            <div className="border-t border-[#39FF14]/25 pt-2 space-y-1.5">
              <div className="flex items-center gap-1.5 font-mono text-[10px] font-bold uppercase tracking-wider text-green-300/75">
                <Clapperboard className="h-3 w-3 text-[#39FF14]" />
                <span>Recently Screened</span>
              </div>
              <div className="space-y-1">
                {recentSeen.map((item) => (
                  <a
                    key={item.imdbId}
                    href={item.imdbUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center justify-between gap-2 rounded border border-[#39FF14]/25 bg-[#090118]/80 px-2 py-1 text-[11px] font-semibold text-green-100 transition hover:border-[#39FF14] hover:text-[#39FF14]"
                  >
                    <span className="truncate">{item.title}</span>
                    <span className="shrink-0 font-mono text-[10px] text-green-300/70">
                      {item.releaseYear ?? '—'}
                    </span>
                  </a>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {lastGeneratedPrompt && (
        <div className="animate-pop-card flex flex-col gap-2.5 rounded-md border-2 border-[#FF7A00] bg-[#240D02] px-3.5 py-3 text-xs font-semibold text-orange-100 shadow-[4px_4px_0px_#FF7A00]">
          <div className="flex items-start gap-2">
            <Sparkles className="h-4 w-4 shrink-0 text-[#FFE600] mt-0.5" />
            <span>
              From your last session:{' '}
              <a
                href={lastGeneratedPrompt.imdbUrl}
                target="_blank"
                rel="noreferrer"
                className="font-bold text-[#39FF14] underline decoration-[#39FF14]/50 underline-offset-2 transition hover:text-[#FFE600] hover:decoration-[#FFE600]"
              >
                {lastGeneratedPrompt.title}
              </a>
              {lastGeneratedPrompt.releaseYear ? ` (${lastGeneratedPrompt.releaseYear})` : ''}. Watched it?
            </span>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => onMarkLastSeen(lastGeneratedPrompt.imdbId)}
              className="inline-flex flex-1 items-center justify-center gap-1 rounded border-2 border-[#39FF14] bg-[#39FF14] px-2.5 py-1 font-extrabold text-[#090118]"
            >
              <Check className="h-3.5 w-3.5" />
              <span>Seen</span>
            </button>
            <button
              type="button"
              onClick={onDismissPrompt}
              className="rounded border-2 border-purple-500/70 bg-[#14062B] px-2.5 py-1 text-purple-200 hover:border-white"
            >
              Skip
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
