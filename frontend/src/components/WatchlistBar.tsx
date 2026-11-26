import { useRef, useState } from 'react';
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
    <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-4 space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Watchlist</h2>
        <button
          type="button"
          onClick={() => setImporting((v) => !v)}
          className="text-xs font-medium text-amber-400 hover:text-amber-300"
        >
          {importing ? 'Cancel' : '+ Import IMDB CSV'}
        </button>
      </div>

      {importing && (
        <form onSubmit={handleImport} className="space-y-2 rounded-xl border border-slate-800 bg-slate-950/70 p-3">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="Watchlist name..."
            className="w-full rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-400 focus:outline-none"
          />
          <input
            type="file"
            accept=".csv,text/csv"
            onChange={(e) => setNewFile(e.target.files?.[0] ?? null)}
            className="block w-full text-xs text-slate-400 file:mr-3 file:rounded-lg file:border-0 file:bg-slate-800 file:px-3 file:py-1.5 file:text-xs file:font-medium file:text-slate-200 hover:file:bg-slate-700"
          />
          <button
            type="submit"
            disabled={!newName.trim() || !newFile}
            className="w-full rounded-lg bg-amber-400 px-3 py-2 text-sm font-semibold text-slate-950 hover:bg-amber-300 disabled:opacity-40"
          >
            Import Watchlist
          </button>
        </form>
      )}

      <div className="flex flex-wrap gap-2">
        <select
          value={selectedWatchlistId ?? ''}
          onChange={(e) => onSelectWatchlist(e.target.value || null)}
          className="min-w-[180px] flex-1 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus:border-amber-400 focus:outline-none"
        >
          <option value="">Select a watchlist...</option>
          {watchlists.map((w) => (
            <option key={w.id} value={w.id}>
              {w.name} ({w.itemCount} titles)
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
              className="rounded-lg border border-slate-700 px-3 py-2 text-xs font-medium text-slate-200 hover:bg-slate-800"
            >
              Update CSV
            </button>
            <button
              type="button"
              onClick={() => onDeleteWatchlist(selectedWatchlistId)}
              className="rounded-lg border border-rose-500/40 px-3 py-2 text-xs font-medium text-rose-400 hover:bg-rose-500/10"
            >
              Delete
            </button>
          </>
        )}
      </div>
    </section>
  );
}
