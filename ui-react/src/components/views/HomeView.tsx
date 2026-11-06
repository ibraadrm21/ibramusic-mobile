import React from 'react';
import { useAudio } from '../../context/AudioContext';
import { Track, Playlist } from '../../types';

interface HomeViewProps {
  playlists: Playlist[];
  onSelectPlaylist: (playlist: Playlist) => void;
  onOpenArtist: (artist: { id: string; name: string }) => void;
}

export const HomeView: React.FC<HomeViewProps> = ({ playlists, onSelectPlaylist, onOpenArtist }) => {
  const { playTrack, currentTrack, isPlaying } = useAudio();

  // Mock recommendations based on IbraStream catalog
  const quickPicks: Track[] = [
    {
      id: 'pick-1',
      title: 'Blinding Lights',
      artist: 'The Weeknd',
      artistId: 'weeknd',
      album: 'After Hours',
      duration: 200,
      thumbnail: 'https://images.unsplash.com/photo-1614613535308-eb5fbd3d2c17?w=300&q=80',
    },
    {
      id: 'pick-2',
      title: 'Starboy',
      artist: 'The Weeknd, Daft Punk',
      artistId: 'weeknd',
      album: 'Starboy',
      duration: 230,
      thumbnail: 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=300&q=80',
    },
    {
      id: 'pick-3',
      title: 'As It Was',
      artist: 'Harry Styles',
      artistId: 'harry',
      album: "Harry's House",
      duration: 167,
      thumbnail: 'https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&q=80',
    },
    {
      id: 'pick-4',
      title: 'Flowers',
      artist: 'Miley Cyrus',
      artistId: 'miley',
      album: 'Endless Summer Vacation',
      duration: 200,
      thumbnail: 'https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=300&q=80',
    },
  ];

  return (
    <div className="flex flex-col gap-8 pb-32 animate-[fadeIn_0.3s_ease]">
      {/* Hero Welcome Banner */}
      <section className="relative rounded-3xl p-8 bg-gradient-to-r from-brand-accent/20 via-white/5 to-transparent border border-white/10 overflow-hidden shadow-2xl">
        <div className="relative z-10 flex flex-col gap-2 max-w-xl">
          <span className="text-xs uppercase font-bold tracking-widest text-brand-accent">
            Welcome to IbraStream
          </span>
          <h1 className="text-3xl sm:text-4xl font-extrabold text-white tracking-tight">
            Let The Music Take You Away
          </h1>
          <p className="text-sm text-gray-300 mt-1">
            Studio-grade lossless streaming, instant YouTube and Deezer playback, and seamless crossfade audio.
          </p>
        </div>
      </section>

      {/* Quick Picks / Jump Back In */}
      <section className="flex flex-col gap-4">
        <h2 className="text-xl font-bold text-white tracking-tight">Quick Picks</h2>
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {quickPicks.map(track => {
            const isCurrent = currentTrack?.id === track.id;
            return (
              <div
                key={track.id}
                onClick={() => playTrack(track, quickPicks)}
                className={`flex items-center gap-3.5 p-3 rounded-2xl border transition-all cursor-pointer group ${
                  isCurrent
                    ? 'bg-white/15 border-white/20'
                    : 'bg-white/5 hover:bg-white/10 border-white/5 hover:border-white/10'
                }`}
              >
                <div className="relative w-12 h-12 rounded-xl overflow-hidden shrink-0 shadow-md">
                  <img src={track.thumbnail} alt={track.title} className="w-full h-full object-cover" />
                  {isCurrent && isPlaying && (
                    <div className="absolute inset-0 bg-black/40 flex items-center justify-center">
                      <div className="w-2.5 h-2.5 bg-brand-accent rounded-full animate-pulse" />
                    </div>
                  )}
                </div>

                <div className="flex flex-col min-w-0 flex-1">
                  <span className="text-sm font-semibold text-white truncate group-hover:text-brand-accent transition-colors">
                    {track.title}
                  </span>
                  <span className="text-xs text-gray-400 truncate mt-0.5">{track.artist}</span>
                </div>
              </div>
            );
          })}
        </div>
      </section>

      {/* Recommended Playlists */}
      {playlists.length > 0 && (
        <section className="flex flex-col gap-4">
          <h2 className="text-xl font-bold text-white tracking-tight">Featured Playlists</h2>
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-5 gap-4">
            {playlists.map(pl => (
              <div
                key={pl.id}
                onClick={() => onSelectPlaylist(pl)}
                className="flex flex-col gap-3 p-3.5 rounded-2xl bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/10 cursor-pointer group transition-all"
              >
                <div className="relative aspect-square rounded-xl overflow-hidden shadow-lg bg-[#181818]">
                  <img
                    src={pl.coverUrl || 'https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=400&q=80'}
                    alt={pl.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                </div>
                <div className="flex flex-col min-w-0">
                  <span className="text-sm font-semibold text-white truncate">{pl.name}</span>
                  <span className="text-xs text-gray-400 truncate mt-0.5">{pl.tracks.length} songs</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};
