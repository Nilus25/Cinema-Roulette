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
      <div className="neo-panel-green p-4 space-y-3">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 font-display text-sm font-bold text-[#39FF14]">
            <User className="h-4 w-4" />
            <span>Profile</span>
          </div>
          <button
            type="button"
            onClick={() => setCreating((v) => !v)}
            className="inline-flex items-center gap-1 rounded border-2 border-[#39FF14] bg-[#39FF14]/15 px-2.5 py-0.5 text-xs font-bold text-[#39FF14] transition hover:bg-[#39FF14] hover:text-[#090118]"
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
              className="flex-1 rounded border-2 border-[#39FF14]/60 bg-[#090118] px-3 py-1.5 text-sm text-white placeholder-green-300/40 focus:border-[#39FF14] focus:outline-none"
            />
            <button
              type="submit"
              className="rounded border-2 border-[#39FF14] bg-[#39FF14] px-3.5 py-1.5 text-xs font-extrabold text-[#090118] hover:brightness-110"
            >
              Add
            </button>
          </form>
        )}

        <div className="flex gap-2">
          <select
            value={selectedUserId ?? ''}
            onChange={(e) => onSelectUser(e.target.value || null)}
            className="flex-1 rounded border-2 border-[#39FF14]/50 bg-[#090118] px-3 py-2 text-sm font-semibold text-white focus:border-[#39FF14] focus:outline-none"
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
              className="inline-flex items-center justify-center rounded border-2 border-[#FF2E54] bg-[#FF2E54]/15 px-3 py-2 text-[#FF2E54] transition hover:bg-[#FF2E54] hover:text-white"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {lastGeneratedPrompt && (
        <div className="animate-pop-card flex flex-col gap-2.5 rounded-md border-2 border-[#FF7A00] bg-[#240D02] px-4 py-3 text-xs font-semibold text-orange-100 shadow-[4px_4px_0px_#FF7A00]">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 shrink-0 text-[#FFE600]" />
            <span>
              Last rolled: <strong className="text-[#39FF14]">{lastGeneratedPrompt.title}</strong>
              {lastGeneratedPrompt.releaseYear ? ` (${lastGeneratedPrompt.releaseYear})` : ''}. Watched it?
            </span>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={() => onMarkLastSeen(lastGeneratedPrompt.imdbId)}
              className="inline-flex items-center gap-1 rounded border-2 border-[#39FF14] bg-[#39FF14] px-3 py-1 font-extrabold text-[#090118]"
            >
              <Check className="h-3.5 w-3.5" />
              <span>Mark Seen</span>
            </button>
            <button
              type="button"
              onClick={onDismissPrompt}
              className="rounded border-2 border-purple-500/70 bg-[#14062B] px-2.5 py-1 text-purple-200 hover:border-white"
            >
              Skip
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
