import { useState } from 'react';
import { Clapperboard, Lock, LogIn, UserPlus } from 'lucide-react';

interface AuthPanelProps {
  onLogin: (username: string, password: string) => Promise<void>;
  onRegister: (username: string, password: string) => Promise<void>;
  error: string | null;
}

export function AuthPanel({ onLogin, onRegister, error }: AuthPanelProps) {
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!username.trim() || !password) return;
    setSubmitting(true);
    try {
      if (mode === 'login') {
        await onLogin(username.trim(), password);
      } else {
        await onRegister(username.trim(), password);
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="neon-curtains min-h-screen flex flex-col items-center justify-center px-4 py-10">
      <div aria-hidden="true" className="neon-spotlight-left hidden sm:block" />
      <div aria-hidden="true" className="neon-spotlight-right hidden sm:block" />
      <div aria-hidden="true" className="neon-carpet-stage">
        <div className="neon-carpet" />
      </div>
      <div className="relative z-10 mb-3 rounded border border-[#FF1E56]/80 bg-[#1F0622]/90 px-3 py-1 font-mono text-[10px] font-black uppercase tracking-[0.25em] text-[#FF7A91] shadow-[0_0_18px_rgba(255,0,60,0.55)]">
        NEON LOBBY &middot; ADMIT ONE
      </div>
      <div className="neo-panel-purple relative z-10 w-full max-w-md overflow-hidden">
        <div className="flex items-center gap-3.5 border-b-2 border-[#B5179E]/60 bg-[#0D031F] px-6 py-5">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded border-2 border-[#39FF14] bg-[#090118] text-[#39FF14] shadow-[0_0_16px_rgba(57,255,20,0.5)]">
            <Clapperboard className="h-6 w-6" />
          </div>
          <div>
            <h1 className="font-display text-2xl font-extrabold tracking-tight text-[#39FF14] drop-shadow-[0_0_12px_rgba(57,255,20,0.55)]">
              Cinema Roulette
            </h1>
            <p className="text-xs font-semibold text-[#FF8DC7]">
              Spin your watchlist. Watch something already.
            </p>
          </div>
        </div>

        <div className="p-6 space-y-5">
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => setMode('login')}
              className={`inline-flex items-center justify-center gap-2 rounded border-2 py-2 text-xs font-extrabold transition ${
                mode === 'login'
                  ? 'border-[#39FF14] bg-[#39FF14] text-[#090118]'
                  : 'border-purple-500/50 bg-[#090118] text-purple-200'
              }`}
            >
              <LogIn className="h-3.5 w-3.5" />
              <span>Sign In</span>
            </button>
            <button
              type="button"
              onClick={() => setMode('register')}
              className={`inline-flex items-center justify-center gap-2 rounded border-2 py-2 text-xs font-extrabold transition ${
                mode === 'register'
                  ? 'border-[#FF2A85] bg-[#FF2A85] text-white'
                  : 'border-purple-500/50 bg-[#090118] text-purple-200'
              }`}
            >
              <UserPlus className="h-3.5 w-3.5" />
              <span>Create Account</span>
            </button>
          </div>

          {error && (
            <div className="rounded border-2 border-[#FF2E54] bg-[#290510] px-3.5 py-2.5 text-xs font-bold text-rose-200">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-3.5">
            <div className="space-y-1">
              <label className="block text-xs font-bold text-purple-200">Username</label>
              <input
                type="text"
                autoComplete="username"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder="your_handle"
                className="w-full rounded border-2 border-purple-500/70 bg-[#090118] px-3.5 py-2.5 text-sm text-white placeholder-purple-300/40 focus:border-[#39FF14] focus:outline-none"
              />
            </div>

            <div className="space-y-1">
              <label className="block text-xs font-bold text-purple-200">Password</label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3 top-3 h-4 w-4 text-purple-400" />
                <input
                  type="password"
                  autoComplete={mode === 'login' ? 'current-password' : 'new-password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder={mode === 'register' ? 'At least 6 characters' : '••••••••'}
                  className="w-full rounded border-2 border-purple-500/70 bg-[#090118] pl-9 pr-3.5 py-2.5 text-sm text-white placeholder-purple-300/40 focus:border-[#39FF14] focus:outline-none"
                />
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting || !username.trim() || !password}
              className="neo-btn-primary w-full py-3 font-display text-sm font-extrabold uppercase tracking-wide disabled:cursor-not-allowed"
            >
              {submitting ? 'Loading...' : mode === 'login' ? 'Enter Cinema' : 'Create & Enter'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
