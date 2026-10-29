// Shared bits of the illustration pages: the language switch (?lang=ar|fr|en) and the eight-point star used as a badge.
function starPoints(cx, cy, R, r) {
  const p = [];
  for (let k = 0; k < 16; k++) {
    const a = (k * 22.5 - 90) * Math.PI / 180, rad = k % 2 ? r : R;
    p.push((cx + rad * Math.cos(a)).toFixed(2) + "," + (cy + rad * Math.sin(a)).toFixed(2));
  }
  return p.join(" ");
}
// <svg><use href="#st"/></svg> draws the star in a 100x100 box
document.body.insertAdjacentHTML("afterbegin",
  '<svg width="0" height="0" style="position:absolute"><defs><polygon id="st" points="' + starPoints(50, 50, 48, 33) + '"/></defs></svg>');
function pageLang(dict) {
  const q = new URLSearchParams(location.search).get("lang") || "fr", c = dict[q];
  document.documentElement.lang = q;
  document.getElementById("root").dir = c.dir;
  return c;
}
const CHECK_BADGE = '<svg class="ck" viewBox="0 0 100 100"><use href="#st" fill="#D1AB68"/><path d="M29 52l14 14 28-31" fill="none" stroke="#0b1d13" stroke-width="9" stroke-linecap="round" stroke-linejoin="round"/></svg>';
