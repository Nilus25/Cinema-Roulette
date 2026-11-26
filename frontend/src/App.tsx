import { useCallback, useEffect, useState } from 'react';
import confetti from 'canvas-confetti';
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

function fireRainbowConfetti() {
  confetti({
    particleCount: 70,
    spread: 80,
    origin: { y: 0.6 },
    colors: ['#FF3B6B', '#FF8C42', '#FFE600', '#39FF14', '#00E5FF', '#A855F7', '#FF49B0'],
  });
}

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
    <div className="relative min-h-screen overflow-hidden pb-12">
      {/* Top Rainbow Ribbon */}
      <div className="rainbow-bar h-2.5 w-full shadow-md" />

      {/* Decorative Floating Soap Bubbles */}
      <div className="pointer-events-none fixed inset-0 overflow-hidden">
        <div className="soap-bubble absolute -top-10 -right-10 h-44 w-44 opacity-80" />
        <div className="soap-bubble absolute top-1/3 -left-12 h-32 w-32 opacity-70" style={{ animationDelay: '1.5s' }} />
        <div className="soap-bubble absolute bottom-8 right-6 h-24 w-24 opacity-75" style={{ animationDelay: '3s' }} />
      </div>

      <div className="relative z-10 mx-auto max-w-3xl px-4 py-6 sm:py-8 space-y-5">
        {/* Whimsical Header */}
        <header className="aero-card flex items-center justify-between rounded-full px-5 py-3">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-full bg-gradient-to-br from-[#FF49B0] via-purple-500 to-[#39FF14] text-white shadow-md">
              <Clapperboard className="h-5 w-5" />
            </div>
            <div>
              <h1 className="font-display text-2xl font-bold tracking-tight bg-gradient-to-r from-purple-700 via-[#FF2A85] to-orange-500 bg-clip-text text-transparent">
                IMDBSelector
              </h1>
              <p className="text-xs font-bold text-sky-600">Pick something fun without the scroll</p>
            </div>
          </div>
        </header>

        {error && (
          <div className="animate-pop-in rounded-3xl border-3 border-rose-400 bg-rose-50 px-4 py-3 text-sm font-bold text-rose-600 shadow-md">
            {error}
          </div>
        )}

        {/* Zone 1: Profile & Watchlist */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
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
        <section className="aero-card rounded-[2rem] p-5 space-y-5">
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
                  fireRainbowConfetti();
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
