const fs = require('fs');
const path = require('path');

const jsPath = path.join(__dirname, 'assets/public/assets/index-CHYrWmZb.js');
const jsCode = fs.readFileSync(jsPath, 'utf8');

// Look for player elements: play/pause, seek, mini player, audio element
const playerSnippets = [];
const terms = ['Seamless Transitions', 'Crossfade suave', 'isShuffle', 'isRepeat', 'miniPlayer', 'Queue'];
for (const term of terms) {
  let idx = 0;
  while ((idx = jsCode.indexOf(term, idx)) !== -1) {
    const start = Math.max(0, idx - 150);
    const end = Math.min(jsCode.length, idx + 200);
    playerSnippets.push({ term, snippet: jsCode.substring(start, end).replace(/\n/g, ' ') });
    idx += term.length;
  }
}

console.log(`Found ${playerSnippets.length} player snippets.`);
fs.writeFileSync(path.join(__dirname, 'player_snippets.json'), JSON.stringify(playerSnippets, null, 2));

