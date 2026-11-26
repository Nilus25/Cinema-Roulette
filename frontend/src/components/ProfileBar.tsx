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
      <div className="rounded-2xl border border-[#311B52] bg-[#150C26]/90 p-3.5 space-y-2.5">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-purple-300">
            <User className="h-3.5 w-3.5 text-[#39FF14]" />
            <span>Profile</span>
          </div>
          <button
            type="button"
            onClick={() => setCreating((v) => !v)}
            className="inline-flex items-center gap-1 rounded-full border border-[#39FF14]/40 bg-[#39FF14]/10 px-2.5 py-1 text-xs font-medium text-[#39FF14] transition hover:bg-[#39FF14]/20"
          >
            {creating ? <X className="h-3 w-3" /> : <Plus className="h-3 w-3" />}
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
              className="flex-1 rounded-xl border border-[#3E2468] bg-[#0B0614] px-3 py-2 text-sm text-slate-100 placeholder-slate-500 focus:border-[#39FF14] focus:outline-none"
            />
            <button
              type="submit"
              className="rounded-xl bg-[#39FF14] px-3.5 py-2 text-xs font-bold text-[#07120A] transition hover:bg-[#5CFF3B]"
            >
              Create
            </button>
          </form>
        )}

        <div className="flex gap-2">
          <select
            value={selectedUserId ?? ''}
            onChange={(e) => onSelectUser(e.target.value || null)}
            className="flex-1 rounded-xl border border-[#3E2468] bg-[#0B0614] px-3 py-2 text-sm text-slate-100 focus:border-purple-400 focus:outline-none"
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
              title="Delete profile"
              className="inline-flex items-center justify-center rounded-xl border border-[#FF3B5C]/40 bg-[#FF3B5C]/10 px-3 py-2 text-[#FF3B5C] transition hover:bg-[#FF3B5C]/20"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {lastGeneratedPrompt && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 rounded-2xl border border-[#FF7A59]/45 bg-[#FF7A59]/10 px-4 py-3 text-xs text-orange-100">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 shrink-0 text-[#FF9E44]" />
            <span>
              Last picked: <strong className="text-white">{lastGeneratedPrompt.title}</strong>
              {lastGeneratedPrompt.releaseYear ? ` (${lastGeneratedPrompt.releaseYear})` : ''}. Did you watch it?
            </span>
          </div>
          <div className="flex gap-2 shrink-0">
            <button
              type="button"
              onClick={() => onMarkLastSeen(lastGeneratedPrompt.imdbId)}
              className="inline-flex items-center gap-1 rounded-lg bg-[#39FF14] px-3 py-1.5 font-bold text-[#07120A] transition hover:bg-[#5CFF3B]"
            >
              <Check className="h-3.5 w-3.5" />
              <span>Mark Seen</span>
            </button>
            <button
              type="button"
              onClick={onDismissPrompt}
              className="rounded-lg border border-[#3E2468] bg-[#150C26] px-2.5 py-1.5 text-slate-300 hover:bg-[#22143B]"
            >
              Dismiss
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
