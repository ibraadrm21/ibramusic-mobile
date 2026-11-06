/**
 * IbraStream Design System & Visual Tokens
 * Extracted from production assets (index-YQ3bLvmv.css)
 */

export const colors = {
  // Brand Core
  brand: {
    accent: '#e8e8e8',
    darkBg: '#0f0f0f',
    emerald: '#00bb7f',
    cyan: '#00b7d7',
    red: '#fb2c36',
    purple: '#ac4bff',
    yellow: '#edb200',
  },

  // Surfaces & Backgrounds
  surface: {
    noir: '#000000',
    dark: '#0f0f0f',
    card: '#181818',
    cardHover: '#242424',
    elevated: 'rgba(255, 255, 255, 0.05)',
    elevatedHover: 'rgba(255, 255, 255, 0.10)',
    glass: 'rgba(15, 15, 15, 0.85)',
    glassBorder: 'rgba(255, 255, 255, 0.10)',
  },

  // Text
  text: {
    primary: '#ffffff',
    secondary: '#a3a3a3',
    tertiary: '#737373',
    muted: '#525252',
  },
};

export const glassmorphism = {
  panel: 'backdrop-blur-xl bg-black/60 border border-white/10 shadow-2xl',
  floatingBar: 'backdrop-blur-2xl bg-[#0f0f0f]/90 border border-white/10 shadow-[0_8px_32px_rgba(0,0,0,0.5)]',
  card: 'bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/10 transition-all duration-200',
  pill: 'bg-white/10 hover:bg-white/15 border border-white/15 text-white transition-all',
  activePill: 'bg-white text-black font-semibold shadow-md',
};

export const animations = {
  fadeIn: 'animate-[fadeIn_0.3s_ease]',
  slideUp: 'animate-[slideUp_0.2s_cubic-bezier(0.16,1,0.3,1)]',
  scaleUp: 'animate-[scaleUp_0.4s_ease-out]',
  spinSlow: 'animate-spin-slow', // 12s vinyl rotation
  pulseDot: 'animate-pulse',
};

