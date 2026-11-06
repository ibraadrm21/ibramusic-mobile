import React from 'react';
import { UserStats, LeaderboardUser } from '../../types';

interface StatsViewProps {
  stats?: UserStats;
  leaderboard?: LeaderboardUser[];
}

export const StatsView: React.FC<StatsViewProps> = ({ stats, leaderboard = [] }) => {
  const defaultStats: UserStats = stats || {
    totalSecondsListened: 184500, // ~3075 minutes
    totalTracksPlayed: 852,
    topArtists: [
      { id: '1', name: 'The Weeknd', count: 184, minutes: 640 },
      { id: '2', name: 'Drake', count: 122, minutes: 420 },
      { id: '3', name: 'Bad Bunny', count: 98, minutes: 310 },
      { id: '4', name: 'Travis Scott', count: 74, minutes: 260 },
      { id: '5', name: 'Daft Punk', count: 62, minutes: 220 },
    ],
    topTracks: [
      { id: 't1', title: 'Blinding Lights', artist: 'The Weeknd', count: 48 },
      { id: 't2', title: 'Starboy', artist: 'The Weeknd', count: 36 },
      { id: 't3', title: 'FE!N', artist: 'Travis Scott, Playboi Carti', count: 28 },
      { id: 't4', title: 'As It Was', artist: 'Harry Styles', count: 24 },
    ],
    personalityType: 'The Night Owl Audiophile',
    monthlyRank: 1,
  };

  const totalMinutes = Math.floor(defaultStats.totalSecondsListened / 60);

  return (
    <div className="flex flex-col gap-8 pb-32 animate-[fadeIn_0.3s_ease]">
      {/* Wrapped Header */}
      <section className="relative rounded-3xl p-8 bg-gradient-to-br from-purple-900/40 via-brand-accent/10 to-transparent border border-white/10 overflow-hidden shadow-2xl">
        <div className="relative z-10 flex flex-col gap-2 max-w-xl">
          <span className="text-xs uppercase font-extrabold tracking-widest text-purple-400">
            ibramusic wrapped 2026
          </span>
          <h1 className="text-3xl sm:text-5xl font-black text-white tracking-tight">
            Your Journey In Sound
          </h1>
          <p className="text-sm text-gray-300 mt-2">
            A celebration of every track, artist, and late-night listening session that defined your year.
          </p>
        </div>
      </section>

      {/* Primary KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {/* Total Time */}
        <div className="p-6 rounded-3xl bg-white/5 border border-white/10 flex flex-col gap-2">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Total Listening Time</span>
          <span className="text-4xl font-extrabold text-white tracking-tight tabular-nums">
            {totalMinutes.toLocaleString()}
          </span>
          <span className="text-xs text-gray-400">minutes listened</span>
        </div>

        {/* Total Songs */}
        <div className="p-6 rounded-3xl bg-white/5 border border-white/10 flex flex-col gap-2">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Total Tracks Played</span>
          <span className="text-4xl font-extrabold text-white tracking-tight tabular-nums">
            {defaultStats.totalTracksPlayed.toLocaleString()}
          </span>
          <span className="text-xs text-gray-400">songs completed</span>
        </div>

        {/* Personality */}
        <div className="p-6 rounded-3xl bg-white/5 border border-white/10 flex flex-col gap-2">
          <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Musical Personality</span>
          <span className="text-2xl font-bold text-white tracking-tight leading-snug">
            {defaultStats.personalityType}
          </span>
          <span className="text-xs text-gray-400">Sonic Profile</span>
        </div>
      </div>

      {/* Top Artists & Top Tracks Breakdown */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Top Artists */}
        <div className="p-6 rounded-3xl bg-white/5 border border-white/10 flex flex-col gap-4">
          <h2 className="text-lg font-bold text-white flex items-center justify-between">
            <span>Your Icons • Top Artists</span>
            <span className="text-xs text-gray-400 font-normal">Ranked by plays</span>
          </h2>
          <div className="flex flex-col gap-3">
            {defaultStats.topArtists.map((artist, idx) => (
              <div key={artist.id} className="flex items-center justify-between p-3 rounded-2xl bg-white/5">
                <div className="flex items-center gap-3">
                  <span className="text-sm font-bold text-brand-accent w-5 text-center">{idx + 1}</span>
                  <span className="text-sm font-semibold text-white">{artist.name}</span>
                </div>
                <div className="flex items-center gap-4 text-xs text-gray-400 tabular-nums">
                  <span>{artist.minutes} mins</span>
                  <span className="font-semibold text-white">{artist.count} plays</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Top Tracks */}
        <div className="p-6 rounded-3xl bg-white/5 border border-white/10 flex flex-col gap-4">
          <h2 className="text-lg font-bold text-white flex items-center justify-between">
            <span>Your Soundtrack • Top Tracks</span>
            <span className="text-xs text-gray-400 font-normal">Ranked by plays</span>
          </h2>
          <div className="flex flex-col gap-3">
            {defaultStats.topTracks.map((track, idx) => (
              <div key={track.id} className="flex items-center justify-between p-3 rounded-2xl bg-white/5">
                <div className="flex items-center gap-3 min-w-0">
                  <span className="text-sm font-bold text-brand-accent w-5 text-center">{idx + 1}</span>
                  <div className="flex flex-col min-w-0">
                    <span className="text-sm font-semibold text-white truncate">{track.title}</span>
                    <span className="text-xs text-gray-400 truncate">{track.artist}</span>
                  </div>
                </div>
                <span className="text-xs font-semibold text-white tabular-nums shrink-0">{track.count} plays</span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Community Monthly Leaderboard: "Desempleado del Mes" */}
      <section className="p-6 rounded-3xl bg-white/5 border border-white/10 flex flex-col gap-4">
        <div className="flex items-center justify-between flex-wrap gap-2">
          <div className="flex items-center gap-2">
            <span className="text-2xl">👑</span>
            <h2 className="text-xl font-bold text-white tracking-tight">Desempleado del Mes</h2>
          </div>
          <span className="text-xs text-gray-400">Most active community listeners this month</span>
        </div>

        <div className="flex flex-col gap-2 mt-2">
          <div className="flex items-center justify-between p-4 rounded-2xl bg-gradient-to-r from-amber-500/20 via-white/5 to-transparent border border-amber-500/30">
            <div className="flex items-center gap-3">
              <span className="text-lg font-black text-amber-400">#1</span>
              <div className="w-10 h-10 rounded-full bg-amber-400/20 border border-amber-400/40 flex items-center justify-center font-bold text-amber-300">
                👑
              </div>
              <div className="flex flex-col">
                <span className="text-sm font-bold text-white">Leader 👑</span>
                <span className="text-xs text-gray-400">3,420 mins • 980 tracks</span>
              </div>
            </div>
            <span className="text-xs font-bold text-amber-400 bg-amber-400/10 px-3 py-1 rounded-full border border-amber-400/30">
              Desempleado #1
            </span>
          </div>
        </div>
      </section>
    </div>
  );
};
