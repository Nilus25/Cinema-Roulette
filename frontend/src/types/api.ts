export interface UserResponse {
  id: string;
  name: string;
  lastGeneratedImdbId: string | null;
  lastGeneratedAt: string | null;
  seenCount: number;
}

export interface MediaItemResponse {
  imdbId: string;
  title: string;
  originalTitle: string | null;
  director: string;
  releaseYear: number | null;
  rating: number | null;
  runtimeMinutes: number | null;
  numVotes: number | null;
  mediaType: string;
  imdbUrl: string;
  genres: string[];
}

export interface UserDetailsResponse {
  id: string;
  name: string;
  unseenLastGeneratedPrompt: MediaItemResponse | null;
  seenItems: MediaItemResponse[];
}

export type WatchlistVisibility = 'PRIVATE' | 'SHARED' | 'PUBLIC';

export interface WatchlistResponse {
  id: string;
  name: string;
  itemCount: number;
  updatedAt: string;
  owned: boolean;
  ownerName: string;
  visibility: WatchlistVisibility;
  shareCode: string | null;
}

export interface WatchlistItemResponse {
  item: MediaItemResponse;
  seen: boolean;
}

export interface WatchlistUpdatePreviewResponse {
  added: MediaItemResponse[];
  removed: MediaItemResponse[];
}

export interface WatchlistUpdateApplyOptions {
  excludeAddedIds: string[];
  keepRemovedIds: string[];
  markSeenIds: string[];
}

export interface FilterMetadata {
  genres: string[];
  mediaTypes: string[];
  minYear: number;
}

export type SortBy = 'DEFAULT' | 'RATING' | 'YEAR' | 'RUNTIME' | 'VOTES' | 'TITLE' | 'UNSEEN';
export type SortDirection = 'ASC' | 'DESC';

export interface FilterState {
  minRating: string;
  maxRating: string;
  minYear: string;
  maxYear: string;
  minRuntime: string;
  maxRuntime: string;
  minVotes: string;
  maxVotes: string;
  genres: string[];
  mediaTypes: string[];
  keywords: string;
  excludeSeen: boolean;
  query: string;
}
