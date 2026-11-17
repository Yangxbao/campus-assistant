/**
 * tabBar 图标生成脚本（纯 node，无任何依赖）
 * 手写 PNG 编码（IHDR/IDAT/IEND + zlib.deflateSync），81x81 RGBA 纯色形状图标
 * 用法：node scripts/gen-icons.js
 */
const zlib = require('zlib');
const fs = require('fs');
const path = require('path');

/* ---------------- PNG 编码 ---------------- */
let CRC_TABLE = null;
function crc32(buf) {
  if (!CRC_TABLE) {
    CRC_TABLE = [];
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      CRC_TABLE[n] = c >>> 0;
    }
  }
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const typeBuf = Buffer.from(type, 'ascii');
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(Buffer.concat([typeBuf, data])), 0);
  return Buffer.concat([len, typeBuf, data, crc]);
}

function encodePNG(width, height, rgba) {
  const sig = Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8;  // bit depth
  ihdr[9] = 6;  // color type RGBA
  ihdr[10] = 0; ihdr[11] = 0; ihdr[12] = 0;
  const stride = width * 4 + 1;
  const raw = Buffer.alloc(stride * height);
  for (let y = 0; y < height; y++) {
    raw[y * stride] = 0; // filter: none
    rgba.copy(raw, y * stride + 1, y * width * 4, (y + 1) * width * 4);
  }
  const idat = zlib.deflateSync(raw, { level: 9 });
  return Buffer.concat([sig, chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', Buffer.alloc(0))]);
}

/* ---------------- 极简画布 ---------------- */
const TRANSPARENT = [0, 0, 0, 0];

class Canvas {
  constructor(w, h) {
    this.w = w; this.h = h;
    this.data = Buffer.alloc(w * h * 4);
  }
  set(x, y, c) {
    x = Math.round(x); y = Math.round(y);
    if (x < 0 || y < 0 || x >= this.w || y >= this.h) return;
    const i = (y * this.w + x) * 4;
    this.data[i] = c[0]; this.data[i + 1] = c[1]; this.data[i + 2] = c[2]; this.data[i + 3] = c[3];
  }
  fillRect(x, y, w, h, c) {
    for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) this.set(xx, yy, c);
  }
  fillRoundRect(x, y, w, h, r, c) {
    for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) {
      const dx = Math.max(x + r - xx, xx - (x + w - 1 - r), 0);
      const dy = Math.max(y + r - yy, yy - (y + h - 1 - r), 0);
      if (dx * dx + dy * dy <= r * r) this.set(xx, yy, c);
    }
  }
  fillCircle(cx, cy, r, c) {
    for (let yy = cy - r; yy <= cy + r; yy++) for (let xx = cx - r; xx <= cx + r; xx++)
      if ((xx - cx) * (xx - cx) + (yy - cy) * (yy - cy) <= r * r) this.set(xx, yy, c);
  }
  ring(cx, cy, rOuter, rInner, c) {
    for (let yy = cy - rOuter; yy <= cy + rOuter; yy++) for (let xx = cx - rOuter; xx <= cx + rOuter; xx++) {
      const d = (xx - cx) * (xx - cx) + (yy - cy) * (yy - cy);
      if (d <= rOuter * rOuter && d >= rInner * rInner) this.set(xx, yy, c);
    }
  }
  fillPolygon(pts, c) {
    const ys = pts.map(p => p[1]);
    const minY = Math.floor(Math.min(...ys)), maxY = Math.ceil(Math.max(...ys));
    for (let y = minY; y <= maxY; y++) {
      const xs = [];
      for (let i = 0; i < pts.length; i++) {
        const [x1, y1] = pts[i], [x2, y2] = pts[(i + 1) % pts.length];
        if ((y1 <= y && y2 > y) || (y2 <= y && y1 > y)) xs.push(x1 + ((y - y1) / (y2 - y1)) * (x2 - x1));
      }
      xs.sort((a, b) => a - b);
      for (let i = 0; i + 1 < xs.length; i += 2)
        for (let x = Math.round(xs[i]); x <= Math.round(xs[i + 1]); x++) this.set(x, y, c);
    }
  }
  eraseRect(x, y, w, h) { this.fillRect(x, y, w, h, TRANSPARENT); }
  eraseRoundRect(x, y, w, h, r) { this.fillRoundRect(x, y, w, h, r, TRANSPARENT); }
  eraseCircle(cx, cy, r) { this.fillCircle(cx, cy, r, TRANSPARENT); }
}

/* ---------------- 图标绘制（81x81，居中留白） ---------------- */
const SIZE = 81;

// 首页：房子（三角屋顶 + 房体 + 门洞）
function drawHome(color) {
  const cv = new Canvas(SIZE, SIZE);
  cv.fillPolygon([[40, 8], [74, 40], [7, 40]], color);
  cv.fillRoundRect(15, 38, 51, 36, 5, color);
  cv.eraseRect(33, 50, 15, 24); // 门
  return cv;
}

// 教室：楼宇（楼体 + 窗洞 + 门洞）
function drawClassroom(color) {
  const cv = new Canvas(SIZE, SIZE);
  cv.fillRoundRect(13, 12, 55, 58, 6, color);
  cv.eraseRect(21, 20, 12, 10);
  cv.eraseRect(37, 20, 12, 10);
  cv.eraseRect(49, 20, 12, 10);
  cv.eraseRect(21, 34, 12, 10);
  cv.eraseRect(37, 34, 12, 10);
  cv.eraseRect(49, 34, 12, 10);
  cv.eraseRect(33, 50, 15, 20); // 门
  return cv;
}

// 课程表：日历（表体 + 装订条 + 网格点）
function drawTimetable(color) {
  const cv = new Canvas(SIZE, SIZE);
  cv.fillRect(22, 6, 7, 14, color);
  cv.fillRect(52, 6, 7, 14, color);
  cv.fillRoundRect(9, 14, 63, 60, 7, color);
  cv.eraseRect(15, 32, 51, 36); // 内部区域
  // 网格课程点
  const xs = [18, 30, 42, 54];
  const ys = [36, 48, 60];
  for (const y of ys) for (const x of xs) cv.fillRoundRect(x, y, 10, 8, 2, color);
  return cv;
}

// 失物招领：放大镜（圆环 + 斜柄）
function drawLostfound(color) {
  const cv = new Canvas(SIZE, SIZE);
  cv.ring(35, 35, 24, 15, color);
  cv.fillPolygon([[47, 54], [55, 46], [72, 63], [64, 71]], color);
  return cv;
}

// 我的：人形（头 + 肩部半圆）
function drawProfile(color) {
  const cv = new Canvas(SIZE, SIZE);
  cv.fillCircle(40, 26, 14, color);
  cv.fillCircle(40, 76, 28, color);
  cv.fillRect(12, 78, 57, 3, TRANSPARENT); // 底部微收
  return cv;
}

/* ---------------- 输出 ---------------- */
const GRAY = [154, 161, 172, 255];   // #9AA1AC
const BLUE = [61, 126, 255, 255];    // #3D7EFF

const ICONS = {
  home: drawHome,
  classroom: drawClassroom,
  timetable: drawTimetable,
  lostfound: drawLostfound,
  profile: drawProfile
};

const outDir = path.join(__dirname, '..', 'assets', 'tabbar');
fs.mkdirSync(outDir, { recursive: true });

let ok = 0;
for (const [name, draw] of Object.entries(ICONS)) {
  for (const [suffix, color] of [['', GRAY], ['-active', BLUE]]) {
    const cv = draw(color);
    const buf = encodePNG(SIZE, SIZE, cv.data);
    const file = path.join(outDir, `${name}${suffix}.png`);
    fs.writeFileSync(file, buf);
    const size = fs.statSync(file).size;
    if (size > 0) { ok++; console.log(`OK  ${file}  ${size} bytes`); }
    else { console.error(`FAIL ${file} 为空`); process.exitCode = 1; }
  }
}
console.log(`共生成 ${ok}/10 个图标`);
