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
      <div className="swing-window">
        <div className="swing-titlebar-green flex items-center justify-between px-2.5 py-1.5 text-black">
          <div className="flex items-center gap-1.5 font-display text-xs font-bold uppercase">
            <User className="h-3.5 w-3.5" />
            <span>Profile.exe</span>
          </div>
          <button
            type="button"
            onClick={() => setCreating((v) => !v)}
            className="swing-btn inline-flex items-center gap-1 bg-[#1B0938] px-2 py-0.5 text-[10px] font-bold uppercase text-[#39FF14]"
          >
            {creating ? <X className="h-3 w-3" /> : <Plus className="h-3 w-3" />}
            <span>{creating ? 'Close' : 'New'}</span>
          </button>
        </div>

        <div className="p-3 space-y-2.5">
          {creating && (
            <form onSubmit={handleCreate} className="flex gap-2">
              <input
                type="text"
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="USER NAME..."
                className="swing-inset flex-1 px-2.5 py-1.5 text-xs text-[#39FF14] placeholder-purple-400/50 focus:outline-none"
              />
              <button
                type="submit"
                className="swing-btn bg-[#39FF14] px-3 py-1.5 text-xs font-bold uppercase text-black"
              >
                OK
              </button>
            </form>
          )}

          <div className="flex gap-2">
            <select
              value={selectedUserId ?? ''}
              onChange={(e) => onSelectUser(e.target.value || null)}
              className="swing-inset flex-1 px-2.5 py-2 text-xs text-slate-100 focus:outline-none"
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
                className="swing-btn inline-flex items-center justify-center bg-[#FF1E56] px-2.5 py-2 text-white"
              >
                <Trash2 className="h-4 w-4" />
              </button>
            )}
          </div>
        </div>
      </div>

      {lastGeneratedPrompt && (
        <div className="swing-window border-[#FF6B00] bg-[#240B00] p-3 text-xs text-[#FFB380]">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <Sparkles className="h-4 w-4 shrink-0 text-[#FFE600]" />
              <span>
                LAST PICK: <strong className="text-[#39FF14]">{lastGeneratedPrompt.title}</strong>
                {lastGeneratedPrompt.releaseYear ? ` (${lastGeneratedPrompt.releaseYear})` : ''} — WATCHED?
              </span>
            </div>
            <div className="flex gap-2 shrink-0">
              <button
                type="button"
                onClick={() => onMarkLastSeen(lastGeneratedPrompt.imdbId)}
                className="swing-btn inline-flex items-center gap-1 bg-[#39FF14] px-2.5 py-1 font-bold uppercase text-black"
              >
                <Check className="h-3.5 w-3.5" />
                <span>Seen</span>
              </button>
              <button
                type="button"
                onClick={onDismissPrompt}
                className="swing-btn bg-[#2D1154] px-2.5 py-1 uppercase text-purple-200"
              >
                Skip
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
