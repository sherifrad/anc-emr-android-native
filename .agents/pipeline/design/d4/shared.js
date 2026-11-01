/* Shared by every mock page. Light theme by default, A-/A+ text size with a hard limit, and a fit pass
   so short labels (pills, chips, buttons, row names) stay on ONE line, never cut, never wrapped.
   Mirrors the rule the app must follow: bounded text scale + maxLines=1 with shrink-to-fit for short labels. */
(function () {
  var STEPS = [0.9, 1, 1.1, 1.2, 1.3], DEF = 1, SHORT = 30, FLOOR = 0.8;
  var root = document.documentElement, idx = DEF, big = null;
  function get(k, d) { try { var v = localStorage.getItem(k); return v === null ? d : v; } catch (e) { return d; } }
  function put(k, v) { try { localStorage.setItem(k, v); } catch (e) {} }
  function setTheme(t) { root.setAttribute('data-theme', t); put('anc-theme', t); var b = document.getElementById('ancTh'); if (b) b.textContent = t === 'light' ? 'Light' : 'Dark'; }
  function leaves() {
    var out = [], all = document.querySelectorAll('.phone *');
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      if (el.children.length || el.closest('[data-wrap]') || /^(SCRIPT|STYLE|SVG|PATH|INPUT|TEXTAREA|OPTION)$/i.test(el.tagName)) continue;
      var t = (el.textContent || '').trim();
      if (t && t.length <= SHORT && el.offsetParent !== null) out.push(el);
    }
    return out;
  }
  function lines(el) {
    var r = document.createRange(); r.selectNodeContents(el);
    var rects = r.getClientRects(), tops = [];
    for (var i = 0; i < rects.length; i++) {
      if (rects[i].width < 1) continue;
      var t = Math.round(rects[i].top / 3);
      if (tops.indexOf(t) < 0) tops.push(t);
    }
    return Math.max(tops.length, 1);
  }
  function fits(el) { return lines(el) <= 1 && el.scrollWidth <= el.clientWidth + 1; }
  function fitAll() {
    var bad = 0, els = leaves();
    els.forEach(function (el) { el.style.fontSize = ''; el.style.whiteSpace = 'nowrap'; });
    els.forEach(function (el) {
      if (fits(el)) return;
      var base = parseFloat(getComputedStyle(el).fontSize), s = base;
      while (!fits(el) && s > base * FLOOR) { s -= 0.5; el.style.fontSize = s + 'px'; }
      if (!fits(el)) bad++;
    });
    return bad;
  }
  function apply(i) {
    idx = i; root.style.setProperty('--fs', STEPS[i]); if (document.body) document.body.style.setProperty('--fs', STEPS[i]);
    var l = document.getElementById('ancPct'); if (l) l.textContent = Math.round(STEPS[i] * 100) + '%';
    return fitAll();
  }
  function refresh() {
    var m = document.getElementById('ancMinus'), p = document.getElementById('ancPlus');
    if (m) m.disabled = idx === 0;
    if (p) { p.disabled = idx === STEPS.length - 1; p.title = p.disabled ? 'Largest size' : 'Larger text'; }
  }
  function bigger() {
    if (idx >= STEPS.length - 1) return;
    var prev = idx, bad = apply(idx + 1);
    if (bad > 0) { apply(prev); toast('Largest size that fits this screen'); }
    put('anc-fs', idx); refresh();
  }
  function smaller() { if (idx > 0) { apply(idx - 1); put('anc-fs', idx); refresh(); } }
  function toast(msg) {
    var t = document.getElementById('ancToast'); if (!t) return;
    t.textContent = msg; t.style.opacity = 1; setTimeout(function () { t.style.opacity = 0; }, 1800);
  }
  function build() {
    var css = document.createElement('style');
    css.textContent = '#ancBar{position:fixed;left:50%;transform:translateX(-50%);bottom:10px;z-index:9999;display:flex;gap:6px;align-items:center;background:#0D1B3D;color:#fff;border-radius:28px;padding:4px 8px;box-shadow:0 4px 16px #0005;font:600 14px system-ui}' +
      '#ancBar button{min-width:48px;min-height:48px;border:0;border-radius:24px;background:transparent;color:#fff;font:inherit;font-size:16px;cursor:pointer}' +
      '#ancBar button:disabled{opacity:.35;cursor:default}#ancBar button:active:not(:disabled){background:#fff3}' +
      '#ancBar span{min-width:44px;text-align:center}#ancToast{position:fixed;left:50%;transform:translateX(-50%);bottom:76px;background:#000c;color:#fff;border-radius:14px;padding:8px 14px;font:13px system-ui;opacity:0;transition:opacity .2s;z-index:9999;pointer-events:none}';
    document.head.appendChild(css);
    var bar = document.createElement('div'); bar.id = 'ancBar'; bar.setAttribute('role', 'group'); bar.setAttribute('aria-label', 'Text size and theme');
    bar.innerHTML = '<button id="ancMinus" aria-label="Smaller text">A−</button><span id="ancPct">100%</span><button id="ancPlus" aria-label="Larger text">A+</button><button id="ancTh" aria-label="Light or dark">Light</button>';
    document.body.appendChild(bar);
    var t = document.createElement('div'); t.id = 'ancToast'; document.body.appendChild(t);
    document.getElementById('ancMinus').onclick = smaller; document.getElementById('ancPlus').onclick = bigger;
    document.getElementById('ancTh').onclick = function () { setTheme(root.getAttribute('data-theme') === 'light' ? 'dark' : 'light'); };
  }
  function start() {
    setTheme(get('anc-theme', 'light'));
    build();
    var s = parseInt(get('anc-fs', DEF), 10); if (!(s >= 0 && s < STEPS.length)) s = DEF;
    apply(s); if (fitAll() > 0 && s > DEF) apply(DEF); refresh();
    var timer = null;
    new MutationObserver(function () { clearTimeout(timer); timer = setTimeout(fitAll, 60); }).observe(document.body, { childList: true, subtree: true, characterData: true });
    window.addEventListener('resize', function () { fitAll(); });
  }
  window.AncUI = { fit: fitAll, bigger: bigger, smaller: smaller };
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', start); else start();
})();
