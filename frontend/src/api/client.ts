import type {
  FilterMetadata,
  FilterState,
  MediaItemResponse,
  UserDetailsResponse,
  UserResponse,
  WatchlistItemResponse,
  WatchlistResponse,
} from '../types/api';

const BASE = import.meta.env.VITE_API_BASE_URL ?? '';

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, init);
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

function buildFilterParams(filter: FilterState, userId?: string | null): URLSearchParams {
  const params = new URLSearchParams();
  if (userId) params.set('userId', userId);
  if (filter.minRating) params.set('minRating', filter.minRating);
  if (filter.maxRating) params.set('maxRating', filter.maxRating);
  if (filter.minYear) params.set('minYear', filter.minYear);
  if (filter.maxYear) params.set('maxYear', filter.maxYear);
  if (filter.genres.length > 0) params.set('genres', filter.genres.join(','));
  if (filter.mediaTypes.length > 0) params.set('mediaTypes', filter.mediaTypes.join(','));
  if (filter.keywords.trim()) params.set('keywords', filter.keywords.trim());
  if (filter.excludeSeen) params.set('excludeSeen', 'true');
  return params;
}

export const api = {
  getFilterMetadata: () => request<FilterMetadata>('/api/metadata/filters'),

  listUsers: () => request<UserResponse[]>('/api/users'),
  createUser: (name: string) =>
    request<UserResponse>('/api/users', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name }),
    }),
  getUserDetails: (userId: string) => request<UserDetailsResponse>(`/api/users/${userId}`),
  deleteUser: (userId: string) => request<void>(`/api/users/${userId}`, { method: 'DELETE' }),
  markSeen: (userId: string, imdbId: string) =>
    request<UserResponse>(`/api/users/${userId}/seen/${imdbId}`, { method: 'POST' }),
  unmarkSeen: (userId: string, imdbId: string) =>
    request<UserResponse>(`/api/users/${userId}/seen/${imdbId}`, { method: 'DELETE' }),

  listWatchlists: () => request<WatchlistResponse[]>('/api/watchlists'),
  importWatchlist: (name: string, file: File) => {
    const form = new FormData();
    form.append('name', name);
    form.append('file', file);
    return request<WatchlistResponse>('/api/watchlists', { method: 'POST', body: form });
  },
  updateWatchlistCsv: (watchlistId: string, file: File) => {
    const form = new FormData();
    form.append('file', file);
    return request<WatchlistResponse>(`/api/watchlists/${watchlistId}/import`, {
      method: 'PUT',
      body: form,
    });
  },
  deleteWatchlist: (watchlistId: string) =>
    request<void>(`/api/watchlists/${watchlistId}`, { method: 'DELETE' }),

  getWatchlistItems: (watchlistId: string, filter: FilterState, userId?: string | null) => {
    const qs = buildFilterParams(filter, userId).toString();
    return request<WatchlistItemResponse[]>(`/api/watchlists/${watchlistId}/items${qs ? `?${qs}` : ''}`);
  },

  selectRandom: (watchlistId: string, filter: FilterState, userId?: string | null) =>
    request<MediaItemResponse>(`/api/watchlists/${watchlistId}/select`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: userId ?? null,
        minRating: filter.minRating ? Number(filter.minRating) : null,
        maxRating: filter.maxRating ? Number(filter.maxRating) : null,
        minYear: filter.minYear ? Number(filter.minYear) : null,
        maxYear: filter.maxYear ? Number(filter.maxYear) : null,
        genres: filter.genres,
        mediaTypes: filter.mediaTypes,
        keywords: filter.keywords
          .split(',')
          .map((k) => k.trim())
          .filter(Boolean),
        excludeSeen: filter.excludeSeen,
      }),
    }),
};
