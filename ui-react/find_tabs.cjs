const fs = require('fs');
const path = require('path');

const jsPath = path.join(__dirname, 'assets/public/assets/index-CHYrWmZb.js');
const jsCode = fs.readFileSync(jsPath, 'utf8');

// Find the main app switch / router
// In React apps built without react-router (using state-based tabs):
// k === 'home' ? ... : k === 'search' ? ... : k === 'playlists' ? ...

const tabRegex = /([a-zA-Z0-9_$]+)===`([a-zA-Z0-9_-]+)`\s*\?\s*\([0-9]+,\s*[a-zA-Z0-9_$]+\.(?:jsx|jsxs)\)\(`([a-zA-Z0-9_-]+)`/g;
let m;
console.log('Main Tab ternary switches:');
while ((m = tabRegex.exec(jsCode)) !== null) {
  console.log(`- Var '${m[1]}' === '${m[2]}' renders <${m[3]}> at offset ${m.index}`);
}

// Let's search for sections with className animate-mobile-page or similar
const sectionRegex = /className:`[^`]*animate-mobile-page[^`]*`/g;
const sections = [];
while ((m = sectionRegex.exec(jsCode)) !== null) {
  const start = Math.max(0, m.index - 100);
  const end = Math.min(jsCode.length, m.index + 250);
  sections.push(jsCode.substring(start, end));
}
console.log(`Found ${sections.length} mobile page sections!`);
fs.writeFileSync(path.join(__dirname, 'mobile_pages_extracted.json'), JSON.stringify(sections, null, 2));

