import { useState } from 'react';
import type { MediaItemResponse, UserResponse } from '../types/api';

interface ProfileBarProps {
  users: UserResponse[];
  selectedUserId: string | null;
  onSelectUser: (id: string | null) => void;
  onCreateUser: (name: string) => Promise<void>;
  onDeleteUser: (id: string) => Promise<void>;
  lastGeneratedPrompt: MediaItemResponse | null;
  onMarkLastSeen: (imdbId: string) => Promise<void>;
  onDismissPrompt: () => void;
}

export function ProfileBar({
  users,
  selectedUserId,
  onSelectUser,
  onCreateUser,
  onDeleteUser,
  lastGeneratedPrompt,
  onMarkLastSeen,
  onDismissPrompt,
}: ProfileBarProps) {
  const [newName, setNewName] = useState('');
  const [creating, setCreating] = useState(false);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newName.trim()) return;
    await onCreateUser(newName.trim());
    setNewName('');
    setCreating(false);
  };

  return (
    <section className="rounded-2xl border border-slate-800 bg-slate-900/80 p-4 space-y-3">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-400">Profile</h2>
        <button
          type="button"
          onClick={() => setCreating((v) => !v)}
          className="text-xs font-medium text-amber-400 hover:text-amber-300"
        >
          {creating ? 'Cancel' : '+ New Profile'}
        </button>
      </div>

      {creating && (
        <form onSubmit={handleCreate} className="flex gap-2">
          <input
            type="text"
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="Profile name..."
            className="flex-1 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-amber-400 focus:outline-none"
          />
          <button
            type="submit"
            className="rounded-lg bg-amber-400 px-3 py-2 text-sm font-semibold text-slate-950 hover:bg-amber-300"
          >
            Create
          </button>
        </form>
      )}

      <div className="flex gap-2">
        <select
          value={selectedUserId ?? ''}
          onChange={(e) => onSelectUser(e.target.value || null)}
          className="flex-1 rounded-lg border border-slate-700 bg-slate-950 px-3 py-2 text-sm text-slate-100 focus:border-amber-400 focus:outline-none"
        >
          <option value="">Select a profile...</option>
          {users.map((u) => (
            <option key={u.id} value={u.id}>
              {u.name} ({u.seenCount} seen)
            </option>
          ))}
        </select>
        {selectedUserId && (
          <button
            type="button"
            onClick={() => onDeleteUser(selectedUserId)}
            className="rounded-lg border border-rose-500/40 px-3 py-2 text-xs font-medium text-rose-400 hover:bg-rose-500/10"
          >
            Delete
          </button>
        )}
      </div>

      {lastGeneratedPrompt && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 rounded-xl border border-amber-500/30 bg-amber-500/10 p-3 text-xs text-amber-200">
          <span>
            Last picked: <strong>{lastGeneratedPrompt.title}</strong>
            {lastGeneratedPrompt.releaseYear ? ` (${lastGeneratedPrompt.releaseYear})` : ''}. Mark as seen?
          </span>
          <div className="flex gap-2 shrink-0">
            <button
              type="button"
              onClick={() => onMarkLastSeen(lastGeneratedPrompt.imdbId)}
              className="rounded-lg bg-amber-400 px-2.5 py-1 font-semibold text-slate-950 hover:bg-amber-300"
            >
              Mark Seen
            </button>
            <button
              type="button"
              onClick={onDismissPrompt}
              className="rounded-lg border border-slate-700 px-2.5 py-1 text-slate-300 hover:bg-slate-800"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}
    </section>
  );
}
