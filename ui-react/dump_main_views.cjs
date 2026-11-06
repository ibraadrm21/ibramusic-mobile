const fs = require('fs');
const path = require('path');

const jsPath = path.join(__dirname, 'assets/public/assets/index-CHYrWmZb.js');
const jsCode = fs.readFileSync(jsPath, 'utf8');

const mainViewsBlock = jsCode.substring(1470000, 1590000);
fs.writeFileSync(path.join(__dirname, 'main_views_block.js'), mainViewsBlock);
console.log('Saved main_views_block.js, size:', mainViewsBlock.length);

