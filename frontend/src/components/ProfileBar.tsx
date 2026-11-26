import { useState } from 'react';
import { Check, Plus, Sparkles, Trash2, User, X } from 'lucide-react';
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
    <div className="space-y-3">
      <div className="aero-card rounded-3xl p-4 space-y-2.5">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 font-display text-sm font-bold text-purple-700">
            <span className="flex h-7 w-7 items-center justify-center rounded-full bg-[#39FF14] text-green-950 shadow-sm">
              <User className="h-4 w-4" />
            </span>
            <span>Profile</span>
          </div>
          <button
            type="button"
            onClick={() => setCreating((v) => !v)}
            className="inline-flex items-center gap-1 rounded-full bg-purple-600 px-3 py-1 text-xs font-bold text-white shadow-sm transition hover:bg-purple-500 active:scale-95"
          >
            {creating ? <X className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
            <span>{creating ? 'Cancel' : 'New'}</span>
          </button>
        </div>

        {creating && (
          <form onSubmit={handleCreate} className="flex gap-2">
            <input
              type="text"
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              placeholder="Profile name..."
              className="flex-1 rounded-2xl border-2 border-purple-200 bg-white px-3.5 py-2 text-sm font-semibold text-slate-800 placeholder-slate-400 focus:border-[#39FF14] focus:outline-none"
            />
            <button
              type="submit"
              className="rounded-2xl bg-[#39FF14] px-4 py-2 text-xs font-extrabold text-green-950 border-2 border-green-600 hover:brightness-105"
            >
              Add
            </button>
          </form>
        )}

        <div className="flex gap-2">
          <select
            value={selectedUserId ?? ''}
            onChange={(e) => onSelectUser(e.target.value || null)}
            className="flex-1 rounded-2xl border-2 border-purple-200 bg-white px-3.5 py-2 text-sm font-semibold text-slate-800 focus:border-purple-500 focus:outline-none"
          >
            <option value="">Pick a profile...</option>
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
              title="Delete profile"
              className="inline-flex items-center justify-center rounded-2xl border-2 border-rose-300 bg-rose-50 px-3 py-2 text-rose-500 transition hover:bg-rose-500 hover:text-white"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {lastGeneratedPrompt && (
        <div className="animate-pop-in flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 rounded-3xl border-3 border-[#FF7A59] bg-gradient-to-r from-orange-50 to-pink-50 px-4 py-3 text-xs font-semibold text-slate-800 shadow-lg">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 shrink-0 text-[#FF5E3A]" />
            <span>
              Last time you rolled <strong className="text-purple-700">{lastGeneratedPrompt.title}</strong>
              {lastGeneratedPrompt.releaseYear ? ` (${lastGeneratedPrompt.releaseYear})` : ''}. Watch it?
            </span>
          </div>
          <div className="flex gap-2 shrink-0">
            <button
              type="button"
              onClick={() => onMarkLastSeen(lastGeneratedPrompt.imdbId)}
              className="inline-flex items-center gap-1 rounded-full bg-[#39FF14] border-2 border-green-600 px-3 py-1 font-extrabold text-green-950 hover:brightness-105"
            >
              <Check className="h-3.5 w-3.5" />
              <span>Yes, Seen!</span>
            </button>
            <button
              type="button"
              onClick={onDismissPrompt}
              className="rounded-full border-2 border-slate-300 bg-white px-3 py-1 text-slate-600 hover:bg-slate-100"
            >
              Nope
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
