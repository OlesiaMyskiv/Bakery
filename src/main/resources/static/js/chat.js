/**
 * CAKE HOUSE — Глобальний чат
 * Підключається на всіх сторінках через header.html
 * Не залежить від sec:authorize — сам перевіряє авторизацію через fetch
 */
(function () {
    'use strict';

    var gcOrderId  = null;
    var gcInterval = null;
    var gcLoaded   = false;

    // ── CSS ──────────────────────────────────────────────────────────────
    var style = document.createElement('style');
    style.textContent = `
        .gc-bubble-btn {
            position: fixed;
            bottom: 28px; right: 28px;
            width: 58px; height: 58px;
            border-radius: 50%;
            background: #3AA6B9;
            border: none;
            cursor: pointer;
            z-index: 9999;
            display: none;           /* спочатку прихований; JS покаже після перевірки */
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 18px rgba(58,166,185,.55);
            transition: transform .2s, box-shadow .2s;
        }
        .gc-bubble-btn:hover {
            transform: scale(1.1);
            box-shadow: 0 6px 24px rgba(58,166,185,.7);
        }
        .gc-bubble-btn svg { width:26px; height:26px; }

        .gc-unread {
            position: absolute;
            top:-3px; right:-3px;
            background:#e74c3c; color:white;
            border-radius:50%; width:20px; height:20px;
            font-size:11px; font-weight:700;
            display:flex; align-items:center; justify-content:center;
            font-family:sans-serif;
        }

        .gc-panel {
            position: fixed;
            bottom: 100px; right: 28px;
            width: 350px; height: 500px;
            min-width: 260px; min-height: 300px;
            max-width: 92vw; max-height: 88vh;
            background: white;
            border-radius: 16px;
            box-shadow: 0 8px 36px rgba(0,0,0,.18);
            z-index: 9998;
            display: none;
            flex-direction: column;
            overflow: hidden;
            font-family: 'Cormorant Garamond', Georgia, serif;
        }
        .gc-panel.gc-open { display:flex; }

        .gc-head {
            background: #3AA6B9;
            color: white;
            padding: 14px 16px;
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            cursor: move;
            flex-shrink: 0;
            border-radius: 16px 16px 0 0;
            user-select: none;
        }
        .gc-head-title { font-size:18px; font-weight:700; }
        .gc-head-sub   { font-size:13px; opacity:.8; margin-top:2px; }
        .gc-x {
            background:none; border:none; color:white;
            font-size:20px; cursor:pointer; padding:0; line-height:1; opacity:.85;
        }
        .gc-x:hover { opacity:1; }

        .gc-sel-wrap {
            padding:10px 14px;
            border-bottom:1px solid #f0f0ec;
            background:#f8f9fa; flex-shrink:0;
        }
        .gc-sel-wrap label { font-size:12px; color:#888; display:block; margin-bottom:4px; }
        .gc-sel {
            width:100%; padding:7px 10px;
            border:1px solid #ddd; border-radius:8px;
            font-family:inherit; font-size:15px; background:white;
        }
        .gc-sel:focus { outline:none; border-color:#3AA6B9; }

        .gc-msgs {
            flex:1; overflow-y:auto;
            padding:12px;
            display:flex; flex-direction:column; gap:8px;
            background:#f8f9fa;
        }
        .gc-hint {
            text-align:center; color:#bbb;
            font-size:15px; padding:24px 12px; line-height:1.5;
        }

        .gc-bbl {
            max-width:82%; padding:8px 12px; border-radius:14px;
            font-size:15px; line-height:1.4; word-break:break-word;
        }
        .gc-bbl.mine {
            background:#3AA6B9; color:white;
            align-self:flex-end; border-bottom-right-radius:3px;
        }
        .gc-bbl.theirs {
            background:white; color:#1a1a1a;
            align-self:flex-start;
            border:1px solid #e8e8e4; border-bottom-left-radius:3px;
        }
        .gc-sender { font-size:11px; font-weight:700; color:#3AA6B9; margin-bottom:3px; }
        .gc-time   { font-size:11px; opacity:.6; margin-top:3px; text-align:right; }
        .gc-bbl.theirs .gc-time { text-align:left; }

        .gc-inp-area {
            padding:10px 12px;
            border-top:1px solid #e8e8e4;
            display:flex; gap:8px;
            background:white; flex-shrink:0;
            border-radius:0 0 16px 16px;
        }
        .gc-txt {
            flex:1; border:1px solid #ddd; border-radius:16px;
            padding:8px 13px;
            font-family:inherit; font-size:15px;
            resize:none; height:38px; outline:none;
            transition:border-color .15s;
        }
        .gc-txt:focus { border-color:#3AA6B9; }
        .gc-send-btn {
            background:#3AA6B9; color:white; border:none;
            border-radius:50%; width:38px; height:38px;
            cursor:pointer; flex-shrink:0;
            display:flex; align-items:center; justify-content:center;
            transition:background .15s;
        }
        .gc-send-btn:hover { background:#2d8a9e; }

        /* resize ручки */
        .gc-rz-b  { position:absolute; bottom:0;left:0; width:100%;height:6px; cursor:ns-resize; }
        .gc-rz-l  { position:absolute; top:0;left:0;    width:6px;height:100%; cursor:ew-resize; }
        .gc-rz-c  { position:absolute; bottom:0;left:0; width:20px;height:20px; cursor:nwse-resize; }
    `;
    document.head.appendChild(style);

    // ── HTML ─────────────────────────────────────────────────────────────
    var wrap = document.createElement('div');
    wrap.innerHTML = `
        <button class="gc-bubble-btn" id="gcBubble" title="Чат з менеджером">
            <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="1.8"
                 stroke-linecap="round" stroke-linejoin="round">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                <circle cx="9"  cy="10" r="1" fill="white" stroke="none"/>
                <circle cx="12" cy="10" r="1" fill="white" stroke="none"/>
                <circle cx="15" cy="10" r="1" fill="white" stroke="none"/>
            </svg>
            <span class="gc-unread" id="gcUnread" style="display:none;"></span>
        </button>

        <div class="gc-panel" id="gcPanel">
            <div class="gc-rz-b" id="gcRzB"></div>
            <div class="gc-rz-l" id="gcRzL"></div>
            <div class="gc-rz-c" id="gcRzC"></div>

            <div class="gc-head" id="gcHead">
                <div>
                    <div class="gc-head-title">💬 Чат з менеджером</div>
                    <div class="gc-head-sub">Ми відповімо найближчим часом</div>
                </div>
                <button class="gc-x" id="gcClose">✕</button>
            </div>

            <div class="gc-sel-wrap">
                <label>Оберіть замовлення:</label>
                <select class="gc-sel" id="gcSel">
                    <option value="">— оберіть замовлення —</option>
                </select>
            </div>

            <div class="gc-msgs" id="gcMsgs">
                <div class="gc-hint">Оберіть замовлення вище щоб переглянути чат</div>
            </div>

            <div class="gc-inp-area">
                <textarea class="gc-txt" id="gcTxt" placeholder="Напишіть повідомлення..."></textarea>
                <button class="gc-send-btn" id="gcSendBtn">
                    <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2" stroke-linecap="round">
                        <line x1="22" y1="2" x2="11" y2="13"/>
                        <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                    </svg>
                </button>
            </div>
        </div>
    `;
    document.body.appendChild(wrap);

    // ── ЕЛЕМЕНТИ ─────────────────────────────────────────────────────────
    var bubble  = document.getElementById('gcBubble');
    var panel   = document.getElementById('gcPanel');
    var closeBtn= document.getElementById('gcClose');
    var sel     = document.getElementById('gcSel');
    var msgs    = document.getElementById('gcMsgs');
    var txt     = document.getElementById('gcTxt');
    var sendBtn = document.getElementById('gcSendBtn');

    // ── ПЕРЕВІРКА АВТОРИЗАЦІЇ ─────────────────────────────────────────────
    // Якщо /api/my-orders повертає 200 — юзер залогінений і не адмін
    fetch('/api/my-orders', { credentials: 'include' })
        .then(function (r) {
            if (r.ok) {
                // Показуємо кнопку чату
                bubble.style.display = 'flex';
            }
            // 401/403 — не залогінений або адмін → не показуємо
        })
        .catch(function () { /* мережева помилка — не показуємо */ });

    // ── ВІДКРИТИ / ЗАКРИТИ ────────────────────────────────────────────────
    bubble.addEventListener('click', function () {
        panel.classList.add('gc-open');
        bubble.style.display = 'none';
        if (!gcLoaded) { gcLoaded = true; loadOrders(); }
    });

    closeBtn.addEventListener('click', close);
    function close() {
        panel.classList.remove('gc-open');
        bubble.style.display = 'flex';
        if (gcInterval) clearInterval(gcInterval);
        gcInterval = null;
    }

    // ── ЗАМОВЛЕННЯ ────────────────────────────────────────────────────────
    function loadOrders() {
        fetch('/api/my-orders', { credentials: 'include' })
            .then(function (r) { return r.json(); })
            .then(function (orders) {
                sel.innerHTML = '<option value="">— оберіть замовлення —</option>';
                if (!orders || orders.length === 0) {
                    msgs.innerHTML = '<div class="gc-hint">У вас ще немає замовлень 🎂<br>Після оформлення замовлення ви зможете написати менеджеру.</div>';
                    return;
                }
                orders.forEach(function (o) {
                    var opt = document.createElement('option');
                    opt.value = o.id;
                    opt.textContent = '№' + o.id + ' — ' + (o.composition || 'Замовлення');
                    sel.appendChild(opt);
                });
                if (orders.length === 1) {
                    sel.value = orders[0].id;
                    loadMsgs();
                }
            })
            .catch(function () {
                msgs.innerHTML = '<div class="gc-hint">Не вдалось завантажити замовлення</div>';
            });
    }

    sel.addEventListener('change', loadMsgs);

    function loadMsgs() {
        gcOrderId = sel.value;
        if (gcInterval) clearInterval(gcInterval);
        if (!gcOrderId) {
            msgs.innerHTML = '<div class="gc-hint">Оберіть замовлення вище щоб переглянути чат</div>';
            return;
        }
        fetchMsgs();
        gcInterval = setInterval(fetchMsgs, 5000);
    }

    function fetchMsgs() {
        if (!gcOrderId) return;
        fetch('/chat/' + gcOrderId + '/messages', { credentials: 'include' })
            .then(function (r) { return r.json(); })
            .then(function (data) {
                if (!data || data.length === 0) {
                    msgs.innerHTML = '<div class="gc-hint">Напишіть перше повідомлення 🎂</div>';
                    return;
                }
                msgs.innerHTML = data.map(function (m) {
                    var cls  = m.isAdmin ? 'theirs' : 'mine';
                    var time = m.sentAt ? m.sentAt.substring(11, 16) : '';
                    return '<div class="gc-bbl ' + cls + '">' +
                        (m.isAdmin ? '<div class="gc-sender">' + esc(m.senderName) + '</div>' : '') +
                        '<div>' + esc(m.text) + '</div>' +
                        '<div class="gc-time">' + time + '</div>' +
                        '</div>';
                }).join('');
                msgs.scrollTop = msgs.scrollHeight;
            })
            .catch(function () {});
    }

    // ── НАДІСЛАТИ ─────────────────────────────────────────────────────────
    sendBtn.addEventListener('click', send);
    txt.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); send(); }
    });

    function send() {
        if (!gcOrderId) return;
        var text = txt.value.trim();
        if (!text) return;
        fetch('/chat/' + gcOrderId + '/send', {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ text: text })
        })
            .then(function () { txt.value = ''; fetchMsgs(); })
            .catch(function () {});
    }

    // ── DRAG ──────────────────────────────────────────────────────────────
    var head = document.getElementById('gcHead');
    var dragging = false, dx0, dy0, px0, py0;

    head.addEventListener('mousedown', function (e) {
        if (e.target === closeBtn) return;
        dragging = true;
        var r = panel.getBoundingClientRect();
        dx0 = e.clientX; dy0 = e.clientY;
        px0 = r.left;    py0 = r.top;
        panel.style.right = 'auto'; panel.style.bottom = 'auto';
        panel.style.left  = px0 + 'px'; panel.style.top = py0 + 'px';
        e.preventDefault();
    });
    document.addEventListener('mousemove', function (e) {
        if (!dragging) return;
        var nl = Math.max(0, Math.min(window.innerWidth  - panel.offsetWidth,  px0 + e.clientX - dx0));
        var nt = Math.max(0, Math.min(window.innerHeight - panel.offsetHeight, py0 + e.clientY - dy0));
        panel.style.left = nl + 'px'; panel.style.top = nt + 'px';
    });
    document.addEventListener('mouseup', function () { dragging = false; });

    // ── RESIZE ────────────────────────────────────────────────────────────
    function mkResize(el, dir) {
        var rs = false, sw, sh, sl, st, mx, my;
        el.addEventListener('mousedown', function (e) {
            rs = true;
            var r = panel.getBoundingClientRect();
            sw = r.width; sh = r.height; sl = r.left; st = r.top;
            mx = e.clientX; my = e.clientY;
            panel.style.right = 'auto'; panel.style.bottom = 'auto';
            panel.style.left  = sl + 'px'; panel.style.top = st + 'px';
            e.preventDefault(); e.stopPropagation();
        });
        document.addEventListener('mousemove', function (e) {
            if (!rs) return;
            if (dir === 'b' || dir === 'c') {
                panel.style.height = Math.max(300, Math.min(window.innerHeight - st, sh + e.clientY - my)) + 'px';
            }
            if (dir === 'l' || dir === 'c') {
                var nw = Math.max(260, sw - (e.clientX - mx));
                var nl = sl + (e.clientX - mx);
                if (nl >= 0 && nw >= 260) { panel.style.width = nw + 'px'; panel.style.left = nl + 'px'; }
            }
        });
        document.addEventListener('mouseup', function () { rs = false; });
    }
    mkResize(document.getElementById('gcRzB'), 'b');
    mkResize(document.getElementById('gcRzL'), 'l');
    mkResize(document.getElementById('gcRzC'), 'c');

    function esc(s) {
        return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }
})();
