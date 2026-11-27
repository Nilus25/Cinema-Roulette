import type {
  FilterMetadata,
  FilterState,
  PagedWatchlistItemsResponse,
  QueryFlavor,
  SelectionResponse,
  SortBy,
  SortDirection,
  UserDetailsResponse,
  UserResponse,
  WatchlistResponse,
  WatchlistUpdateApplyOptions,
  WatchlistUpdatePreviewResponse,
  WatchlistVisibility,
} from '../types/api';

const BASE = import.meta.env.VITE_API_BASE_URL ?? '';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    credentials: 'include',
  });
  if (!res.ok) {
    let message = `HTTP ${res.status}`;
    try {
      const body = await res.json();
      const detail = body?.detail ?? body?.message;
      if (detail) {
        message = res.status >= 500 && body?.traceId
          ? `${detail} (Ref: ${body.traceId})`
          : detail;
      }
    } catch {
      // ignore non-JSON error body
    }
    throw new Error(message);
  }
  if (res.status === 204) {
    return undefined as T;
  }
  return res.json() as Promise<T>;
}

function buildFilterParams(
  filter: FilterState,
  sortBy?: SortBy,
  sortDir?: SortDirection,
  page?: number,
  size?: number,
  quickSearch?: string,
): URLSearchParams {
  const params = new URLSearchParams();
  if (filter.minRating) params.set('minRating', filter.minRating);
  if (filter.maxRating) params.set('maxRating', filter.maxRating);
  if (filter.minYear) params.set('minYear', filter.minYear);
  if (filter.maxYear) params.set('maxYear', filter.maxYear);
  if (filter.minRuntime) params.set('minRuntime', filter.minRuntime);
  if (filter.maxRuntime) params.set('maxRuntime', filter.maxRuntime);
  if (filter.minVotes) params.set('minVotes', filter.minVotes);
  if (filter.maxVotes) params.set('maxVotes', filter.maxVotes);
  if (filter.genres.length > 0) params.set('genres', filter.genres.join(','));
  if (filter.mediaTypes.length > 0) params.set('mediaTypes', filter.mediaTypes.join(','));
  if (filter.keywords.trim()) params.set('keywords', filter.keywords.trim());
  if (filter.excludeSeen) params.set('excludeSeen', 'true');
  if (filter.query.trim()) params.set('query', filter.query.trim());
  if (quickSearch && quickSearch.trim()) params.set('quickSearch', quickSearch.trim());
  if (sortBy && sortBy !== 'DEFAULT') {
    params.set('sortBy', sortBy);
    if (sortDir) params.set('sortDir', sortDir);
  }
  if (page !== undefined) params.set('page', String(page));
  if (size !== undefined) params.set('size', String(size));
  return params;
}

export const api = {
  getFilterMetadata: () => request<FilterMetadata>('/api/metadata/filters'),

  register: (username: string, password: string) =>
    request<UserDetailsResponse>('/api/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }),
  login: (username: string, password: string) =>
    request<UserDetailsResponse>('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password }),
    }),
  logout: () => request<void>('/api/auth/logout', { method: 'POST' }),
  me: () => request<UserDetailsResponse>('/api/auth/me'),
  updatePreferences: (queryFlavor: QueryFlavor) =>
    request<UserResponse>('/api/users/me/preferences', {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ queryFlavor }),
    }),
  changePassword: (currentPassword: string, newPassword: string) =>
    request<void>('/api/auth/password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ currentPassword, newPassword }),
    }),
  downloadExport: async () => {
    const res = await fetch(`${BASE}/api/users/me/export`, {
      credentials: 'include',
    });
    if (!res.ok) {
      throw new Error(`Export failed (HTTP ${res.status})`);
    }
    const disposition = res.headers.get('Content-Disposition') ?? '';
    const match = disposition.match(/filename="([^"]+)"/);
    const filename = match ? match[1] : 'cinema-roulette-backup.json';
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  },

  markSeen: (imdbId: string) =>
    request<UserResponse>(`/api/users/me/seen/${imdbId}`, { method: 'POST' }),
  unmarkSeen: (imdbId: string) =>
    request<UserResponse>(`/api/users/me/seen/${imdbId}`, { method: 'DELETE' }),

  listWatchlists: () => request<WatchlistResponse[]>('/api/watchlists'),
  listPublicWatchlists: () => request<WatchlistResponse[]>('/api/watchlists/public'),
  importWatchlist: (name: string, file: File) => {
    const form = new FormData();
    form.append('name', name);
    form.append('file', file);
    return request<WatchlistResponse>('/api/watchlists', { method: 'POST', body: form });
  },
  previewWatchlistCsv: (watchlistId: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<WatchlistUpdatePreviewResponse>(`/api/watchlists/${watchlistId}/import/preview`, {
      method: 'POST',
      body: form,
    });
  },
  updateWatchlistCsv: (watchlistId: string, file: File, options?: WatchlistUpdateApplyOptions) => {
    const form = new FormData();
    form.append('file', file);
    if (options?.excludeAddedIds.length) {
      form.append('excludeAddedIds', options.excludeAddedIds.join(','));
    }
    if (options?.keepRemovedIds.length) {
      form.append('keepRemovedIds', options.keepRemovedIds.join(','));
    }
    if (options?.markSeenIds.length) {
      form.append('markSeenIds', options.markSeenIds.join(','));
    }
    return request<WatchlistResponse>(`/api/watchlists/${watchlistId}/import`, {
      method: 'PUT',
      body: form,
    });
  },
  changeVisibility: (watchlistId: string, visibility: WatchlistVisibility) =>
    request<WatchlistResponse>(`/api/watchlists/${watchlistId}/visibility`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ visibility }),
    }),
  subscribeByCode: (shareCode: string) =>
    request<void>('/api/watchlists/subscribe', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ shareCode }),
    }),
  subscribePublic: (watchlistId: string) =>
    request<void>(`/api/watchlists/${watchlistId}/subscribe`, { method: 'POST' }),
  unsubscribe: (watchlistId: string) =>
    request<void>(`/api/watchlists/${watchlistId}/subscribe`, { method: 'DELETE' }),
  deleteWatchlist: (watchlistId: string) =>
    request<void>(`/api/watchlists/${watchlistId}`, { method: 'DELETE' }),

  getWatchlistItems: (
    watchlistId: string,
    filter: FilterState,
    sortBy?: SortBy,
    sortDir?: SortDirection,
    page = 0,
    size = 40,
    quickSearch?: string,
  ) => {
    const qs = buildFilterParams(filter, sortBy, sortDir, page, size, quickSearch).toString();
    return request<PagedWatchlistItemsResponse>(`/api/watchlists/${watchlistId}/items${qs ? `?${qs}` : ''}`);
  },

  selectRandom: (watchlistId: string, filter: FilterState) =>
    request<SelectionResponse>(`/api/watchlists/${watchlistId}/select`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        minRating: filter.minRating ? Number(filter.minRating) : null,
        maxRating: filter.maxRating ? Number(filter.maxRating) : null,
        minYear: filter.minYear ? Number(filter.minYear) : null,
        maxYear: filter.maxYear ? Number(filter.maxYear) : null,
        minRuntime: filter.minRuntime ? Number(filter.minRuntime) : null,
        maxRuntime: filter.maxRuntime ? Number(filter.maxRuntime) : null,
        minVotes: filter.minVotes ? Number(filter.minVotes) : null,
        maxVotes: filter.maxVotes ? Number(filter.maxVotes) : null,
        genres: filter.genres,
        mediaTypes: filter.mediaTypes,
        keywords: filter.keywords
          .split(',')
          .map((k) => k.trim())
          .filter(Boolean),
        excludeSeen: filter.excludeSeen,
        query: filter.query.trim() || null,
      }),
    }),
};
