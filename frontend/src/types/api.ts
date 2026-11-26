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
  director: string;
  releaseYear: number | null;
  rating: number | null;
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

export interface WatchlistResponse {
  id: string;
  name: string;
  itemCount: number;
  updatedAt: string;
}

export interface WatchlistItemResponse {
  item: MediaItemResponse;
  seen: boolean;
}

export interface FilterMetadata {
  genres: string[];
  mediaTypes: string[];
  minYear: number;
}

export interface FilterState {
  minRating: string;
  maxRating: string;
  minYear: string;
  maxYear: string;
  genres: string[];
  mediaTypes: string[];
  keywords: string;
  excludeSeen: boolean;
}
