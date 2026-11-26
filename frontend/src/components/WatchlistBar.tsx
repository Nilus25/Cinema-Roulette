import { useRef, useState } from 'react';
import { Film, Plus, RefreshCw, Trash2, Upload, X } from 'lucide-react';
import type { WatchlistResponse } from '../types/api';

interface WatchlistStats {
  total: number;
  unseenCount: number;
  avgRating: string | null;
  topGenre: string | null;
}

interface WatchlistBarProps {
  watchlists: WatchlistResponse[];
  selectedWatchlistId: string | null;
  onSelectWatchlist: (id: string | null) => void;
  onImportWatchlist: (name: string, file: File) => Promise<void>;
  onUpdateWatchlistCsv: (id: string, file: File) => Promise<void>;
  onDeleteWatchlist: (id: string) => Promise<void>;
  stats: WatchlistStats | null;
}

export function WatchlistBar({
  watchlists,
  selectedWatchlistId,
  onSelectWatchlist,
  onImportWatchlist,
  onUpdateWatchlistCsv,
  onDeleteWatchlist,
  stats,
}: WatchlistBarProps) {
  const [importing, setImporting] = useState(false);
  const [newName, setNewName] = useState('');
  const [newFile, setNewFile] = useState<File | null>(null);
  const updateInputRef = useRef<HTMLInputElement>(null);

  const handleImport = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim() || !newFile) return;
    await onImportWatchlist(newName.trim(), newFile);
    setNewName('');
    setNewFile(null);
    setImporting(false);
  };

  const handleUpdateFile = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file && selectedWatchlistId) {
      await onUpdateWatchlistCsv(selectedWatchlistId, file);
      e.target.value = '';
    }
  };

  return (
    <div className="neo-panel-pink p-4 space-y-3">
      <div className="flex items-center justify-between gap-2 border-b border-[#FF1E8A]/35 pb-2">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-[#FF5CA8]">
          <Film className="h-4 w-4" />
          <span>Watchlist</span>
        </div>
        <button
          type="button"
          onClick={() => setImporting((v) => !v)}
          className="inline-flex items-center gap-1 rounded border-2 border-[#FF7A00] bg-[#FF7A00]/15 px-2.5 py-0.5 text-xs font-bold text-[#FF9E44] transition hover:bg-[#FF7A00] hover:text-[#090118]"
        >
          {importing ? <X className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
          <span>{importing ? 'Cancel' : 'Import'}</span>
        </button>
      </div>

      {importing && (
        <form onSubmit={handleImport} className="space-y-2 rounded border-2 border-[#FF1E8A]/60 bg-[#090118] p-3">
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

      <div className="flex flex-wrap gap-2">
        <select
          value={selectedWatchlistId ?? ''}
          onChange={(e) => onSelectWatchlist(e.target.value || null)}
          className="min-w-[140px] flex-1 rounded border-2 border-[#FF1E8A]/50 bg-[#090118] px-2.5 py-2 text-xs font-semibold text-white focus:border-[#FF1E8A] focus:outline-none"
        >
          <option value="">Select watchlist...</option>
          {watchlists.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name} ({w.itemCount})
            </option>
          ))}
        </select>
        {selectedWatchlistId && (
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
              onClick={() => onDeleteWatchlist(selectedWatchlistId)}
              title="Delete watchlist"
              className="inline-flex items-center justify-center rounded border-2 border-[#FF2E54] bg-[#FF2E54]/15 px-2.5 py-2 text-[#FF2E54] transition hover:bg-[#FF2E54] hover:text-white"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </>
        )}
      </div>

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
