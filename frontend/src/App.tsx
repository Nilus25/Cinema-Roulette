import { useCallback, useEffect, useMemo, useState } from 'react';
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
  UserDetailsResponse,
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
    particleCount: 75,
    spread: 85,
    origin: { y: 0.6 },
    colors: ['#FF1E56', '#FF7A00', '#FFE600', '#39FF14', '#00E5FF', '#9D4EDD', '#FF1E8A'],
  });
}

function mostFrequentGenre(items: MediaItemResponse[]): string | null {
  const counts = new Map<string, number>();
  for (const item of items) {
    for (const g of item.genres) {
      counts.set(g, (counts.get(g) ?? 0) + 1);
    }
  }
  let top: string | null = null;
  let max = 0;
  for (const [genre, count] of counts) {
    if (count > max) {
      max = count;
      top = genre;
    }
  }
  return top;
}

export function App() {
  const [metadata, setMetadata] = useState<FilterMetadata | null>(null);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [watchlists, setWatchlists] = useState<WatchlistResponse[]>([]);
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);
  const [selectedWatchlistId, setSelectedWatchlistId] = useState<string | null>(null);
  const [filter, setFilter] = useState<FilterState>(EMPTY_FILTER);

  const [userDetails, setUserDetails] = useState<UserDetailsResponse | null>(null);
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

  const refreshUserDetails = useCallback(async (userId: string | null) => {
    if (!userId) {
      setUserDetails(null);
      setLastGeneratedPrompt(null);
      return;
    }
    const details = await api.getUserDetails(userId);
    setUserDetails(details);
    setLastGeneratedPrompt(details.unseenLastGeneratedPrompt);
  }, []);

  useEffect(() => {
    Promise.all([api.getFilterMetadata(), refreshLists()])
      .then(([meta]) => setMetadata(meta))
      .catch((e) => setError(e.message));
  }, [refreshLists]);

  useEffect(() => {
    refreshUserDetails(selectedUserId).catch((e) => setError(e.message));
  }, [selectedUserId, refreshUserDetails]);

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

  const profileStats = useMemo(() => {
    if (!userDetails) return null;
    const seenInWatchlist = items.filter((i) => i.seen).length;
    return {
      totalSeen: userDetails.seenItems.length,
      seenInWatchlist,
      watchlistTotal: items.length,
      topGenre: mostFrequentGenre(userDetails.seenItems),
    };
  }, [userDetails, items]);

  const watchlistStats = useMemo(() => {
    if (!selectedWatchlistId) return null;
    const rated = items.map((i) => i.item.rating).filter((r): r is number => r !== null);
    const avg = rated.length > 0 ? (rated.reduce((a, b) => a + b, 0) / rated.length).toFixed(1) : null;
    return {
      total: items.length,
      unseenCount: items.filter((i) => !i.seen).length,
      avgRating: avg,
      topGenre: mostFrequentGenre(items.map((i) => i.item)),
    };
  }, [selectedWatchlistId, items]);

  return (
    <div className="min-h-screen pb-12">
      <div className="mx-auto max-w-[1680px] px-4 sm:px-6 lg:px-8 py-6 space-y-4">
        {error && (
          <div className="rounded border-2 border-[#FF2E54] bg-[#290510] px-4 py-3 text-xs font-bold text-rose-200 shadow-[4px_4px_0px_#FF2E54]">
            {error}
          </div>
        )}

        {/* 3-Column Uneven Desktop Layout with wide 1fr center */}
        <div className="grid grid-cols-1 lg:grid-cols-[270px_1fr_310px] gap-6 items-start">
          {/* Left Column: Profile + Stats */}
          <aside className="order-2 lg:order-1">
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
                  await Promise.all([refreshLists(), refreshUserDetails(selectedUserId), loadItems()]);
                })
              }
              onDismissPrompt={() => setLastGeneratedPrompt(null)}
              stats={profileStats}
            />
          </aside>

          {/* Center Column: Unified Title + Filters + Picker Stage (1fr wide) */}
          <main className="order-1 lg:order-2 min-w-0">
            <section className="neo-panel-purple flex overflow-hidden">
              <div className="filmstrip-rail hidden sm:block border-r-2 border-[#B5179E]/45" />
              <div className="flex-1 min-w-0">
                <header className="flex items-center gap-3.5 border-b-2 border-[#B5179E]/60 bg-[#0D031F] px-5 py-4">
                  <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded border-2 border-[#39FF14] bg-[#090118] text-[#39FF14] shadow-[0_0_16px_rgba(57,255,20,0.5)]">
                    <Clapperboard className="h-6 w-6" />
                  </div>
                  <div>
                    <h1 className="font-display text-2xl sm:text-3xl font-extrabold tracking-tight text-[#39FF14] drop-shadow-[0_0_12px_rgba(57,255,20,0.55)]">
                      IMDBSelector
                    </h1>
                    <p className="text-xs font-semibold text-[#FF8DC7]">
                      Pick something to watch without the endless scroll
                    </p>
                  </div>
                </header>

                <div className="p-5 space-y-5">
                  <FilterPanel
                    metadata={metadata}
                    filter={filter}
                    hasUser={Boolean(selectedUserId)}
                    onChange={setFilter}
                    onReset={() => setFilter(EMPTY_FILTER)}
                  />

                  <PickCard
                    canPick={Boolean(selectedWatchlistId)}
                    candidateCount={items.length}
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
                        await Promise.all([refreshLists(), refreshUserDetails(selectedUserId), loadItems()]);
                      })
                    }
                  />
                </div>
              </div>
              <div className="filmstrip-rail hidden sm:block border-l-2 border-[#B5179E]/45" />
            </section>
          </main>

          {/* Right Column: Watchlist + Stats + Vault */}
          <aside className="order-3 space-y-4">
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
              stats={watchlistStats}
            />

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
                    await Promise.all([refreshLists(), refreshUserDetails(selectedUserId), loadItems()]);
                  })
                }
              />
            )}
          </aside>
        </div>
      </div>
    </div>
  );
}

export default App;
