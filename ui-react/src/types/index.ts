export interface Artist {
  id: string;
  name: string;
  thumbnail?: string;
  monthlyListeners?: number;
  bio?: string;
}

export interface Album {
  id: string;
  title: string;
  artist: string;
  artistId?: string;
  thumbnail?: string;
  releaseYear?: number;
  totalTracks?: number;
  tracks?: Track[];
}

export interface Track {
  id: string;
  title: string;
  artist: string;
  artistId?: string;
  artists?: { id: string; name: string }[];
  album?: string;
  albumId?: string;
  albumName?: string;
  thumbnail: string;
  duration: number; // in seconds
  durationMs?: number;
  audioUrl?: string;
  dateAdded?: string;
  isExplicit?: boolean;
  isLiked?: boolean;
  isDownloaded?: boolean;
  localPath?: string;
  playCount?: number;
}

export interface Playlist {
  id: string;
  name: string;
  description?: string;
  coverUrl?: string;
  tracks: Track[];
  isPublic?: boolean;
  createdAt?: string;
  updatedAt?: string;
  isPinned?: boolean;
  isHidden?: boolean;
}

export type RepeatMode = 'none' | 'all' | 'one';

export interface PlaybackState {
  currentTrack: Track | null;
  isPlaying: boolean;
  isLoading: boolean;
  currentTime: number;
  duration: number;
  volume: number;
  isMuted: boolean;
  isShuffle: boolean;
  isRepeat: RepeatMode;
  seamlessTransitions: boolean;
  seamlessDuration: number;
  ambientGlow: boolean;
  playingPlaylistId: string | null;
}

export interface QueueState {
  userQueue: Track[];
  playlistQueue: Track[];
  originalPlaylistQueue: Track[];
  currentIndex: number;
  history: Track[];
}

export interface LyricLine {
  time: number; // seconds
  text: string;
  translation?: string;
}

export interface UserStats {
  totalSecondsListened: number;
  totalTracksPlayed: number;
  topArtists: { id: string; name: string; count: number; minutes: number }[];
  topTracks: { id: string; title: string; artist: string; count: number }[];
  monthlyRank?: number;
  personalityType?: string;
}

export interface LeaderboardUser {
  id: string;
  username: string;
  avatarUrl?: string;
  totalMinutes: number;
  tracksPlayed: number;
  rank: number;
}

export type ThemeMode = 'noir' | 'dark' | 'bright';

export interface ThemeSettings {
  mode: ThemeMode;
  accentColor: string;
  backgroundColor: string;
  backgroundImage?: string;
  borderRadius: 'rounded' | 'sharp' | 'pill';
  ambientGlow: boolean;
}

export interface EqPreset {
  name: string;
  gains: number[]; // e.g. 5 or 10 bands (-12dB to +12dB)
}

