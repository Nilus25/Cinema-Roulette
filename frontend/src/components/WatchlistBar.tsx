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
    <div className="neo-panel p-4 space-y-3">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 font-display text-sm font-bold text-purple-200">
          <Film className="h-4 w-4 text-purple-400" />
          <span>Watchlist</span>
        </div>
        <button
          type="button"
          onClick={() => setImporting((v) => !v)}
          className="inline-flex items-center gap-1 rounded border border-purple-400/60 bg-purple-950/60 px-2.5 py-0.5 text-xs font-bold text-purple-200 transition hover:border-[#FF1E8A] hover:text-[#FF72B6]"
        >
          {importing ? <X className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
          <span>{importing ? 'Cancel' : 'Import CSV'}</span>
        </button>
      </div>

      {importing && (
        <form onSubmit={handleImport} className="space-y-2 rounded border-2 border-purple-500/70 bg-[#090118] p-3">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="Watchlist name..."
            className="w-full rounded border-2 border-purple-500/70 bg-[#14062B] px-3 py-1.5 text-sm text-white placeholder-purple-300/40 focus:border-[#FF1E8A] focus:outline-none"
          />
          <label className="flex cursor-pointer items-center justify-center gap-2 rounded border-2 border-dashed border-purple-400/70 bg-[#14062B] px-3 py-2 text-xs font-bold text-purple-200 hover:border-[#FF1E8A]">
            <Upload className="h-3.5 w-3.5 text-[#FF72B6]" />
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
          className="min-w-[160px] flex-1 rounded border-2 border-purple-500/70 bg-[#090118] px-3 py-2 text-sm font-semibold text-white focus:border-purple-400 focus:outline-none"
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
              title="Update watchlist from CSV"
              className="inline-flex items-center gap-1.5 rounded border-2 border-purple-500/60 bg-[#090118] px-3 py-2 text-xs font-bold text-purple-200 transition hover:border-[#FF9E44] hover:text-[#FF9E44]"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">CSV</span>
            </button>
            <button
              type="button"
              onClick={() => onDeleteWatchlist(selectedWatchlistId)}
              title="Delete watchlist"
              className="inline-flex items-center justify-center rounded border-2 border-purple-500/60 bg-[#090118] px-3 py-2 text-rose-400 transition hover:border-[#FF2E54] hover:bg-[#FF2E54] hover:text-white"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
