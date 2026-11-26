import { useCallback, useEffect, useState } from 'react';
import { Clapperboard } from 'lucide-react';
import { api } from './api/client';
import { FilterPanel } from './components/FilterPanel';
import { PickCard } from './components/PickCard';
import { ProfileBar } from './components/ProfileBar';
import { WatchlistBar } from './components/WatchlistBar';
import { WatchlistBrowser } from './components/WatchlistBrowser';
import type {
  FilterMetadata,
  FilterState,
  MediaItemResponse,
  UserResponse,
  WatchlistItemResponse,
  WatchlistResponse,
} from './types/api';

const EMPTY_FILTER: FilterState = {
  minRating: '',
  maxRating: '',
  minYear: '',
  maxYear: '',
  genres: [],
  mediaTypes: [],
  keywords: '',
  excludeSeen: false,
};

export function App() {
  const [metadata, setMetadata] = useState<FilterMetadata | null>(null);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [watchlists, setWatchlists] = useState<WatchlistResponse[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [selectedWatchlistId, setSelectedWatchlistId] = useState<string | null>(null);
  const [filter, setFilter] = useState<FilterState>(EMPTY_FILTER);

  const [lastGeneratedPrompt, setLastGeneratedPrompt] = useState<MediaItemResponse | null>(null);
  const [picked, setPicked] = useState<MediaItemResponse | null>(null);
  const [items, setItems] = useState<WatchlistItemResponse[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [picking, setPicking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshLists = useCallback(async () => {
    const [u, w] = await Promise.all([api.listUsers(), api.listWatchlists()]);
    setUsers(u);
    setWatchlists(w);
  }, []);

  useEffect(() => {
    Promise.all([api.getFilterMetadata(), refreshLists()])
      .then(([meta]) => setMetadata(meta))
      .catch((e) => setError(e.message));
  }, [refreshLists]);

  useEffect(() => {
    if (!selectedUserId) {
      setLastGeneratedPrompt(null);
      return;
    }
    api
      .getUserDetails(selectedUserId)
      .then((details) => setLastGeneratedPrompt(details.unseenLastGeneratedPrompt))
      .catch((e) => setError(e.message));
  }, [selectedUserId]);

  const loadItems = useCallback(async () => {
    if (!selectedWatchlistId) {
      setItems([]);
      return;
    }
    setLoadingItems(true);
    try {
      const data = await api.getWatchlistItems(selectedWatchlistId, filter, selectedUserId);
      setItems(data);
      setError(null);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoadingItems(false);
    }
  }, [selectedWatchlistId, filter, selectedUserId]);

  useEffect(() => {
    loadItems();
  }, [loadItems]);

  const runAction = async (fn: () => Promise<void>) => {
    setError(null);
    try {
      await fn();
    } catch (e) {
      setError((e as Error).message);
    }
  };

  return (
    <div className="min-h-screen text-slate-100">
      <div className="mx-auto max-w-3xl px-4 py-6 sm:py-8 space-y-5">
        {/* Top Bar */}
        <header className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-[#39FF14]/40 bg-[#150C26] text-[#39FF14] shadow-[0_0_16px_rgba(57,255,20,0.2)]">
              <Clapperboard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="font-display text-2xl font-bold tracking-tight bg-gradient-to-r from-[#39FF14] via-[#A855F7] to-[#FF2A85] bg-clip-text text-transparent">
                IMDBSelector
              </h1>
              <p className="text-xs text-purple-200/70">Midnight watchlist randomizer</p>
            </div>
          </div>
        </header>

        {error && (
          <div className="rounded-2xl border border-[#FF3B5C]/50 bg-[#FF3B5C]/15 px-4 py-3 text-sm text-rose-200">
            {error}
          </div>
        )}

        {/* Zone 1: Profile & Watchlist Controls */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
          <ProfileBar
            users={users}
            selectedUserId={selectedUserId}
            onSelectUser={setSelectedUserId}
            onCreateUser={(name) =>
              runAction(async () => {
                const created = await api.createUser(name);
                await refreshLists();
                setSelectedUserId(created.id);
              })
            }
            onDeleteUser={(id) =>
              runAction(async () => {
                await api.deleteUser(id);
                setSelectedUserId(null);
                await refreshLists();
              })
            }
            lastGeneratedPrompt={lastGeneratedPrompt}
            onMarkLastSeen={(imdbId) =>
              runAction(async () => {
                if (!selectedUserId) return;
                await api.markSeen(selectedUserId, imdbId);
                setLastGeneratedPrompt(null);
                await Promise.all([refreshLists(), loadItems()]);
              })
            }
            onDismissPrompt={() => setLastGeneratedPrompt(null)}
          />

          <WatchlistBar
            watchlists={watchlists}
            selectedWatchlistId={selectedWatchlistId}
            onSelectWatchlist={setSelectedWatchlistId}
            onImportWatchlist={(name, file) =>
              runAction(async () => {
                const created = await api.importWatchlist(name, file);
                await refreshLists();
                setSelectedWatchlistId(created.id);
              })
            }
            onUpdateWatchlistCsv={(id, file) =>
              runAction(async () => {
                await api.updateWatchlistCsv(id, file);
                await Promise.all([refreshLists(), loadItems()]);
              })
            }
            onDeleteWatchlist={(id) =>
              runAction(async () => {
                await api.deleteWatchlist(id);
                setSelectedWatchlistId(null);
                setPicked(null);
                await refreshLists();
              })
            }
          />
        </div>

        {/* Zone 2: Centerpiece "The Picker" Stage */}
        <section className="neon-marquee-border rounded-3xl bg-[#150C26]/95 p-5 space-y-4">
          <FilterPanel
            metadata={metadata}
            filter={filter}
            hasUser={Boolean(selectedUserId)}
            onChange={setFilter}
            onReset={() => setFilter(EMPTY_FILTER)}
          />

          <PickCard
            canPick={Boolean(selectedWatchlistId)}
            loading={picking}
            picked={picked}
            hasUser={Boolean(selectedUserId)}
            onPick={() =>
              runAction(async () => {
                if (!selectedWatchlistId) return;
                setPicking(true);
                try {
                  const result = await api.selectRandom(selectedWatchlistId, filter, selectedUserId);
                  setPicked(result);
                } finally {
                  setPicking(false);
                }
              })
            }
            onMarkSeen={(imdbId) =>
              runAction(async () => {
                if (!selectedUserId) return;
                await api.markSeen(selectedUserId, imdbId);
                await Promise.all([refreshLists(), loadItems()]);
              })
            }
          />
        </section>

        {/* Zone 3: Watchlist Vault */}
        {selectedWatchlistId && (
          <WatchlistBrowser
            items={items}
            hasUser={Boolean(selectedUserId)}
            loading={loadingItems}
            onToggleSeen={(imdbId, currentlySeen) =>
              runAction(async () => {
                if (!selectedUserId) return;
                if (currentlySeen) {
                  await api.unmarkSeen(selectedUserId, imdbId);
                } else {
                  await api.markSeen(selectedUserId, imdbId);
                }
                await Promise.all([refreshLists(), loadItems()]);
              })
            }
          />
        )}
      </div>
    </div>
  );
}

export default App;
