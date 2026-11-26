import { useState } from 'react';
import { Check, Eye, Plus, RotateCcw, Trash2, X } from 'lucide-react';
import type {
  WatchlistUpdateApplyOptions,
  WatchlistUpdatePreviewResponse,
} from '../types/api';

interface UpdateDiffModalProps {
  watchlistName: string;
  fileName: string;
  preview: WatchlistUpdatePreviewResponse;
  busy: boolean;
  onCancel: () => void;
  onConfirm: (options: WatchlistUpdateApplyOptions) => Promise<void>;
}

export function UpdateDiffModal({
  watchlistName,
  fileName,
  preview,
  busy,
  onCancel,
  onConfirm,
}: UpdateDiffModalProps) {
  const [activeTab, setActiveTab] = useState<'summary' | 'added' | 'removed'>('summary');
  const [excludeAdded, setExcludeAdded] = useState<Set<string>>(() => new Set());
  const [markSeen, setMarkSeen] = useState<Set<string>>(() => new Set());
  const [keepRemoved, setKeepRemoved] = useState<Set<string>>(() => new Set());

  const toggleSet = (setter: React.Dispatch<React.SetStateAction<Set<string>>>, id: string) => {
    setter((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const willAddCount = preview.added.length - excludeAdded.size;
  const willDeleteCount = preview.removed.length - keepRemoved.size;

  const handleConfirm = async () => {
    await onConfirm({
      excludeAddedIds: Array.from(excludeAdded),
      keepRemovedIds: Array.from(keepRemoved),
      markSeenIds: Array.from(markSeen),
    });
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
      <div className="neo-panel-pink flex max-h-[85vh] w-full max-w-3xl flex-col overflow-hidden p-5">
        <div className="flex items-start justify-between gap-3 border-b border-[#FF1E8A]/40 pb-3">
          <div>
            <h2 className="font-display text-lg font-extrabold text-[#FF5CA8]">
              Review Watchlist Update
            </h2>
            <p className="text-xs text-purple-200/80">
              {watchlistName} &larr; <span className="font-mono text-[#FF9E44]">{fileName}</span>
            </p>
          </div>
          <button
            type="button"
            onClick={onCancel}
            disabled={busy}
            className="rounded border border-purple-400/50 p-1 text-purple-200 hover:border-[#FF1E8A] hover:text-[#FF5CA8]"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-2">
          <div className="rounded border-2 border-[#00E5A3]/60 bg-[#00E5A3]/10 px-3 py-1.5 text-xs font-extrabold text-[#00E5A3]">
            +{preview.added.length} being added
          </div>
          <div className="rounded border-2 border-[#FF4D6D]/60 bg-[#FF4D6D]/10 px-3 py-1.5 text-xs font-extrabold text-[#FF7A91]">
            -{preview.removed.length} being deleted
          </div>

          <div className="ml-auto flex flex-wrap gap-2">
            <button
              type="button"
              onClick={() => setActiveTab((t) => (t === 'added' ? 'summary' : 'added'))}
              disabled={preview.added.length === 0}
              className={`inline-flex items-center gap-1.5 rounded border-2 px-3 py-1.5 text-xs font-bold transition disabled:opacity-40 ${
                activeTab === 'added'
                  ? 'border-[#00E5A3] bg-[#00E5A3] text-[#090118]'
                  : 'border-[#00E5A3]/70 bg-[#090118] text-[#00E5A3] hover:bg-[#00E5A3]/15'
              }`}
            >
              <Plus className="h-3.5 w-3.5" />
              <span>Check added ({preview.added.length})</span>
            </button>
            <button
              type="button"
              onClick={() => setActiveTab((t) => (t === 'removed' ? 'summary' : 'removed'))}
              disabled={preview.removed.length === 0}
              className={`inline-flex items-center gap-1.5 rounded border-2 px-3 py-1.5 text-xs font-bold transition disabled:opacity-40 ${
                activeTab === 'removed'
                  ? 'border-[#FF4D6D] bg-[#FF4D6D] text-[#090118]'
                  : 'border-[#FF4D6D]/70 bg-[#090118] text-[#FF7A91] hover:bg-[#FF4D6D]/15'
              }`}
            >
              <Trash2 className="h-3.5 w-3.5" />
              <span>Check deleted ({preview.removed.length})</span>
            </button>
          </div>
        </div>

        <div className="mt-4 flex-1 overflow-y-auto rounded border-2 border-purple-500/40 bg-[#090118] p-3">
          {activeTab === 'summary' && (
            <div className="py-6 text-center text-sm text-purple-200/85">
              <p>
                <strong className="text-[#00E5A3]">{preview.added.length}</strong> new titles will be added and{' '}
                <strong className="text-[#FF7A91]">{preview.removed.length}</strong> titles will be deleted.
              </p>
              <p className="mt-2 text-xs text-purple-300/70">
                Use <strong>Check added</strong> to mark new titles as seen or skip adding them, or{' '}
                <strong>Check deleted</strong> to keep titles instead of removing them.
              </p>
            </div>
          )}

          {activeTab === 'added' && (
            <div className="space-y-2">
              {preview.added.map((item) => {
                const included = !excludeAdded.has(item.imdbId);
                const seen = markSeen.has(item.imdbId);
                return (
                  <div
                    key={item.imdbId}
                    className={`flex flex-wrap items-center justify-between gap-2 rounded border px-3 py-2 text-xs ${
                      included
                        ? 'border-[#00E5A3]/40 bg-[#1F0722]'
                        : 'border-purple-500/20 bg-[#120415] opacity-60'
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="truncate font-bold text-white">
                        {item.title}{' '}
                        {item.releaseYear && <span className="text-purple-300/70">({item.releaseYear})</span>}
                      </div>
                      <div className="truncate text-[11px] text-purple-300/70">
                        {item.director || 'Unknown director'} &middot; {item.rating ?? 'N/A'} &middot;{' '}
                        {item.genres.join(', ') || item.mediaType}
                      </div>
                    </div>
                    <div className="flex items-center gap-3">
                      <label className="inline-flex cursor-pointer items-center gap-1.5 font-bold text-[#00E5A3]">
                        <input
                          type="checkbox"
                          checked={included}
                          onChange={() => toggleSet(setExcludeAdded, item.imdbId)}
                          className="accent-[#00E5A3]"
                        />
                        <span>Add</span>
                      </label>
                      <label className="inline-flex cursor-pointer items-center gap-1.5 font-bold text-[#FF9E44]">
                        <input
                          type="checkbox"
                          checked={seen}
                          onChange={() => toggleSet(setMarkSeen, item.imdbId)}
                          className="accent-[#FF7A00]"
                        />
                        <Eye className="h-3.5 w-3.5" />
                        <span>Seen</span>
                      </label>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

          {activeTab === 'removed' && (
            <div className="space-y-2">
              {preview.removed.map((item) => {
                const kept = keepRemoved.has(item.imdbId);
                return (
                  <div
                    key={item.imdbId}
                    className={`flex flex-wrap items-center justify-between gap-2 rounded border px-3 py-2 text-xs ${
                      kept
                        ? 'border-[#FF9E44]/60 bg-[#1F0722]'
                        : 'border-[#FF4D6D]/40 bg-[#1A0510]'
                    }`}
                  >
                    <div className="min-w-0 flex-1">
                      <div className="truncate font-bold text-white">
                        {item.title}{' '}
                        {item.releaseYear && <span className="text-purple-300/70">({item.releaseYear})</span>}
                      </div>
                      <div className="truncate text-[11px] text-purple-300/70">
                        {item.director || 'Unknown director'} &middot; {item.rating ?? 'N/A'} &middot;{' '}
                        {item.genres.join(', ') || item.mediaType}
                      </div>
                    </div>
                    <label className="inline-flex cursor-pointer items-center gap-1.5 font-bold text-[#FF9E44]">
                      <input
                        type="checkbox"
                        checked={kept}
                        onChange={() => toggleSet(setKeepRemoved, item.imdbId)}
                        className="accent-[#FF7A00]"
                      />
                      <RotateCcw className="h-3.5 w-3.5" />
                      <span>Keep in watchlist</span>
                    </label>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <div className="mt-4 flex flex-wrap items-center justify-between gap-3 border-t border-[#FF1E8A]/35 pt-3">
          <div className="text-xs text-purple-200/80">
            Applying: <strong className="text-[#00E5A3]">+{willAddCount}</strong> added,{' '}
            <strong className="text-[#FF7A91]">-{willDeleteCount}</strong> deleted,{' '}
            <strong className="text-[#FF9E44]">{markSeen.size}</strong> marked seen
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onCancel}
              disabled={busy}
              className="rounded border-2 border-purple-400/50 px-3 py-1.5 text-xs font-bold text-purple-200 hover:border-[#FF5CA8]"
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={handleConfirm}
              disabled={busy}
              className="inline-flex items-center gap-1.5 rounded border-2 border-[#00E5A3] bg-[#00E5A3] px-4 py-1.5 text-xs font-extrabold text-[#090118] transition hover:bg-[#3BF5BD] disabled:opacity-50"
            >
              <Check className="h-3.5 w-3.5" />
              <span>{busy ? 'Updating...' : 'OK, Apply Update'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
