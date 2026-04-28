'use strict';
/**
 * CAKE HOUSE — Глобальний чат
 * Доступний ВСІМ: зареєстрованим і гостям
 * Не показується адміну (перевірка через /api/admin/chat/sessions — якщо відповідає, то адмін)
 */
(function () {

    var SESSION_ID   = null;
    var GUEST_TOKEN  = null;
    var pollInterval = null;

    // ── CSS ──────────────────────────────────────────────────────────────
    var s = document.createElement('style');
    s.textContent = `
        .gc-fab {
            position:fixed; bottom:24px; right:24px;
            width:60px; height:60px; border-radius:50%;
            background:#3AA6B9; border:none; cursor:pointer;
            z-index:9999; display:flex; align-items:center; justify-content:center;
            box-shadow:0 4px 20px rgba(58,166,185,.5);
            transition:transform .2s,box-shadow .2s;
        }
        .gc-fab:hover { transform:scale(1.08); box-shadow:0 6px 28px rgba(58,166,185,.65); }
        .gc-fab img   { width:28px; height:28px; filter:brightness(0) invert(1); }
        .gc-badge {
            position:absolute; top:-4px; right:-4px;
            background:#e74c3c; color:#fff;
            border-radius:50%; width:20px; height:20px;
            font-size:11px; font-family:sans-serif; font-weight:700;
            display:none; align-items:center; justify-content:center;
        }

        .gc-panel {
            position:fixed; bottom:100px; right:24px;
            width:340px; height:480px;
            min-width:260px; min-height:300px;
            background:#fff; border-radius:16px;
            box-shadow:0 8px 40px rgba(0,0,0,.18);
            z-index:9998; display:none; flex-direction:column;
            overflow:hidden;
            font-family:'Cormorant Garamond',Georgia,serif;
        }
        .gc-panel.open { display:flex; }

        .gc-head {
            background:#3AA6B9; color:#fff;
            padding:13px 15px; display:flex;
            justify-content:space-between; align-items:flex-start;
            cursor:move; user-select:none; flex-shrink:0;
        }
        .gc-head h3  { margin:0; font-size:17px; font-weight:700; }
        .gc-head p   { margin:2px 0 0; font-size:12px; opacity:.8; }
        .gc-btn-x    { background:none; border:none; color:#fff; font-size:20px; cursor:pointer; padding:0; }

        .gc-name-wrap {
            padding:10px 14px; background:#f8f9fa;
            border-bottom:1px solid #eee; flex-shrink:0;
            display:none; /* показується тільки для гостей */
        }
        .gc-name-wrap label { font-size:12px; color:#888; display:block; margin-bottom:4px; }
        .gc-name-inp {
            width:100%; padding:7px 10px; border:1px solid #ddd;
            border-radius:8px; font-family:inherit; font-size:15px;
        }
        .gc-name-inp:focus { outline:none; border-color:#3AA6B9; }

        .gc-msgs {
            flex:1; overflow-y:auto; padding:12px;
            display:flex; flex-direction:column; gap:8px;
            background:#f8f9fa;
        }
        .gc-hint { text-align:center; color:#bbb; font-size:14px; padding:20px 12px; line-height:1.5; }

        .gc-bbl {
            max-width:80%; padding:8px 12px; border-radius:14px;
            font-size:15px; line-height:1.4; word-break:break-word;
        }
        .gc-bbl.mine   { background:#3AA6B9; color:#fff; align-self:flex-end; border-bottom-right-radius:3px; }
        .gc-bbl.theirs { background:#fff; color:#1a1a1a; align-self:flex-start; border:1px solid #e8e4e4; border-bottom-left-radius:3px; }
        .gc-admin-lbl  { font-size:11px; font-weight:700; color:#3AA6B9; margin-bottom:3px; }
        .gc-time       { font-size:11px; opacity:.55; margin-top:3px; text-align:right; }
        .gc-bbl.theirs .gc-time { text-align:left; }

        .gc-inp {
            padding:10px 12px; border-top:1px solid #eee;
            display:flex; gap:8px; background:#fff; flex-shrink:0;
        }
        .gc-ta {
            flex:1; border:1px solid #ddd; border-radius:14px;
            padding:8px 12px; font-family:inherit; font-size:15px;
            resize:none; height:38px; outline:none;
        }
        .gc-ta:focus { border-color:#3AA6B9; }
        .gc-send {
            background:#3AA6B9; border:none; border-radius:50%;
            width:38px; height:38px; cursor:pointer;
            display:flex; align-items:center; justify-content:center;
            flex-shrink:0;
        }
        .gc-send:hover { background:#2d8a9e; }
        .gc-send svg { width:16px; height:16px; }

        /* resize handles */
        .gc-rz-t { position:absolute; top:0;left:0; width:100%;height:5px; cursor:ns-resize; }
        .gc-rz-l { position:absolute; top:0;left:0; width:5px;height:100%; cursor:ew-resize; }
        .gc-rz-c { position:absolute; top:0;left:0; width:16px;height:16px; cursor:nwse-resize; }
    `;
    document.head.appendChild(s);

    // ── HTML ─────────────────────────────────────────────────────────────
    var d = document.createElement('div');
    d.innerHTML = `
        <button class="gc-fab" id="gcFab" title="Чат з менеджером">
            <img src="/img/chat-icon.svg" alt="чат">
            <span class="gc-badge" id="gcBadge"></span>
        </button>
        <div class="gc-panel" id="gcPanel">
            <div class="gc-rz-t" id="gcRzT"></div>
            <div class="gc-rz-l" id="gcRzL"></div>
            <div class="gc-rz-c" id="gcRzC"></div>
            <div class="gc-head" id="gcHead">
                <div>
                    <h3>💬 Чат з менеджером</h3>
                    <p>Ми відповімо найближчим часом</p>
                </div>
                <button class="gc-btn-x" id="gcClose">✕</button>
            </div>
            <div class="gc-name-wrap" id="gcNameWrap">
                <label>Ваше ім'я (необов'язково):</label>
                <input class="gc-name-inp" id="gcNameInp" placeholder="Наприклад: Марія" maxlength="50">
            </div>
            <div class="gc-msgs" id="gcMsgs">
                <div class="gc-hint">Вітаємо! 👋<br>Напишіть ваше питання і ми відповімо.</div>
            </div>
            <div class="gc-inp">
                <textarea class="gc-ta" id="gcTa" placeholder="Напишіть повідомлення..."></textarea>
                <button class="gc-send" id="gcSendBtn">
                    <svg viewBox="0 0 24 24" fill="none" stroke="white" stroke-width="2.2" stroke-linecap="round">
                        <line x1="22" y1="2" x2="11" y2="13"/>
                        <polygon points="22 2 15 22 11 13 2 9 22 2" fill="white" stroke="none"/>
                    </svg>
                </button>
            </div>
        </div>`;
    document.body.appendChild(d);

    var fab    = document.getElementById('gcFab');
    var panel  = document.getElementById('gcPanel');
    var badge  = document.getElementById('gcBadge');
    var head   = document.getElementById('gcHead');
    var closeB = document.getElementById('gcClose');
    var msgs   = document.getElementById('gcMsgs');
    var ta     = document.getElementById('gcTa');
    var sendB  = document.getElementById('gcSendBtn');
    var nameW  = document.getElementById('gcNameWrap');
    var nameI  = document.getElementById('gcNameInp');

    // ── ПЕРЕВІРКА ЧИ АДМІН — якщо так, кнопку не показуємо ──────────────
    fetch('/api/admin/chat/sessions', { credentials:'include' })
        .then(function(r) {
            if (r.ok) {
                // Адмін — чат-бабл не показуємо
                fab.style.display = 'none';
            } else {
                // Не адмін — показуємо
                fab.style.display = 'flex';
                initGuestOrUser();
            }
        })
        .catch(function() {
            // Помилка мережі — показуємо для безпеки
            fab.style.display = 'flex';
            initGuestOrUser();
        });

    // ── ІНІЦІАЛІЗАЦІЯ (гість або юзер) ───────────────────────────────────
    function initGuestOrUser() {
        // Якщо гість — показуємо поле імені
        fetch('/api/my-orders', { credentials:'include' })
            .then(function(r) {
                if (!r.ok) {
                    // Гість — показуємо поле імені
                    nameW.style.display = 'block';
                }
            })
            .catch(function() { nameW.style.display = 'block'; });
    }

    // ── ВІДКРИТИ / ЗАКРИТИ ───────────────────────────────────────────────
    fab.addEventListener('click', function() {
        panel.classList.add('open');
        fab.style.display = 'none';
        badge.style.display = 'none';
        if (!SESSION_ID) startSession();
        else startPolling();
    });

    closeB.addEventListener('click', function() {
        panel.classList.remove('open');
        fab.style.display = 'flex';
        stopPolling();
    });

    // ── СТВОРИТИ / ОТРИМАТИ СЕСІЮ ─────────────────────────────────────────
    function startSession() {
        // Гість — генеруємо/беремо токен з localStorage
        GUEST_TOKEN = localStorage.getItem('cake_chat_token');
        if (!GUEST_TOKEN) {
            GUEST_TOKEN = 'g-' + Math.random().toString(36).slice(2) + Date.now();
            localStorage.setItem('cake_chat_token', GUEST_TOKEN);
        }

        var guestName = nameI.value.trim() || localStorage.getItem('cake_chat_name') || '';
        if (guestName) localStorage.setItem('cake_chat_name', guestName);
        if (nameI.value.trim() === '' && guestName) nameI.value = guestName;

        fetch('/api/chat/session', {
            method:'POST', credentials:'include',
            headers:{'Content-Type':'application/json'},
            body: JSON.stringify({ guestToken: GUEST_TOKEN, guestName: guestName })
        })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                SESSION_ID = data.sessionId;
                fetchMsgs();
                startPolling();
            })
            .catch(function(e) { console.error('Session error:', e); });
    }

    // ── ПОВІДОМЛЕННЯ ─────────────────────────────────────────────────────
    function fetchMsgs() {
        if (!SESSION_ID) return;
        var url = '/api/chat/' + SESSION_ID + '/messages?guestToken=' + (GUEST_TOKEN || '');
        fetch(url, { credentials:'include' })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                if (!data || data.length === 0) {
                    msgs.innerHTML = '<div class="gc-hint">Вітаємо! 👋<br>Напишіть ваше питання і ми відповімо.</div>';
                    return;
                }
                msgs.innerHTML = data.map(function(m) {
                    var cls = m.fromAdmin ? 'theirs' : 'mine';
                    var t   = m.sentAt ? m.sentAt.substring(11,16) : '';
                    return '<div class="gc-bbl '+cls+'">' +
                        (m.fromAdmin ? '<div class="gc-admin-lbl">Менеджер</div>' : '') +
                        '<div>'+esc(m.text)+'</div>' +
                        '<div class="gc-time">'+t+'</div></div>';
                }).join('');
                msgs.scrollTop = msgs.scrollHeight;
            })
            .catch(function(){});
    }

    function startPolling() { stopPolling(); pollInterval = setInterval(fetchMsgs, 4000); }
    function stopPolling()  { if (pollInterval) { clearInterval(pollInterval); pollInterval = null; } }

    // ── НАДІСЛАТИ ─────────────────────────────────────────────────────────
    sendB.addEventListener('click', doSend);
    ta.addEventListener('keydown', function(e) {
        if (e.key==='Enter' && !e.shiftKey) { e.preventDefault(); doSend(); }
    });

    function doSend() {
        var text = ta.value.trim();
        if (!text) return;

        // Якщо сесії ще немає — спочатку створюємо
        if (!SESSION_ID) {
            startSession();
            // Після сесії відправимо повторно
            setTimeout(function() {
                if (SESSION_ID) _send(text);
            }, 800);
        } else {
            _send(text);
        }
    }

    function _send(text) {
        ta.value = '';
        fetch('/api/chat/' + SESSION_ID + '/send', {
            method:'POST', credentials:'include',
            headers:{'Content-Type':'application/json'},
            body: JSON.stringify({ text: text, guestToken: GUEST_TOKEN || '' })
        })
            .then(function() { fetchMsgs(); })
            .catch(function(){});
    }

    // ── DRAG ─────────────────────────────────────────────────────────────
    var dragging=false, dx0,dy0,px0,py0;
    head.addEventListener('mousedown', function(e) {
        if (e.target===closeB) return;
        dragging=true;
        var r=panel.getBoundingClientRect();
        dx0=e.clientX; dy0=e.clientY; px0=r.left; py0=r.top;
        panel.style.right='auto'; panel.style.bottom='auto';
        panel.style.left=px0+'px'; panel.style.top=py0+'px';
        e.preventDefault();
    });
    document.addEventListener('mousemove', function(e) {
        if(!dragging) return;
        panel.style.left=Math.max(0,Math.min(window.innerWidth-panel.offsetWidth, px0+e.clientX-dx0))+'px';
        panel.style.top =Math.max(0,Math.min(window.innerHeight-panel.offsetHeight,py0+e.clientY-dy0))+'px';
    });
    document.addEventListener('mouseup', function(){ dragging=false; });

    // ── RESIZE ───────────────────────────────────────────────────────────
    function mkRz(el, dir) {
        var rs=false,sw,sh,sl,st,mx,my;
        el.addEventListener('mousedown', function(e) {
            rs=true; var r=panel.getBoundingClientRect();
            sw=r.width; sh=r.height; sl=r.left; st=r.top; mx=e.clientX; my=e.clientY;
            panel.style.right='auto'; panel.style.bottom='auto';
            panel.style.left=sl+'px'; panel.style.top=st+'px';
            e.preventDefault(); e.stopPropagation();
        });
        document.addEventListener('mousemove', function(e) {
            if(!rs) return;
            if(dir==='t'||dir==='c') {
                var nh=Math.max(300, sh-(e.clientY-my));
                var nt=st+(e.clientY-my);
                if(nt>=0){ panel.style.height=nh+'px'; panel.style.top=nt+'px'; }
            }
            if(dir==='l'||dir==='c') {
                var nw=Math.max(260, sw-(e.clientX-mx));
                var nl=sl+(e.clientX-mx);
                if(nl>=0&&nw>=260){ panel.style.width=nw+'px'; panel.style.left=nl+'px'; }
            }
        });
        document.addEventListener('mouseup', function(){ rs=false; });
    }
    mkRz(document.getElementById('gcRzT'),'t');
    mkRz(document.getElementById('gcRzL'),'l');
    mkRz(document.getElementById('gcRzC'),'c');

    function esc(s) {
        return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
    }
})();