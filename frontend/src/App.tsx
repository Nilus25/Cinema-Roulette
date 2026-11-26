import { useCallback, useEffect, useState } from 'react';
import confetti from 'canvas-confetti';
import { Clapperboard, Dices } from 'lucide-react';
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
    particleCount: 75,
    spread: 85,
    origin: { y: 0.6 },
    colors: ['#FF0055', '#FF6B00', '#FFE600', '#39FF14', '#00E5FF', '#9D4EDD', '#FF0099'],
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
    <div className="min-h-screen pb-10">
      <div className="rainbow-strip h-2 w-full" />

      <div className="mx-auto max-w-3xl px-4 py-5 space-y-4">
        {/* 90s Swing JFrame Main Header */}
        <header className="swing-window">
          <div className="swing-titlebar-purple flex items-center justify-between px-3 py-1.5 text-white">
            <div className="flex items-center gap-2 font-display text-xs font-bold uppercase">
              <Clapperboard className="h-4 w-4 text-[#39FF14]" />
              <span>IMDBSelector.exe</span>
            </div>
            <div className="flex gap-1 text-[10px] font-bold">
              <span className="swing-btn bg-[#2B0F54] px-1.5">_</span>
              <span className="swing-btn bg-[#2B0F54] px-1.5">[]</span>
              <span className="swing-btn bg-[#FF0080] px-1.5">X</span>
            </div>
          </div>
          <div className="flex flex-wrap items-center justify-between gap-2 px-4 py-3">
            <h1 className="font-display text-xl sm:text-2xl font-bold text-[#39FF14] drop-shadow-[0_0_12px_rgba(57,255,20,0.7)]">
              IMDBSELECTOR
            </h1>
            <span className="border border-[#FF0080] bg-[#FF0080]/20 px-2.5 py-0.5 text-[11px] font-bold uppercase text-[#FF6BB0]">
              90s Neon Edition
            </span>
          </div>
        </header>

        {error && (
          <div className="swing-window border-[#FF1E56] bg-[#2B0410] px-4 py-2.5 text-xs font-bold text-[#FF8CA0]">
            ERROR: {error}
          </div>
        )}

        {/* Zone 1: Profile & Watchlist Windows */}
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

        {/* Zone 2: Picker Window */}
        <section className="swing-window">
          <div className="swing-titlebar-purple flex items-center gap-2 px-3 py-1.5 font-display text-xs font-bold uppercase text-white">
            <Dices className="h-3.5 w-3.5 text-[#39FF14]" />
            <span>Random_Picker_Panel</span>
          </div>
          <div className="p-4 space-y-4">
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
          </div>
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
