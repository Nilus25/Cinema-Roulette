import { useCallback, useEffect, useMemo, useState } from 'react';
import confetti from 'canvas-confetti';
import { Clapperboard } from 'lucide-react';
import { api } from './api/client';
import { AuthPanel } from './components/AuthPanel';
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
  const [authChecked, setAuthChecked] = useState(false);
  const [userDetails, setUserDetails] = useState<UserDetailsResponse | null>(null);
  const [authError, setAuthError] = useState<string | null>(null);

  const [metadata, setMetadata] = useState<FilterMetadata | null>(null);
  const [watchlists, setWatchlists] = useState<WatchlistResponse[]>([]);
  const [publicWatchlists, setPublicWatchlists] = useState<WatchlistResponse[]>([]);
  const [selectedWatchlistId, setSelectedWatchlistId] = useState<string | null>(null);
  const [filter, setFilter] = useState<FilterState>(EMPTY_FILTER);

  const [lastGeneratedPrompt, setLastGeneratedPrompt] = useState<MediaItemResponse | null>(null);
  const [picked, setPicked] = useState<MediaItemResponse | null>(null);
  const [items, setItems] = useState<WatchlistItemResponse[]>([]);
  const [loadingItems, setLoadingItems] = useState(false);
  const [picking, setPicking] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refreshWatchlists = useCallback(async () => {
    const [accessible, pub] = await Promise.all([
      api.listWatchlists(),
      api.listPublicWatchlists(),
    ]);
    setWatchlists(accessible);
    setPublicWatchlists(pub);
  }, []);

  const refreshMe = useCallback(async () => {
    const me = await api.me();
    setUserDetails(me);
    setLastGeneratedPrompt(me.unseenLastGeneratedPrompt);
  }, []);

  useEffect(() => {
    Promise.all([api.getFilterMetadata(), api.me().catch(() => null)])
      .then(([meta, me]) => {
        setMetadata(meta);
        if (me) {
          setUserDetails(me);
          setLastGeneratedPrompt(me.unseenLastGeneratedPrompt);
          return refreshWatchlists();
        }
      })
      .catch((e) => setError(e.message))
      .finally(() => setAuthChecked(true));
  }, [refreshWatchlists]);

  const loadItems = useCallback(async () => {
    if (!userDetails || !selectedWatchlistId) {
      setItems([]);
      return;
    }
    setLoadingItems(true);
    try {
      const data = await api.getWatchlistItems(selectedWatchlistId, filter);
      setItems(data);
      setError(null);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoadingItems(false);
    }
  }, [userDetails, selectedWatchlistId, filter]);

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
    if (!userDetails) return { totalSeen: 0, seenInWatchlist: 0, watchlistTotal: 0, topGenre: null };
    return {
      totalSeen: userDetails.seenItems.length,
      seenInWatchlist: items.filter((i) => i.seen).length,
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

  if (!authChecked) {
    return (
      <div className="min-h-screen flex items-center justify-center text-sm font-bold text-purple-200">
        Loading Cinema Roulette...
      </div>
    );
  }

  if (!userDetails) {
    return (
      <AuthPanel
        error={authError}
        onLogin={async (username, password) => {
          setAuthError(null);
          try {
            const me = await api.login(username, password);
            setUserDetails(me);
            setLastGeneratedPrompt(me.unseenLastGeneratedPrompt);
            await refreshWatchlists();
          } catch (e) {
            setAuthError((e as Error).message);
          }
        }}
        onRegister={async (username, password) => {
          setAuthError(null);
          try {
            const me = await api.register(username, password);
            setUserDetails(me);
            setLastGeneratedPrompt(me.unseenLastGeneratedPrompt);
            await refreshWatchlists();
          } catch (e) {
            setAuthError((e as Error).message);
          }
        }}
      />
    );
  }

  return (
    <div className="min-h-screen pb-12">
      <div className="mx-auto max-w-[1680px] px-4 sm:px-6 lg:px-8 py-6 space-y-4">
        {error && (
          <div className="rounded border-2 border-[#FF2E54] bg-[#290510] px-4 py-3 text-xs font-bold text-rose-200 shadow-[4px_4px_0px_#FF2E54]">
            {error}
          </div>
        )}

        <div className="grid grid-cols-1 lg:grid-cols-[270px_1fr_310px] gap-6 items-start">
          {/* Left Column: Logged-in Profile + Stats */}
          <aside className="order-2 lg:order-1">
            <ProfileBar
              username={userDetails.name}
              onLogout={() =>
                runAction(async () => {
                  await api.logout();
                  setUserDetails(null);
                  setSelectedWatchlistId(null);
                  setPicked(null);
                  setItems([]);
                })
              }
              lastGeneratedPrompt={lastGeneratedPrompt}
              onMarkLastSeen={(imdbId) =>
                runAction(async () => {
                  await api.markSeen(imdbId);
                  setLastGeneratedPrompt(null);
                  await Promise.all([refreshMe(), loadItems()]);
                })
              }
              onDismissPrompt={() => setLastGeneratedPrompt(null)}
              stats={profileStats}
            />
          </aside>

          {/* Center Column: Unified Title + Filters + Picker Stage */}
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
                      Cinema Roulette
                    </h1>
                    <p className="text-xs font-semibold text-[#FF8DC7]">
                      Spin your watchlist. Watch something already.
                    </p>
                  </div>
                </header>

                <div className="p-5 space-y-5">
                  <FilterPanel
                    metadata={metadata}
                    filter={filter}
                    hasUser={true}
                    onChange={setFilter}
                    onReset={() => setFilter(EMPTY_FILTER)}
                  />

                  <PickCard
                    canPick={Boolean(selectedWatchlistId)}
                    candidateCount={items.length}
                    loading={picking}
                    picked={picked}
                    hasUser={true}
                    onPick={() =>
                      runAction(async () => {
                        if (!selectedWatchlistId) return;
                        setPicking(true);
                        try {
                          const result = await api.selectRandom(selectedWatchlistId, filter);
                          setPicked(result);
                          fireRainbowConfetti();
                        } finally {
                          setPicking(false);
                        }
                      })
                    }
                    onMarkSeen={(imdbId) =>
                      runAction(async () => {
                        await api.markSeen(imdbId);
                        await Promise.all([refreshMe(), loadItems()]);
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
              publicWatchlists={publicWatchlists}
              selectedWatchlistId={selectedWatchlistId}
              onSelectWatchlist={setSelectedWatchlistId}
              onImportWatchlist={(name, file) =>
                runAction(async () => {
                  const created = await api.importWatchlist(name, file);
                  await refreshWatchlists();
                  setSelectedWatchlistId(created.id);
                })
              }
              onUpdateWatchlistCsv={(id, file) =>
                runAction(async () => {
                  await api.updateWatchlistCsv(id, file);
                  await Promise.all([refreshWatchlists(), loadItems()]);
                })
              }
              onChangeVisibility={(id, visibility) =>
                runAction(async () => {
                  await api.changeVisibility(id, visibility);
                  await refreshWatchlists();
                })
              }
              onSubscribeByCode={(code) =>
                runAction(async () => {
                  await api.subscribeByCode(code);
                  await refreshWatchlists();
                })
              }
              onSubscribePublic={(id) =>
                runAction(async () => {
                  await api.subscribePublic(id);
                  await refreshWatchlists();
                })
              }
              onUnsubscribe={(id) =>
                runAction(async () => {
                  await api.unsubscribe(id);
                  if (selectedWatchlistId === id) {
                    setSelectedWatchlistId(null);
                    setPicked(null);
                  }
                  await refreshWatchlists();
                })
              }
              onDeleteWatchlist={(id) =>
                runAction(async () => {
                  await api.deleteWatchlist(id);
                  setSelectedWatchlistId(null);
                  setPicked(null);
                  await refreshWatchlists();
                })
              }
              stats={watchlistStats}
            />

            {selectedWatchlistId && (
              <WatchlistBrowser
                items={items}
                hasUser={true}
                loading={loadingItems}
                onToggleSeen={(imdbId, currentlySeen) =>
                  runAction(async () => {
                    if (currentlySeen) {
                      await api.unmarkSeen(imdbId);
                    } else {
                      await api.markSeen(imdbId);
                    }
                    await Promise.all([refreshMe(), loadItems()]);
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
