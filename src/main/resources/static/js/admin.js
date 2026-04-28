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