const fs = require('fs');
const path = require('path');

const jsPath = path.join(__dirname, 'assets/public/assets/index-CHYrWmZb.js');
const cssPath = path.join(__dirname, 'assets/public/assets/index-YQ3bLvmv.css');

const jsCode = fs.readFileSync(jsPath, 'utf8');
const cssCode = fs.readFileSync(cssPath, 'utf8');

console.log('JS Size:', jsCode.length);
console.log('CSS Size:', cssCode.length);

// Analyze CSS classes and color variables
const colorVars = cssCode.match(/--[a-zA-Z0-9_-]+:[^;]+;/g) || [];
console.log('Found CSS variables:', colorVars.slice(0, 20));

// Find components / function signatures
const funcs = jsCode.match(/function\s+([A-Za-z0-9_$]+)\s*\(/g) || [];
console.log('Total functions:', funcs.length);

// Search for route / view states with quotes or backticks
const viewMatches = jsCode.match(/[a-zA-Z0-9_$]+===([`"'])([a-zA-Z0-9_-]+)\1/g) || [];
const uniqueViews = new Set(viewMatches.map(m => m.split('===')[1].replace(/[`"']/g, '')));
console.log('Unique view/route states:', Array.from(uniqueViews));

// Search for navigation items
const navMatches = jsCode.match(/[`"'](home|search|library|explore|playlists?|artists?|albums?|favorites|liked|history|settings|lyrics|queue|stats)[`"']/gi) || [];
console.log('Common nav terms:', Array.from(new Set(navMatches.map(m => m.replace(/[`"']/g, '').toLowerCase()))));

fs.writeFileSync(path.join(__dirname, 'css_vars.json'), JSON.stringify(colorVars, null, 2));
fs.writeFileSync(path.join(__dirname, 'views_found.json'), JSON.stringify(Array.from(uniqueViews), null, 2));
