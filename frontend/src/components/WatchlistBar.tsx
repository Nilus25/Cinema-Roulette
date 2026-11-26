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
    <div className="swing-window">
      <div className="swing-titlebar-pink flex items-center justify-between px-2.5 py-1.5 text-white">
        <div className="flex items-center gap-1.5 font-display text-xs font-bold uppercase">
          <Film className="h-3.5 w-3.5" />
          <span>Watchlist.csv</span>
        </div>
        <button
          type="button"
          onClick={() => setImporting((v) => !v)}
          className="swing-btn inline-flex items-center gap-1 bg-[#1B0938] px-2 py-0.5 text-[10px] font-bold uppercase text-[#FF6B00]"
        >
          {importing ? <X className="h-3 w-3" /> : <Plus className="h-3 w-3" />}
          <span>{importing ? 'Close' : 'Import'}</span>
        </button>
      </div>

      <div className="p-3 space-y-2.5">
        {importing && (
          <form onSubmit={handleImport} className="swing-inset space-y-2 p-2.5">
            <input
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="WATCHLIST NAME..."
              className="swing-inset w-full px-2.5 py-1.5 text-xs text-slate-100 placeholder-purple-400/50 focus:outline-none"
            />
            <label className="swing-btn flex cursor-pointer items-center justify-center gap-2 bg-[#2B0F54] px-3 py-1.5 text-xs font-bold uppercase text-[#FFE600]">
              <Upload className="h-3.5 w-3.5" />
              <span className="truncate">{newFile ? newFile.name : 'BROWSE CSV...'}</span>
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
              className="swing-btn w-full bg-[#FF0080] px-3 py-1.5 text-xs font-bold uppercase text-white disabled:opacity-40"
            >
              Upload
            </button>
          </form>
        )}

        <div className="flex flex-wrap gap-2">
          <select
            value={selectedWatchlistId ?? ''}
            onChange={(e) => onSelectWatchlist(e.target.value || null)}
            className="swing-inset min-w-[160px] flex-1 px-2.5 py-2 text-xs text-slate-100 focus:outline-none"
          >
            <option value="">-- SELECT WATCHLIST --</option>
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
                className="swing-btn inline-flex items-center gap-1 bg-[#FF6B00] px-2.5 py-2 text-xs font-bold uppercase text-black"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                <span className="hidden sm:inline">CSV</span>
              </button>
              <button
                type="button"
                onClick={() => onDeleteWatchlist(selectedWatchlistId)}
                title="Delete watchlist"
                className="swing-btn inline-flex items-center justify-center bg-[#FF1E56] px-2.5 py-2 text-white"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
