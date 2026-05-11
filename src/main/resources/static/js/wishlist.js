/**
 * CAKE HOUSE — СПИСОК БАЖАНЬ
 * Зберігається в localStorage під ключем 'cakeWishlist'
 */

const WISHLIST_KEY = 'cakeWishlist';

// ── Читання/запис ─────────────────────────────────────────────
function getWishlist() {
    try { return JSON.parse(localStorage.getItem(WISHLIST_KEY)) || []; }
    catch(e) { return []; }
}

function saveWishlist(list) {
    localStorage.setItem(WISHLIST_KEY, JSON.stringify(list));
    updateWishlistBadge();
}

// ── Додати / видалити ─────────────────────────────────────────
function toggleWishlist(item) {
    var list = getWishlist();
    var idx  = list.findIndex(function(w) { return w.id === item.id; });

    if (idx !== -1) {
        list.splice(idx, 1);
        saveWishlist(list);
        return false; // видалено
    } else {
        list.push(item);
        saveWishlist(list);
        return true;  // додано
    }
}

function isInWishlist(productId) {
    return getWishlist().some(function(w) { return w.id === 'product_' + productId; });
}

function removeFromWishlist(id) {
    saveWishlist(getWishlist().filter(function(w) { return w.id !== id; }));
    renderWishlistPage();
}

// ── Лічильник на іконці (якщо потрібен) ─────────────────────
function updateWishlistBadge() {
    var badge = document.getElementById('wishlistBadge');
    if (badge) {
        var count = getWishlist().length;
        badge.textContent = count > 0 ? count : '';
        badge.style.display = count > 0 ? 'flex' : 'none';
    }
}

// ── Натискання на серце в картці асортименту ─────────────────
function toggleWishlistFromCard(btn, productId, name, type, image, priceUnit, price, discount, weight, unit, urgency) {
    var item = {
        id:            'product_' + productId,
        type:          type,
        productId:     productId,
        name:          name,
        imagePath:     (image && image !== 'null' && image !== '') ? image : null,
        priceUnit:     priceUnit,
        unitPrice:     parseInt(price),
        discountPrice: discount ? parseInt(discount) : null,
        quantity:      parseFloat(weight) || 1,
        unit:          unit || 'шт',
        urgency:       urgency || null
    };

    var added = toggleWishlist(item);

    // Анімація серця
    var heart = btn.querySelector('.heart-icon');
    if (heart) {
        heart.classList.toggle('heart-active', added);
        // Пульс-анімація
        heart.classList.add('heart-pulse');
        setTimeout(function() { heart.classList.remove('heart-pulse'); }, 400);
    }

    // Текст кнопки
    var label = btn.querySelector('.wishlist-label');
    if (label) {
        label.textContent = added ? 'В списку бажань' : 'Додати в список бажань';
    }
}

// ── Ініціалізація сердечок при завантаженні сторінки ─────────
function initWishlistHearts() {
    document.querySelectorAll('.wishlist-btn').forEach(function(btn) {
        var pid = btn.getAttribute('data-product-id');
        if (pid && isInWishlist(pid)) {
            var heart = btn.querySelector('.heart-icon');
            if (heart) heart.classList.add('heart-active');
            var label = btn.querySelector('.wishlist-label');
            if (label) label.textContent = 'В списку бажань';
        }
    });
}

// ── Рендер вкладки "Список бажань" у профілі ─────────────────
function renderWishlistPage() {
    var container = document.getElementById('wishlistContainer');
    if (!container) return;

    var list = getWishlist();

    if (list.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>Ваш список бажань порожній.<br>'
            + '<a href="/assortment" style="color:#3AA6B9;">Перейти до асортименту</a></p></div>';
        return;
    }

    container.innerHTML = '<div class="wishlist-grid">'
        + list.map(function(item) {
            var img = item.imagePath
                ? '<img src="' + item.imagePath + '" class="wishlist-item-img" alt="">'
                : '<div class="wishlist-item-img-placeholder">🎂</div>';

            var priceRow = item.unitPrice + ' ' + item.priceUnit;
            var defenders = item.discountPrice
                ? '<div class="wishlist-item-discount">🇺🇦 Захисникам: ' + item.discountPrice + ' грн</div>'
                : '';

            var weightLabel = item.unit === 'кг'
                ? '<div class="wishlist-item-weight">' + item.quantity + ' кг</div>'
                : '';

            return '<div class="wishlist-item-card">'
                + '<div class="wishlist-item-img-wrap">' + img + '</div>'
                + '<div class="wishlist-item-body">'
                + '<div class="wishlist-item-name">' + esc(item.name) + '</div>'
                + weightLabel
                + '<div class="wishlist-item-price">' + priceRow + '</div>'
                + defenders
                + '<div class="wishlist-item-actions">'
                + '<button class="wishlist-add-cart-btn" onclick="wishlistAddToCart(\'' + item.id + '\')">'
                + '<span class="btn-text">додати в корзину</span>'
                + '<div class="btn-line-circle"><div class="btn-line"></div><div class="btn-circle"></div></div>'
                + '</button>'
                + '<button class="wishlist-remove-btn" onclick="removeFromWishlist(\'' + item.id + '\')" title="Видалити">'
                + heartSVG(false)
                + '</button>'
                + '</div>'
                + '</div>'
                + '</div>';
        }).join('')
        + '</div>';
}

function wishlistAddToCart(id) {
    var item = getWishlist().find(function(w) { return w.id === id; });
    if (!item) return;
    if (typeof addProductToCart === 'function') {
        addProductToCart(
            item.productId, item.name, item.type, item.imagePath,
            item.priceUnit, item.unitPrice, item.discountPrice,
            item.quantity, item.unit, item.urgency
        );
    }
}

// ── SVG серця ─────────────────────────────────────────────────
function heartSVG(filled) {
    var fill = filled ? '#DF7481' : 'none';
    var stroke = '#DF7481';
    return '<svg class="heart-icon' + (filled ? ' heart-active' : '') + '" '
        + 'width="22" height="22" viewBox="0 0 24 24" '
        + 'fill="' + fill + '" stroke="' + stroke + '" stroke-width="2" '
        + 'stroke-linecap="round" stroke-linejoin="round">'
        + '<path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06'
        + 'a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06'
        + 'a5.5 5.5 0 0 0 0-7.78z"/>'
        + '</svg>';
}

function esc(s) {
    return String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}

// ── Ініціалізація ─────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', function() {
    updateWishlistBadge();
    initWishlistHearts();
    renderWishlistPage();
});