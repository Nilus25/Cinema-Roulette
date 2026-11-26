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
    <div className="border-2 border-[#FF007F]/80 bg-[#0C041A]/95 p-3.5 space-y-2.5 shadow-[0_0_12px_rgba(255,0,127,0.28)]">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-2 font-display text-xs uppercase tracking-wider text-[#FF5CA0]">
          <Film className="h-3.5 w-3.5" />
          <span>Watchlist</span>
        </div>
        <button
          type="button"
          onClick={() => setImporting((v) => !v)}
          className="inline-flex items-center gap-1 border border-[#FF7A59] bg-[#FF7A59]/15 px-2.5 py-0.5 text-[11px] font-bold uppercase text-[#FF9E44] hover:bg-[#FF7A59] hover:text-black"
        >
          {importing ? <X className="h-3 w-3" /> : <Plus className="h-3 w-3" />}
          <span>{importing ? 'Cancel' : 'Import CSV'}</span>
        </button>
      </div>

      {importing && (
        <form onSubmit={handleImport} className="space-y-2 border border-[#FF007F]/50 bg-[#05010D] p-3">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="WATCHLIST NAME..."
            className="w-full border border-purple-500/60 bg-[#0C041A] px-3 py-1.5 text-xs text-slate-100 placeholder-slate-600 focus:border-[#FF7A59] focus:outline-none"
          />
          <label className="flex cursor-pointer items-center justify-center gap-2 border border-dashed border-[#FF7A59]/70 bg-[#0C041A] px-3 py-2 text-xs text-[#FF9E44] hover:border-[#FF007F]">
            <Upload className="h-3.5 w-3.5" />
            <span className="truncate">{newFile ? newFile.name : 'SELECT IMDB CSV...'}</span>
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
            className="w-full border border-[#FF007F] bg-[#FF007F] px-3 py-1.5 text-xs font-bold uppercase text-white hover:brightness-110 disabled:opacity-40"
          >
            Upload Watchlist
          </button>
        </form>
      )}

      <div className="flex flex-wrap gap-2">
        <select
          value={selectedWatchlistId ?? ''}
          onChange={(e) => onSelectWatchlist(e.target.value || null)}
          className="min-w-[160px] flex-1 border border-purple-500/60 bg-[#05010D] px-3 py-2 text-xs text-slate-100 focus:border-[#FF007F] focus:outline-none"
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
              title="Replace watchlist with updated CSV"
              className="inline-flex items-center gap-1.5 border border-[#FF9E44] bg-[#FF9E44]/15 px-3 py-2 text-xs font-bold uppercase text-[#FF9E44] hover:bg-[#FF9E44] hover:text-black"
            >
              <RefreshCw className="h-3.5 w-3.5" />
              <span className="hidden sm:inline">CSV</span>
            </button>
            <button
              type="button"
              onClick={() => onDeleteWatchlist(selectedWatchlistId)}
              title="Delete watchlist"
              className="inline-flex items-center justify-center border border-[#FF2A55] bg-[#FF2A55]/15 px-3 py-2 text-[#FF2A55] hover:bg-[#FF2A55] hover:text-black"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          </>
        )}
      </div>
    </div>
  );
}
