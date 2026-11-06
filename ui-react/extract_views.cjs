const fs = require('fs');
const path = require('path');

const jsPath = path.join(__dirname, 'assets/public/assets/index-CHYrWmZb.js');
const jsCode = fs.readFileSync(jsPath, 'utf8');

// Find all React hooks and component names
// React.createElement or JSX runtime calls: (0, Q.jsx) or (0, Q.jsxs)
// In minified code, look for section headers, titles, aria-labels, and data attributes

const titles = [];
const titleRegex = /(?:title|placeholder|aria-label|alt|children):[`"']([^`"']{3,50})[`"']/g;
let m;
while ((m = titleRegex.exec(jsCode)) !== null) {
  if (!m[1].includes('<') && !m[1].includes('{')) {
    titles.push(m[1]);
  }
}

const uniqueTitles = Array.from(new Set(titles));
fs.writeFileSync(path.join(__dirname, 'extracted_labels.json'), JSON.stringify(uniqueTitles, null, 2));

// Find API endpoints and network requests
const apiUrls = [];
const urlRegex = /(https?:\/\/[a-zA-Z0-9_.-]+(?:\/[a-zA-Z0-9_.\-\/\?=&%+]*)?)/g;
while ((m = urlRegex.exec(jsCode)) !== null) {
  apiUrls.push(m[1]);
}
fs.writeFileSync(path.join(__dirname, 'api_endpoints.json'), JSON.stringify(Array.from(new Set(apiUrls)), null, 2));

// Find local storage keys
const storageKeys = [];
const storageRegex = /localStorage\.(?:getItem|setItem|removeItem)\([`"']([a-zA-Z0-9_.-]+)[`"']/g;
while ((m = storageRegex.exec(jsCode)) !== null) {
  storageKeys.push(m[1]);
}
fs.writeFileSync(path.join(__dirname, 'storage_keys.json'), JSON.stringify(Array.from(new Set(storageKeys)), null, 2));

console.log('Extracted labels count:', uniqueTitles.length);
console.log('Extracted API URLs count:', Array.from(new Set(apiUrls)).length);
console.log('Extracted Storage keys count:', Array.from(new Set(storageKeys)).length);
console.log('Sample storage keys:', Array.from(new Set(storageKeys)));

