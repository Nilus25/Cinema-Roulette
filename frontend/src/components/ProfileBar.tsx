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
      <div className="border-2 border-[#39FF14]/70 bg-[#0C041A]/95 p-3.5 space-y-2.5 shadow-[0_0_12px_rgba(57,255,20,0.25)]">
        <div className="flex items-center justify-between gap-2">
          <div className="flex items-center gap-2 font-display text-xs uppercase tracking-wider text-[#39FF14]">
            <User className="h-3.5 w-3.5" />
            <span>Profile</span>
          </div>
          <button
            type="button"
            onClick={() => setCreating((v) => !v)}
            className="inline-flex items-center gap-1 border border-[#39FF14] bg-[#39FF14]/15 px-2.5 py-0.5 text-[11px] font-bold uppercase text-[#39FF14] hover:bg-[#39FF14] hover:text-black"
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
              placeholder="PROFILE NAME..."
              className="flex-1 border border-[#39FF14]/60 bg-[#05010D] px-3 py-1.5 text-xs text-[#39FF14] placeholder-slate-600 focus:border-[#39FF14] focus:outline-none"
            />
            <button
              type="submit"
              className="border border-[#39FF14] bg-[#39FF14] px-3 py-1.5 text-xs font-bold uppercase text-black hover:brightness-110"
            >
              OK
            </button>
          </form>
        )}

        <div className="flex gap-2">
          <select
            value={selectedUserId ?? ''}
            onChange={(e) => onSelectUser(e.target.value || null)}
            className="flex-1 border border-purple-500/60 bg-[#05010D] px-3 py-2 text-xs text-slate-100 focus:border-[#39FF14] focus:outline-none"
          >
            <option value="">-- SELECT PROFILE --</option>
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
              className="inline-flex items-center justify-center border border-[#FF2A55] bg-[#FF2A55]/15 px-3 py-2 text-[#FF2A55] hover:bg-[#FF2A55] hover:text-black"
            >
              <Trash2 className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>

      {lastGeneratedPrompt && (
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 border-2 border-[#FF7A59] bg-[#220B06] px-4 py-3 text-xs text-[#FFB399] shadow-[0_0_15px_rgba(255,122,89,0.4)]">
          <div className="flex items-center gap-2">
            <Sparkles className="h-4 w-4 shrink-0 text-[#FF9E44]" />
            <span>
              LAST PICK: <strong className="text-white">{lastGeneratedPrompt.title}</strong>
              {lastGeneratedPrompt.releaseYear ? ` (${lastGeneratedPrompt.releaseYear})` : ''} — WATCHED?
            </span>
          </div>
          <div className="flex gap-2 shrink-0">
            <button
              type="button"
              onClick={() => onMarkLastSeen(lastGeneratedPrompt.imdbId)}
              className="inline-flex items-center gap-1 border border-[#39FF14] bg-[#39FF14] px-3 py-1 font-bold uppercase text-black"
            >
              <Check className="h-3.5 w-3.5" />
              <span>Seen</span>
            </button>
            <button
              type="button"
              onClick={onDismissPrompt}
              className="border border-purple-500/60 bg-[#05010D] px-2.5 py-1 uppercase text-slate-300 hover:border-white"
            >
              Skip
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
