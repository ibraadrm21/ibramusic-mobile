const fs = require('fs');
const path = require('path');

const code = fs.readFileSync(path.join(__dirname, 'main_views_block.js'), 'utf8');

const regex = /([a-zA-Z0-9_$]+)===`([^`]+)`/g;
const tabs = [];
let m;
while ((m = regex.exec(code)) !== null) {
  tabs.push({ varName: m[1], tab: m[2], index: m.index });
}

console.log('All tab comparisons:');
tabs.forEach(t => console.log(`- ${t.varName} === '${t.tab}' at offset ${t.index}`));

// Find section titles or button labels inside this block
const textMatches = code.match(/children:(?:\[`|`)([^`]{3,40})/g) || [];
console.log('Sample text labels in main view:', textMatches.slice(0, 30));

