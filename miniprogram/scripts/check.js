/**
 * 小程序静态自检脚本（纯 node，无依赖）
 * 校验：
 *  1) app.json pages 与 tabBar 图标文件全部存在
 *  2) 每个页面 .js/.wxml/.json/.wxss 四件套齐全
 *  3) 所有 json（app/page/component/project/sitemap）可解析
 *  4) wxml 引用的自定义组件已在对应页面 json 的 usingComponents 注册且文件存在
 *  5) 所有 /mp/** 接口字面量与 docs/api-contract.md 第 2 节白名单一致
 * 用法：node scripts/check.js
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.join(__dirname, '..');
let failures = 0;

function ok(msg) { console.log('PASS  ' + msg); }
function fail(msg) { console.log('FAIL  ' + msg); failures++; }

/* ---------- 1. app.json 解析 + pages/tabBar 存在性 ---------- */
const appJsonPath = path.join(ROOT, 'app.json');
let appJson;
try {
  appJson = JSON.parse(fs.readFileSync(appJsonPath, 'utf8'));
  ok('app.json 可解析');
} catch (e) {
  fail('app.json 解析失败: ' + e.message);
  process.exit(1);
}

const BUILTIN_TAGS = new Set([
  'view', 'text', 'image', 'block', 'scroll-view', 'swiper', 'swiper-item',
  'button', 'input', 'textarea', 'picker', 'picker-view', 'picker-view-column',
  'checkbox', 'checkbox-group', 'radio', 'radio-group', 'slider', 'switch',
  'form', 'label', 'navigator', 'progress', 'rich-text', 'canvas', 'map',
  'video', 'audio', 'camera', 'live-player', 'live-pusher', 'web-view',
  'cover-view', 'cover-image', 'movable-area', 'movable-view', 'slot',
  'page-meta', 'navigation-bar', 'match-media', 'functional-page-navigator'
]);

const pageJsonCache = {};

function checkPage(pagePath) {
  const base = path.join(ROOT, pagePath);
  let allExist = true;
  for (const ext of ['.js', '.wxml', '.json', '.wxss']) {
    if (fs.existsSync(base + ext)) continue;
    fail(`页面四件套缺失: ${pagePath}${ext}`);
    allExist = false;
  }
  if (!allExist) return;

  ok(`页面四件套齐全: ${pagePath}`);

  // 页面 json 可解析 + usingComponents 校验
  let pj;
  try {
    pj = JSON.parse(fs.readFileSync(base + '.json', 'utf8'));
    pageJsonCache[pagePath] = pj;
  } catch (e) {
    fail(`json 不可解析: ${pagePath}.json → ${e.message}`);
    return;
  }

  const using = pj.usingComponents || {};
  for (const [tag, compPath] of Object.entries(using)) {
    const resolved = compPath.startsWith('/')
      ? path.join(ROOT, compPath)
      : path.join(ROOT, path.dirname(pagePath), compPath);
    for (const ext of ['.js', '.wxml', '.json', '.wxss']) {
      if (!fs.existsSync(resolved + ext)) {
        fail(`组件文件缺失: ${tag} → ${compPath}${ext}`);
      }
    }
  }

  // wxml 中自定义组件标签是否已注册
  const wxml = fs.readFileSync(base + '.wxml', 'utf8');
  const tagRe = /<([a-zA-Z][a-zA-Z0-9-]*)[\s/>]/g;
  const used = new Set();
  let m;
  while ((m = tagRe.exec(wxml)) !== null) used.add(m[1]);
  for (const tag of used) {
    if (BUILTIN_TAGS.has(tag)) continue;
    if (!using[tag]) fail(`wxml 引用未注册组件: ${pagePath}.wxml → <${tag}>`);
  }
}

for (const p of appJson.pages || []) checkPage(p);

/* ---------- 2. tabBar 图标 ---------- */
const tabList = (appJson.tabBar && appJson.tabBar.list) || [];
for (const item of tabList) {
  if ((appJson.pages || []).indexOf(item.pagePath) === -1) {
    fail(`tabBar pagePath 未注册为页面: ${item.pagePath}`);
  }
  for (const key of ['iconPath', 'selectedIconPath']) {
    if (!item[key]) continue;
    const iconFile = path.join(ROOT, item[key]);
    if (!fs.existsSync(iconFile)) {
      fail(`tabBar 图标不存在: ${item[key]}`);
      continue;
    }
    const buf = fs.readFileSync(iconFile);
    if (!(buf[0] === 0x89 && buf[1] === 0x50 && buf[2] === 0x4e && buf[3] === 0x47)) {
      fail(`tabBar 图标不是有效 PNG: ${item[key]}`);
    } else {
      ok(`tabBar 图标有效: ${item[key]}`);
    }
  }
}
ok('tabBar 配置与图标检查完成');

/* ---------- 3. 组件与其他 json 可解析 ---------- */
function walkJson(dir) {
  for (const name of fs.readdirSync(dir)) {
    const full = path.join(dir, name);
    const st = fs.statSync(full);
    if (st.isDirectory()) { walkJson(full); continue; }
    if (!name.endsWith('.json')) continue;
    try {
      JSON.parse(fs.readFileSync(full, 'utf8'));
    } catch (e) {
      fail(`json 不可解析: ${path.relative(ROOT, full)} → ${e.message}`);
    }
  }
}
walkJson(ROOT);
ok('全部 json 可解析性检查完成');

/* ---------- 4. 接口路径白名单核对（契约第 2 节 /api/mp/**） ---------- */
const WHITELIST = [
  /^\/mp\/auth\/login$/,
  /^\/mp\/user\/me$/, /^\/mp\/user\/me\/contributions$/,
  /^\/mp\/buildings$/, /^\/mp\/buildings\/[^/]+$/, /^\/mp\/buildings\/[^/]+\/guide$/,
  /^\/mp\/classrooms\/search$/, /^\/mp\/classrooms\/recent$/,
  /^\/mp\/classrooms\/latest-updated$/, /^\/mp\/classrooms\/apply-new$/,
  /^\/mp\/classrooms\/[^/]+$/,
  /^\/mp\/classrooms\/[^/]+\/(images|socket-marks|reviews|status|status\/latest|equipment-reports)$/,
  /^\/mp\/socket-marks\/[^/]+\/feedback$/,
  /^\/mp\/qr\/[^/]+$/,
  /^\/mp\/timetable\/semesters$/, /^\/mp\/timetable\/periods$/,
  /^\/mp\/timetable\/courses$/, /^\/mp\/timetable\/courses\/[^/]+$/,
  /^\/mp\/timetable\/today$/, /^\/mp\/timetable\/weeks-current$/,
  /^\/mp\/timetable\/ocr$/, /^\/mp\/timetable\/ocr\/[^/]+$/,
  /^\/mp\/timetable\/ocr\/[^/]+\/confirm$/,
  /^\/mp\/timetable\/remind-config$/, /^\/mp\/timetable\/remind\/subscribe-record$/,
  /^\/mp\/lost-items$/, /^\/mp\/lost-items\/match-feeds$/,
  /^\/mp\/lost-items\/[^/]+$/,
  /^\/mp\/lost-items\/[^/]+\/(status|claims|matches)$/,
  /^\/mp\/claims\/[^/]+\/handle$/,
  /^\/mp\/favorites$/,
  /^\/mp\/messages$/, /^\/mp\/messages\/read-all$/, /^\/mp\/messages\/unread-count$/,
  /^\/mp\/messages\/[^/]+\/read$/,
  /^\/mp\/reports$/, /^\/mp\/announcements$/,
  /^\/mp\/upload\/image(-base64)?$/
];

const urlRe = /(['"`])(\/mp\/[^'"`]+)\1/g;
const found = new Set();
function walkJs(dir) {
  for (const name of fs.readdirSync(dir)) {
    if (name === 'node_modules' || name === 'scripts') continue;
    const full = path.join(dir, name);
    const st = fs.statSync(full);
    if (st.isDirectory()) { walkJs(full); continue; }
    if (!name.endsWith('.js')) continue;
    const src = fs.readFileSync(full, 'utf8');
    let m;
    while ((m = urlRe.exec(src)) !== null) {
      // 模板占位统一为 {id} 再匹配
      const norm = m[2].replace(/\$\{[^}]+\}/g, '{id}');
      found.add(norm);
    }
  }
}
walkJs(ROOT);

for (const url of Array.from(found).sort()) {
  const matched = WHITELIST.some(re => re.test(url));
  if (matched) ok(`接口符合契约: ${url}`);
  else fail(`接口不在契约白名单: ${url}`);
}

/* ---------- 汇总 ---------- */
console.log('------------------------------------------');
if (failures === 0) {
  console.log(`全部检查通过：${appJson.pages.length} 个页面 + ${tabList.length} 个 tab + ${found.size} 个接口`);
} else {
  console.log(`存在 ${failures} 项失败，请修复后重试`);
  process.exitCode = 1;
}
