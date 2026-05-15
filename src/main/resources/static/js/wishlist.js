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

// ── Лічильник на іконці ───────────────────────────────────────
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

    var heart = btn.querySelector('.heart-icon');
    if (heart) {
        heart.classList.toggle('heart-active', added);
        heart.classList.add('heart-pulse');
        setTimeout(function() { heart.classList.remove('heart-pulse'); }, 400);
    }

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

// ── Додати зі списку бажань в кошик ──────────────────────────
function wishlistAddToCart(id) {
    var item = getWishlist().find(function(w) { return w.id === id; });
    if (!item) return;

    // Напряму в localStorage — обходимо перевірку IS_LOGGED_IN в addToCart
    // (користувач вже авторизований, бо він на сторінці профілю)
    var cart;
    try { cart = JSON.parse(localStorage.getItem('cakeCart')) || []; }
    catch(e) { cart = []; }

    var newItem;

    if (item.type === 'AI_DESIGN') {
        // AI-дизайн торта
        var existIdx = cart.findIndex(function(c) { return c.id === item.id; });
        if (existIdx !== -1) {
            // вже є в кошику — просто показуємо сповіщення
            showWishlistCartNotification(item.name);
            return;
        }
        newItem = {
            id:             'ai_' + Date.now(),
            type:           'AI_DESIGN',
            productId:      null,
            name:           item.name || 'ШІ-дизайн торта',
            imagePath:      item.imagePath,
            priceUnit:      item.priceUnit || 'грн/шт',
            unitPrice:      item.unitPrice || 0,
            discountPrice:  null,
            quantity:       1,
            unit:           'шт',
            urgency:        null,
            aiImageUrl:     item.imagePath,
            constructorImg: null,
            description:    item.description || null
        };

    } else if (item.type === 'CONSTRUCTOR') {
        // Конструктор торта
        newItem = {
            id:             'constructor_' + Date.now(),
            type:           'CONSTRUCTOR',
            productId:      null,
            name:           item.name || 'Авторський торт',
            imagePath:      item.imagePath,
            priceUnit:      item.priceUnit || 'грн',
            unitPrice:      item.unitPrice || 0,
            discountPrice:  null,
            quantity:       1,
            unit:           'шт',
            urgency:        null,
            aiImageUrl:     null,
            constructorImg: item.imagePath,
            description:    item.description || null
        };

    } else {
        // Звичайний товар з каталогу
        var cartId = 'product_' + item.productId;
        var existIdx = cart.findIndex(function(c) { return c.id === cartId; });
        if (existIdx !== -1) {
            cart[existIdx].quantity = Math.round((cart[existIdx].quantity + item.quantity) * 10) / 10;
            localStorage.setItem('cakeCart', JSON.stringify(cart));
            if (typeof updateCartBadge === 'function') updateCartBadge();
            showWishlistCartNotification(item.name);
            return;
        }
        newItem = {
            id:             cartId,
            type:           item.type,
            productId:      item.productId,
            name:           item.name,
            imagePath:      item.imagePath,
            priceUnit:      item.priceUnit,
            unitPrice:      item.unitPrice,
            discountPrice:  item.discountPrice || null,
            quantity:       item.quantity,
            unit:           item.unit,
            urgency:        item.urgency || null,
            aiImageUrl:     null,
            constructorImg: null,
            description:    null
        };
    }

    cart.push(newItem);
    localStorage.setItem('cakeCart', JSON.stringify(cart));
    if (typeof updateCartBadge === 'function') updateCartBadge();
    showWishlistCartNotification(item.name);
}

// ── Сповіщення про додавання в кошик ─────────────────────────
function showWishlistCartNotification(name) {
    var existing = document.getElementById('cartToast');
    if (existing) existing.remove();

    var toast = document.createElement('div');
    toast.id = 'cartToast';
    toast.innerHTML = '🛒 «' + esc(name) + '» додано в кошик';
    toast.style.cssText = [
        'position:fixed', 'bottom:30px', 'right:30px', 'z-index:9999',
        'background:#3AA6B9', 'color:white', 'padding:14px 24px',
        'border-radius:12px', 'font-family:Cormorant Garamond,serif',
        'font-size:18px', 'box-shadow:0 4px 20px rgba(0,0,0,0.15)',
        'transition:opacity 0.4s ease', 'opacity:1'
    ].join(';');

    document.body.appendChild(toast);
    setTimeout(function() { toast.style.opacity = '0'; }, 2500);
    setTimeout(function() { if (toast.parentNode) toast.remove(); }, 3000);
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