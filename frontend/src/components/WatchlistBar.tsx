import { useRef, useState } from 'react';
import { Film, Plus, RefreshCw, Trash2, Upload, X } from 'lucide-react';
import type { WatchlistResponse } from '../types/api';

interface WatchlistBarProps {
  watchlists: WatchlistResponse[];
  selectedWatchlistId: string | null;
  onSelectWatchlist: (id: string | null) => void;
  onImportWatchlist: (name: string, file: File) => Promise<void>;
  onUpdateWatchlistCsv: (id: string, file: File) => Promise<void>;
  onDeleteWatchlist: (id: string) => Promise<void>;
}

export function WatchlistBar({
  watchlists,
  selectedWatchlistId,
  onSelectWatchlist,
  onImportWatchlist,
  onUpdateWatchlistCsv,
  onDeleteWatchlist,
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
    <div className="rounded-2xl border border-[#311B52] bg-[#150C26]/90 p-3.5 space-y-2.5">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-purple-300">
          <Film className="h-3.5 w-3.5 text-[#FF2A85]" />
          <span>Watchlist</span>
        </div>
        <button
          type="button"
          onClick={() => setImporting((v) => !v)}
          className="inline-flex items-center gap-1 rounded-full border border-[#FF7A59]/40 bg-[#FF7A59]/10 px-2.5 py-1 text-xs font-medium text-[#FF9E44] transition hover:bg-[#FF7A59]/20"
        >
          {importing ? <X className="h-3 w-3" /> : <Plus className="h-3 w-3" />}
          <span>{importing ? 'Cancel' : 'Import CSV'}</span>
        </button>
      </div>

      {importing && (
        <form onSubmit={handleImport} className="space-y-2 rounded-xl border border-[#3E2468] bg-[#0B0614] p-3">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="Watchlist name..."
            className="w-full rounded-xl border border-[#3E2468] bg-[#150C26] px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-[#FF7A59] focus:outline-none"
          />
          <label className="flex cursor-pointer items-center justify-center gap-2 rounded-xl border border-dashed border-purple-500/50 bg-[#150C26]/60 px-3 py-2.5 text-xs text-purple-200 hover:border-[#FF2A85]">
            <Upload className="h-3.5 w-3.5 text-[#FF7A59]" />
            <span className="truncate">{newFile ? newFile.name : 'Choose IMDb CSV file...'}</span>
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
            className="w-full rounded-xl bg-gradient-to-r from-[#FF2A85] to-[#FF7A59] px-3 py-2 text-xs font-bold text-white transition hover:brightness-110 disabled:opacity-40"
          >
            Import Watchlist
          </button>
        </form>
      )}

      <div className="flex flex-wrap gap-2">
        <select
          value={selectedWatchlistId ?? ''}
          onChange={(e) => onSelectWatchlist(e.target.value || null)}
          className="min-w-[160px] flex-1 rounded-xl border border-[#3E2468] bg-[#0B0614] px-3 py-2 text-sm text-slate-100 focus:border-[#FF2A85] focus:outline-none"
        >
          <option value="">Select a watchlist...</option>
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
              title="Replace watchlist with updated CSV"
              className="inline-flex items-center gap-1.5 rounded-xl border border-[#3E2468] bg-[#0B0614] px-3 py-2 text-xs font-medium text-purple-200 transition hover:border-purple-400"
            >
              <RefreshCw className="h-3.5 w-3.5 text-[#FF9E44]" />
              <span className="hidden sm:inline">Update</span>
            </button>
            <button
              type="button"
              onClick={() => onDeleteWatchlist(selectedWatchlistId)}
              title="Delete watchlist"
              className="inline-flex items-center justify-center rounded-xl border border-[#FF3B5C]/40 bg-[#FF3B5C]/10 px-3 py-2 text-[#FF3B5C] transition hover:bg-[#FF3B5C]/20"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
