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
    <div className="aero-card rounded-3xl p-4 space-y-2.5">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-[#D91B7A]">
          <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#FF49B0] text-white shadow-sm">
            <Film className="h-4 w-4" />
          </span>
          <span>Watchlist</span>
        </div>
        <button
          type="button"
          onClick={() => setImporting((v) => !v)}
          className="inline-flex items-center gap-1 rounded-full bg-gradient-to-r from-[#FF49B0] to-[#FF8C42] px-3 py-1 text-xs font-bold text-white shadow-sm transition hover:brightness-110 active:scale-95"
        >
          {importing ? <X className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
          <span>{importing ? 'Cancel' : 'Import CSV'}</span>
        </button>
      </div>

      {importing && (
        <form onSubmit={handleImport} className="space-y-2 rounded-2xl border-2 border-pink-200 bg-white/90 p-3">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="Watchlist name..."
            className="w-full rounded-xl border-2 border-purple-200 bg-white px-3 py-2 text-sm font-semibold text-slate-800 placeholder-slate-400 focus:border-[#FF49B0] focus:outline-none"
          />
          <label className="flex cursor-pointer items-center justify-center gap-2 rounded-xl border-2 border-dashed border-pink-300 bg-pink-50/70 px-3 py-2.5 text-xs font-bold text-[#D91B7A] hover:bg-pink-100/70">
            <Upload className="h-4 w-4" />
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
            className="w-full rounded-xl bg-gradient-to-r from-[#FF49B0] to-[#FF8C42] px-3 py-2 text-xs font-extrabold text-white shadow-sm hover:brightness-110 disabled:opacity-40"
          >
            Upload Watchlist
          </button>
        </form>
      )}

      <div className="flex flex-wrap gap-2">
        <select
          value={selectedWatchlistId ?? ''}
          onChange={(e) => onSelectWatchlist(e.target.value || null)}
          className="min-w-[160px] flex-1 rounded-2xl border-2 border-pink-200 bg-white px-3.5 py-2 text-sm font-semibold text-slate-800 focus:border-[#FF49B0] focus:outline-none"
        >
          <option value="">Pick a watchlist...</option>
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
              className="inline-flex items-center gap-1.5 rounded-2xl border-2 border-orange-300 bg-orange-50 px-3 py-2 text-xs font-bold text-orange-600 transition hover:bg-orange-400 hover:text-white"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">CSV</span>
            </button>
            <button
              type="button"
              onClick={() => onDeleteWatchlist(selectedWatchlistId)}
              title="Delete watchlist"
              className="inline-flex items-center justify-center rounded-2xl border-2 border-rose-300 bg-rose-50 px-3 py-2 text-rose-500 transition hover:bg-rose-500 hover:text-white"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
