import { useState } from 'react';
import { Check, Download, KeyRound, X } from 'lucide-react';
import { api } from '../api/client';

interface AccountModalProps {
  username: string;
  onClose: () => void;
}

export function AccountModal({ username, onClose }: AccountModalProps) {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [savingPassword, setSavingPassword] = useState(false);
  const [passwordError, setPasswordError] = useState<string | null>(null);
  const [passwordSuccess, setPasswordSuccess] = useState(false);

  const [exporting, setExporting] = useState(false);
  const [exportError, setExportError] = useState<string | null>(null);
  const [exportSuccess, setExportSuccess] = useState(false);

  const handleChangePassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setPasswordError(null);
    setPasswordSuccess(false);

    if (newPassword.length < 6) {
      setPasswordError('New password must be at least 6 characters.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setPasswordError('New password and confirmation do not match.');
      return;
    }
    if (newPassword === currentPassword) {
      setPasswordError('New password must be different from your current password.');
      return;
    }

    setSavingPassword(true);
    try {
      await api.changePassword(currentPassword, newPassword);
      setCurrentPassword('');
      setNewPassword('');
      setConfirmPassword('');
      setPasswordSuccess(true);
    } catch (err) {
      setPasswordError((err as Error).message);
    } finally {
      setSavingPassword(false);
    }
  };

  const handleExport = async () => {
    setExportError(null);
    setExportSuccess(false);
    setExporting(true);
    try {
      await api.downloadExport();
      setExportSuccess(true);
    } catch (err) {
      setExportError((err as Error).message);
    } finally {
      setExporting(false);
    }
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm"
      role="dialog"
      aria-modal="true"
      aria-labelledby="account-modal-title"
    >
      <div className="neo-panel-purple flex max-h-[90vh] w-full max-w-md flex-col overflow-hidden p-5">
        <div className="flex items-start justify-between gap-4 border-b border-[#B5179E]/45 pb-3">
          <div>
            <h3 id="account-modal-title" className="font-display text-lg font-extrabold text-[#39FF14]">
              Account &amp; Backup
            </h3>
            <p className="text-xs text-purple-200/80">
              Signed in as <strong className="text-white">{username}</strong>
            </p>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Close account modal"
            className="rounded border border-purple-400/50 p-1 text-purple-200 hover:border-[#FF1E8A] hover:text-[#FF1E8A]"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        <div className="mt-4 space-y-5 overflow-y-auto pr-1">
          <form onSubmit={handleChangePassword} className="space-y-3">
            <div className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-wider text-[#FF8DC7]">
              <KeyRound className="h-3.5 w-3.5 text-[#39FF14]" />
              <span>Change Password</span>
            </div>

            {passwordError && (
              <div className="rounded border border-[#FF2E54] bg-[#290510] px-3 py-2 text-xs font-semibold text-rose-200">
                {passwordError}
              </div>
            )}
            {passwordSuccess && (
              <div className="flex items-center gap-1.5 rounded border border-[#39FF14] bg-[#07240D] px-3 py-2 text-xs font-semibold text-[#39FF14]">
                <Check className="h-3.5 w-3.5 shrink-0" />
                <span>Password updated successfully.</span>
              </div>
            )}

            <div className="space-y-2 text-xs">
              <label className="block space-y-1">
                <span className="font-semibold text-purple-200/85">Current Password</span>
                <input
                  type="password"
                  required
                  autoComplete="current-password"
                  value={currentPassword}
                  onChange={(e) => setCurrentPassword(e.target.value)}
                  className="w-full rounded border border-[#B5179E]/60 bg-[#090118] px-3 py-2 text-white outline-none focus:border-[#39FF14]"
                />
              </label>

              <label className="block space-y-1">
                <span className="font-semibold text-purple-200/85">New Password</span>
                <input
                  type="password"
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  className="w-full rounded border border-[#B5179E]/60 bg-[#090118] px-3 py-2 text-white outline-none focus:border-[#39FF14]"
                />
              </label>

              <label className="block space-y-1">
                <span className="font-semibold text-purple-200/85">Confirm New Password</span>
                <input
                  type="password"
                  required
                  minLength={6}
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  className="w-full rounded border border-[#B5179E]/60 bg-[#090118] px-3 py-2 text-white outline-none focus:border-[#39FF14]"
                />
              </label>
            </div>

            <button
              type="submit"
              disabled={savingPassword}
              className="w-full rounded border-2 border-[#39FF14] bg-[#39FF14] px-3 py-2 text-xs font-extrabold text-[#090118] transition hover:bg-[#6CFF4D] disabled:opacity-50"
            >
              {savingPassword ? 'Updating...' : 'Update Password'}
            </button>
          </form>

          <div className="border-t border-[#B5179E]/40 pt-4 space-y-2.5">
            <div className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-wider text-[#FF8DC7]">
              <Download className="h-3.5 w-3.5 text-[#00E5FF]" />
              <span>Export My Data</span>
            </div>
            <p className="text-xs text-purple-200/75">
              Download a JSON backup of your seen history, owned watchlists, and subscribed list summaries.
            </p>

            {exportError && (
              <div className="rounded border border-[#FF2E54] bg-[#290510] px-3 py-2 text-xs font-semibold text-rose-200">
                {exportError}
              </div>
            )}
            {exportSuccess && (
              <div className="flex items-center gap-1.5 rounded border border-[#00E5FF] bg-[#041F29] px-3 py-2 text-xs font-semibold text-[#00E5FF]">
                <Check className="h-3.5 w-3.5 shrink-0" />
                <span>Backup downloaded.</span>
              </div>
            )}

            <button
              type="button"
              onClick={handleExport}
              disabled={exporting}
              className="inline-flex w-full items-center justify-center gap-2 rounded border-2 border-[#00E5FF] bg-[#090118] px-3 py-2 text-xs font-extrabold text-[#00E5FF] transition hover:bg-[#00E5FF] hover:text-[#090118] disabled:opacity-50"
            >
              <Download className="h-3.5 w-3.5" />
              <span>{exporting ? 'Preparing Backup...' : 'Download My Data (.json)'}</span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
