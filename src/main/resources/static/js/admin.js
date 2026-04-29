/**
 * Зміна ціни за допомогою кнопок +/- з перевіркою на порожнє значення
 */
function changePrice(delta) {
    const input = document.getElementById('priceInput');
    let val = parseInt(input.value);
    if (isNaN(val)) val = 0;
    input.value = Math.max(1, val + delta);
}

/**
 * Підтвердження видалення товару
 */
function confirmDelete(btn) {
    const name = btn.getAttribute('data-name');
    if (confirm(`Ви впевнені, що хочете видалити "${name}"?`)) {
        btn.closest('form').submit();
    }
}

/* Показати блок "для кого" після вибору події */
function showDesignFor() {
    var select = document.getElementById('designEventSelect');
    var block  = document.getElementById('designForBlock');
    if (select && block) {
        block.style.display = select.value ? 'block' : 'none';
    }
}

/* Пошук товарів у таблиці */
function filterProducts() {
    var searchInput = document.getElementById('productSearch');
    if (!searchInput) return;

    var q = searchInput.value.toLowerCase().trim();
    var rows      = document.querySelectorAll('#productsTable tbody tr[data-name]');
    var clearBtn  = document.getElementById('clearBtn');
    var countEl   = document.getElementById('searchCount');
    var noResults = document.getElementById('noResults');
    var visible   = 0;

    if (clearBtn) clearBtn.style.display = q ? 'block' : 'none';

    rows.forEach(function(row) {
        var name = (row.getAttribute('data-name') || '').toLowerCase();
        if (name.includes(q)) {
            row.style.display = '';
            visible++;
        } else {
            row.style.display = 'none';
        }
    });

    if (countEl) countEl.textContent = q ? 'Знайдено: ' + visible : '';
    if (noResults) noResults.style.display = (q && visible === 0) ? 'block' : 'none';
}

/* Очищення пошуку */
function clearSearch() {
    var searchInput = document.getElementById('productSearch');
    if (searchInput) {
        searchInput.value = '';
        filterProducts();
        searchInput.focus();
    }
}

// ── ГЛОБАЛЬНА ПЕРЕВІРКА НЕПРОЧИТАНИХ ЧАТІВ ДЛЯ АДМІНА ────────────

// Функція для перевірки непрочитаних повідомлень у всіх чатах
function updateGlobalUnreadStatus() {
    const dot = document.getElementById('admin-global-unread-dot');
    if (!dot) return; // Якщо на сторінці немає сайдбару, нічого не робимо

    fetch('/api/admin/chat/sessions', { credentials: 'include' })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(sessions => {
            // Перевіряємо, чи є хоча б одна сесія з позначкою unread: true
            const hasUnread = sessions.some(session => session.unread);

            if (hasUnread) {
                dot.style.display = 'block';
            } else {
                dot.style.display = 'none';
            }
        })
        .catch(error => console.error('Помилка перевірки чатів:', error));
}

// Запускаємо перевірку при завантаженні сторінки
document.addEventListener('DOMContentLoaded', () => {
    updateGlobalUnreadStatus();

    // Встановлюємо інтервал перевірки кожні 10 секунд
    setInterval(updateGlobalUnreadStatus, 10000);
});