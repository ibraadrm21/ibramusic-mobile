const fs = require('fs');
const path = require('path');

const jsPath = path.join(__dirname, 'assets/public/assets/index-CHYrWmZb.js');
const jsCode = fs.readFileSync(jsPath, 'utf8');

// Let's search for component render signatures and strings to map out every single UI module.
// In React bundled with Vite, components render elements using Q.jsx and Q.jsxs

const keywords = [
  'activeTab', 'currentTrack', 'isPlaying', 'volume', 'isMuted', 'shuffle', 'repeat',
  'lyrics', 'equalizer', 'queue', 'playlists', 'favorites', 'history', 'stats', 'wrapped',
  'settings', 'spotify', 'ambientGlow', 'seamlessTransitions', 'desempleado'
];

const results = {};
for (const kw of keywords) {
  let count = 0;
  let idx = 0;
  const snippets = [];
  while ((idx = jsCode.indexOf(kw, idx)) !== -1) {
    count++;
    if (snippets.length < 3) {
      const start = Math.max(0, idx - 150);
      const end = Math.min(jsCode.length, idx + 200);
      snippets.push(jsCode.substring(start, end).replace(/\n/g, ' '));
    }
    idx += kw.length;
  }
  results[kw] = { count, snippets };
}

fs.writeFileSync(path.join(__dirname, 'component_snippets.json'), JSON.stringify(results, null, 2));
console.log('Keyword match summary:');
for (const [k, v] of Object.entries(results)) {
  console.log(`- ${k}: ${v.count} occurrences`);
}

