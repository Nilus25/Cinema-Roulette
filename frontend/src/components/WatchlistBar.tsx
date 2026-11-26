import { useRef, useState } from 'react';
import {
  Copy,
  Film,
  Globe,
  Link2,
  Lock,
  Plus,
  RefreshCw,
  Trash2,
  Upload,
  Users,
  X,
} from 'lucide-react';
import type { WatchlistResponse, WatchlistVisibility } from '../types/api';

interface WatchlistStats {
  total: number;
  unseenCount: number;
  avgRating: string | null;
  topGenre: string | null;
}

interface WatchlistBarProps {
  watchlists: WatchlistResponse[];
  publicWatchlists: WatchlistResponse[];
  selectedWatchlistId: string | null;
  onSelectWatchlist: (id: string | null) => void;
  onImportWatchlist: (name: string, file: File) => Promise<void>;
  onUpdateWatchlistCsv: (id: string, file: File) => Promise<void>;
  onChangeVisibility: (id: string, visibility: WatchlistVisibility) => Promise<void>;
  onSubscribeByCode: (shareCode: string) => Promise<void>;
  onSubscribePublic: (id: string) => Promise<void>;
  onUnsubscribe: (id: string) => Promise<void>;
  onDeleteWatchlist: (id: string) => Promise<void>;
  stats: WatchlistStats | null;
}

export function WatchlistBar({
  watchlists,
  publicWatchlists,
  selectedWatchlistId,
  onSelectWatchlist,
  onImportWatchlist,
  onUpdateWatchlistCsv,
  onChangeVisibility,
  onSubscribeByCode,
  onSubscribePublic,
  onUnsubscribe,
  onDeleteWatchlist,
  stats,
}: WatchlistBarProps) {
  const [panelMode, setPanelMode] = useState<'none' | 'import' | 'subscribe'>('none');
  const [newName, setNewName] = useState('');
  const [newFile, setNewFile] = useState<File | null>(null);
  const [shareCodeInput, setShareCodeInput] = useState('');
  const [copied, setCopied] = useState(false);
  const updateInputRef = useRef<HTMLInputElement>(null);

  const selected = watchlists.find((w) => w.id === selectedWatchlistId) ?? null;

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newFile) return;
    await onImportWatchlist(newName.trim(), newFile);
    setNewName('');
    setNewFile(null);
    setPanelMode('none');
  };

  const handleSubscribeCode = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!shareCodeInput.trim()) return;
    await onSubscribeByCode(shareCodeInput.trim());
    setShareCodeInput('');
    setPanelMode('none');
  };

  const handleUpdateFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && selectedWatchlistId) {
      await onUpdateWatchlistCsv(selectedWatchlistId, file);
      e.target.value = '';
    }
  };

  const copyShareCode = async (code: string) => {
    await navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  const ownedLists = watchlists.filter((w) => w.owned);
  const subscribedLists = watchlists.filter((w) => !w.owned);
  const discoverablePublic = publicWatchlists.filter(
    (pub) => !watchlists.some((mine) => mine.id === pub.id)
  );

  return (
    <div className="neo-panel-pink p-4 space-y-3">
      <div className="flex items-center justify-between gap-2 border-b border-[#FF1E8A]/35 pb-2">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-[#FF5CA8]">
          <Film className="h-4 w-4" />
          <span>Watchlist</span>
        </div>

        {/* Desktop-only management buttons; hidden on phones */}
        <div className="hidden lg:flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => setPanelMode((m) => (m === 'subscribe' ? 'none' : 'subscribe'))}
            className="inline-flex items-center gap-1 rounded border border-purple-400/60 bg-purple-950/60 px-2 py-0.5 text-[11px] font-bold text-purple-200 transition hover:border-[#FF5CA8] hover:text-[#FF5CA8]"
          >
            <Users className="h-3 w-3" />
            <span>{panelMode === 'subscribe' ? 'Close' : 'Follow'}</span>
          </button>
          <button
            type="button"
            onClick={() => setPanelMode((m) => (m === 'import' ? 'none' : 'import'))}
            className="inline-flex items-center gap-1 rounded border-2 border-[#FF7A00] bg-[#FF7A00]/15 px-2 py-0.5 text-[11px] font-bold text-[#FF9E44] transition hover:bg-[#FF7A00] hover:text-[#090118]"
          >
            {panelMode === 'import' ? <X className="h-3 w-3" /> : <Plus className="h-3 w-3" />}
            <span>{panelMode === 'import' ? 'Cancel' : 'Import'}</span>
          </button>
        </div>
      </div>

      {/* Desktop-only Import Form */}
      {panelMode === 'import' && (
        <form onSubmit={handleImport} className="hidden lg:block space-y-2 rounded border-2 border-[#FF1E8A]/60 bg-[#090118] p-3">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="Watchlist name..."
            className="w-full rounded border-2 border-pink-500/50 bg-[#1F0722] px-3 py-1.5 text-xs text-white placeholder-pink-300/40 focus:border-[#FF7A00] focus:outline-none"
          />
          <label className="flex cursor-pointer items-center justify-center gap-2 rounded border-2 border-dashed border-[#FF7A00]/80 bg-[#1F0722] px-3 py-2 text-xs font-bold text-[#FF9E44] hover:border-[#FF1E8A]">
            <Upload className="h-3.5 w-3.5" />
            <span className="truncate">{newFile ? newFile.name : 'Choose IMDb CSV...'}</span>
            <input
              type="file"
              accept=".csv,text/csv"
              onChange={(e) => setNewFile(e.target.files?.[0] ?? null)}
              className="hidden"
            />
          </label>
          <button
            type="submit"
            disabled={!newName.trim() || !newFile}
            className="w-full rounded border-2 border-[#FF1E8A] bg-[#FF1E8A] px-3 py-1.5 text-xs font-extrabold text-white hover:brightness-110 disabled:opacity-40"
          >
            Upload Watchlist
          </button>
        </form>
      )}

      {/* Desktop-only Subscribe Form */}
      {panelMode === 'subscribe' && (
        <div className="hidden lg:block space-y-2.5 rounded border-2 border-purple-500/60 bg-[#090118] p-3 text-xs">
          <form onSubmit={handleSubscribeCode} className="flex gap-1.5">
            <input
              type="text"
              value={shareCodeInput}
              onChange={(e) => setShareCodeInput(e.target.value)}
              placeholder="Paste share code..."
              className="flex-1 min-w-0 rounded border border-purple-500/60 bg-[#14062B] px-2.5 py-1.5 text-xs text-white placeholder-purple-300/40 focus:border-[#FF2A85] focus:outline-none"
            />
            <button
              type="submit"
              className="rounded bg-[#FF2A85] px-2.5 py-1.5 text-xs font-bold text-white hover:brightness-110"
            >
              Add
            </button>
          </form>

          {discoverablePublic.length > 0 && (
            <div className="space-y-1.5 border-t border-purple-800/60 pt-2">
              <p className="text-[10px] font-bold uppercase tracking-wider text-purple-300">Public Lists</p>
              <div className="max-h-28 space-y-1 overflow-y-auto">
                {discoverablePublic.map((pub) => (
                  <div key={pub.id} className="flex items-center justify-between gap-2 rounded bg-[#14062B] px-2 py-1">
                    <span className="truncate text-purple-100">
                      {pub.name} <span className="text-purple-400">by {pub.ownerName}</span>
                    </span>
                    <button
                      type="button"
                      onClick={() => onSubscribePublic(pub.id)}
                      className="shrink-0 rounded bg-[#39FF14] px-2 py-0.5 text-[10px] font-extrabold text-[#090118]"
                    >
                      Follow
                    </button>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Watchlist Selector (Always visible on desktop & mobile) */}
      <div className="flex flex-wrap gap-2">
        <select
          value={selectedWatchlistId ?? ''}
          onChange={(e) => onSelectWatchlist(e.target.value || null)}
          className="min-w-[140px] flex-1 rounded border-2 border-[#FF1E8A]/50 bg-[#090118] px-2.5 py-2 text-xs font-semibold text-white focus:border-[#FF1E8A] focus:outline-none"
        >
          <option value="">Select watchlist...</option>
          {ownedLists.length > 0 && (
            <optgroup label="My Watchlists">
              {ownedLists.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name} ({w.itemCount})
                </option>
              ))}
            </optgroup>
          )}
          {subscribedLists.length > 0 && (
            <optgroup label="Subscribed">
              {subscribedLists.map((w) => (
                <option key={w.id} value={w.id}>
                  {w.name} by {w.ownerName} ({w.itemCount})
                </option>
              ))}
            </optgroup>
          )}
        </select>

        {/* Desktop-only owner actions (CSV update / delete / unsubscribe) */}
        {selected && (
          <div className="hidden lg:flex items-center gap-1.5">
            {selected.owned ? (
              <>
                <input
                  ref={updateInputRef}
                  type="file"
                  accept=".csv,text/csv"
                  onChange={handleUpdateFile}
                  className="hidden"
                />
                <button
                  type="button"
                  onClick={() => updateInputRef.current?.click()}
                  title="Update watchlist from CSV"
                  className="inline-flex items-center gap-1 rounded border-2 border-[#FF7A00] bg-[#FF7A00]/15 px-2.5 py-2 text-xs font-bold text-[#FF9E44] transition hover:bg-[#FF7A00] hover:text-[#090118]"
                >
                  <RefreshCw className="h-3.5 w-3.5" />
                </button>
                <button
                  type="button"
                  onClick={() => onDeleteWatchlist(selected.id)}
                  title="Delete watchlist"
                  className="inline-flex items-center justify-center rounded border-2 border-[#FF2E54] bg-[#FF2E54]/15 px-2.5 py-2 text-[#FF2E54] transition hover:bg-[#FF2E54] hover:text-white"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </>
            ) : (
              <button
                type="button"
                onClick={() => onUnsubscribe(selected.id)}
                title="Unfollow watchlist"
                className="rounded border-2 border-purple-500/60 bg-[#090118] px-2.5 py-2 text-[11px] font-bold text-purple-200 hover:border-[#FF2E54] hover:text-[#FF2E54]"
              >
                Unfollow
              </button>
            )}
          </div>
        )}
      </div>

      {/* Desktop-only Visibility & Share Code Bar (Owners only) */}
      {selected?.owned && (
        <div className="hidden lg:flex flex-wrap items-center justify-between gap-2 rounded border border-[#FF1E8A]/30 bg-[#090118]/75 px-2.5 py-1.5 text-[11px]">
          <div className="flex items-center gap-1">
            {(['PRIVATE', 'SHARED', 'PUBLIC'] as const).map((vis) => {
              const active = selected.visibility === vis;
              return (
                <button
                  key={vis}
                  type="button"
                  onClick={() => onChangeVisibility(selected.id, vis)}
                  className={`inline-flex items-center gap-1 rounded px-2 py-0.5 font-bold transition ${
                    active ? 'bg-[#FF2A85] text-white' : 'text-pink-200/70 hover:text-white'
                  }`}
                >
                  {vis === 'PRIVATE' && <Lock className="h-2.5 w-2.5" />}
                  {vis === 'SHARED' && <Link2 className="h-2.5 w-2.5" />}
                  {vis === 'PUBLIC' && <Globe className="h-2.5 w-2.5" />}
                  <span>{vis.toLowerCase()}</span>
                </button>
              );
            })}
          </div>

          {selected.shareCode && (
            <button
              type="button"
              onClick={() => copyShareCode(selected.shareCode!)}
              title="Copy share code"
              className="inline-flex items-center gap-1 rounded border border-[#39FF14]/40 bg-[#39FF14]/10 px-2 py-0.5 font-mono text-[11px] font-bold text-[#39FF14] hover:bg-[#39FF14]/20"
            >
              <Copy className="h-2.5 w-2.5" />
              <span>{copied ? 'Copied!' : selected.shareCode}</span>
            </button>
          )}
        </div>
      )}

      {stats && (
        <div className="grid grid-cols-3 gap-2 border-t border-[#FF1E8A]/25 pt-3 text-xs">
          <div className="rounded border border-[#FF1E8A]/35 bg-[#090118]/80 p-2">
            <p className="text-[10px] uppercase tracking-wider text-pink-300/70">Matching</p>
            <p className="font-display text-base font-extrabold text-[#FF5CA8]">{stats.total}</p>
          </div>
          <div className="rounded border border-[#FF1E8A]/35 bg-[#090118]/80 p-2">
            <p className="text-[10px] uppercase tracking-wider text-pink-300/70">Avg Star</p>
            <p className="font-display text-base font-extrabold text-[#FF9E44]">
              {stats.avgRating ?? '—'}
            </p>
          </div>
          <div className="rounded border border-[#FF1E8A]/35 bg-[#090118]/80 p-2">
            <p className="text-[10px] uppercase tracking-wider text-pink-300/70">Top</p>
            <p className="truncate font-display text-xs font-bold text-white pt-1">
              {stats.topGenre ?? '—'}
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
