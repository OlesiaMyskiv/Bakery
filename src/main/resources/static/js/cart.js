/**
 * CAKE HOUSE — КОШИК
 * Зберігається в localStorage під ключем 'cakeCart'
 *
 * Структура елемента кошика:
 * {
 *   id:           string,   // унікальний ключ: "product_7" | "ai_design" | "constructor"
 *   type:         string,   // "FLAVOR" | "DESIGN" | "LINE" | "AI_DESIGN" | "CONSTRUCTOR"
 *   productId:    number|null,
 *   name:         string,
 *   imagePath:    string|null,
 *   priceUnit:    string,   // "грн/кг" | "грн/декор" | "грн/шт" | ...
 *   unitPrice:    number,
 *   discountPrice: number|null,  // ціна зі знижкою якщо є
 *   quantity:     number,   // кг або шт
 *   unit:         string,   // "кг" | "шт" | "декор" | ...
 *   urgency:      string|null, // "TODAY" | "HOURS_24" | "HOURS_48"
 *   aiImageUrl:   string|null,
 *   constructorImg: string|null,
 *   description:  string|null,
 * }
 */

const CART_KEY = 'cakeCart';

// ── Читання/запис ─────────────────────────────────────────────────────────────

function getCart() {
    try {
        return JSON.parse(localStorage.getItem(CART_KEY)) || [];
    } catch (e) {
        return [];
    }
}

function saveCart(cart) {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));
    updateCartBadge();
}

// ── Додавання товару ──────────────────────────────────────────────────────────

/**
 * Додає товар з каталогу в кошик.
 * @param {Object} item - дані товару
 */
function addToCart(item) {
    // Перевірка авторизації
    if (!window.IS_LOGGED_IN) {
        var modal = document.getElementById('auth-required-modal');
        if (modal) modal.style.display = 'flex';
        return;
    }
    const cart = getCart();
    const existingIdx = cart.findIndex(function(c) { return c.id === item.id; });

    if (existingIdx !== -1) {
        // Вже є — збільшуємо кількість
        cart[existingIdx].quantity = Math.round((cart[existingIdx].quantity + item.quantity) * 10) / 10;
    } else {
        cart.push(item);
    }

    saveCart(cart);
    showCartNotification(item.name);
}

/**
 * Додає товар з сторінки product-detail.
 * Викликається кнопкою "Додати в кошик".
 */
function addProductToCart(productId, name, type, imagePath, priceUnit, unitPrice, discountPrice, quantity, unit, urgency) {
    var id = 'product_' + productId;
    addToCart({
        id:            id,
        type:          type,
        productId:     productId,
        name:          name,
        imagePath:     imagePath,
        priceUnit:     priceUnit,
        unitPrice:     unitPrice,
        discountPrice: discountPrice || null,
        quantity:      quantity,
        unit:          unit,
        urgency:       urgency || null,
        aiImageUrl:    null,
        constructorImg: null,
        description:   null
    });
}

/**
 * Додає ШІ-дизайн в кошик.
 */
function addAiDesignToCart(imageUrl, description) {
    var id = 'ai_' + Date.now();
    addToCart({
        id:            id,
        type:          'AI_DESIGN',
        productId:     null,
        name:          'ШІ-дизайн торта',
        imagePath:     imageUrl,
        priceUnit:     'грн/шт',
        unitPrice:     0,
        discountPrice: null,
        quantity:      1,
        unit:          'шт',
        urgency:       null,
        aiImageUrl:    imageUrl,
        constructorImg: null,
        description:   description || null
    });
}

/**
 * Додає конструктор торта в кошик.
 */
function addConstructorToCart(constructorImg, description, price, guestsLabel) {
    var id = 'constructor_' + Date.now();
    addToCart({
        id:            id,
        type:          'CONSTRUCTOR',
        productId:     null,
        name:          'Авторський торт',
        imagePath:     constructorImg,
        priceUnit:     guestsLabel || 'грн',
        unitPrice:     price || 0,
        discountPrice: null,
        quantity:      1,
        unit:          'шт',
        urgency:       null,
        aiImageUrl:    null,
        constructorImg: constructorImg,
        description:   description || null
    });
}

// ── Видалення / оновлення ─────────────────────────────────────────────────────

function removeFromCart(id) {
    var cart = getCart().filter(function(c) { return c.id !== id; });
    saveCart(cart);
    renderCartPage();
}

function updateQty(id, newQty) {
    var cart = getCart();
    var idx = cart.findIndex(function(c) { return c.id === id; });
    if (idx !== -1) {
        var qty = Math.max(0.5, Math.round(newQty * 10) / 10);
        cart[idx].quantity = qty;
        saveCart(cart);
        renderCartPage();
    }
}

function clearCart() {
    saveCart([]);
}

// ── Підрахунки ────────────────────────────────────────────────────────────────

/**
 * Підраховує суму кошика.
 * Якщо користувач верифікований — використовує discountPrice де є.
 */
function calcCartTotal(cart, isVerified) {
    return cart.reduce(function(sum, item) {
        var price = (isVerified && item.discountPrice) ? item.discountPrice : item.unitPrice;
        return sum + price * item.quantity;
    }, 0);
}

/**
 * Визначає мінімальну дату доставки на основі терміновості товарів у кошику.
 * TODAY → сьогодні, HOURS_24 → завтра, HOURS_48 → через 2 дні
 */
function calcMinDeliveryDate(cart) {
    var daysToAdd = 0;
    cart.forEach(function(item) {
        if (item.urgency === 'HOURS_24' && daysToAdd < 1) daysToAdd = 1;
        if (item.urgency === 'HOURS_48' && daysToAdd < 2) daysToAdd = 2;
    });
    var date = new Date();
    date.setDate(date.getDate() + daysToAdd);
    return date;
}

/**
 * Перевіряє: є DESIGN без FLAVOR → повертає true (помилка)
 */
function hasDesignWithoutFlavor(cart) {
    var hasDesign = cart.some(function(c) {
        return c.type === 'DESIGN' || c.type === 'AI_DESIGN';
    });
    var hasFlavor = cart.some(function(c) { return c.type === 'FLAVOR'; });
    return hasDesign && !hasFlavor;
}

// ── Іконка кошика в хедері ────────────────────────────────────────────────────

function updateCartBadge() {
    var cart = getCart();
    var total = cart.reduce(function(s, c) { return s + (parseFloat(c.quantity) || 0); }, 0);
    var rounded = Math.round(total);
    // Оновлюємо всі badges на сторінці
    document.querySelectorAll('#cartBadge, .cart-badge').forEach(function(badge) {
        badge.textContent = rounded > 0 ? rounded : '';
        badge.style.display = rounded > 0 ? 'flex' : 'none';
    });
}

// ── Сповіщення ────────────────────────────────────────────────────────────────

function showCartNotification(name) {
    var existing = document.getElementById('cartToast');
    if (existing) existing.remove();

    var toast = document.createElement('div');
    toast.id = 'cartToast';
    toast.innerHTML = '🛒 «' + name + '» додано в кошик';
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

// ── Рендер сторінки кошика ────────────────────────────────────────────────────

function renderCartPage() {
    var container = document.getElementById('cartItemsContainer');
    if (!container) return;

    var cart    = getCart();
    var isVerified = window.IS_VERIFIED === true;

    if (cart.length === 0) {
        container.innerHTML = '<div class="cart-empty">🎂 Кошик порожній.<br><a href="/assortment" class="cart-empty-link">Перейти до асортименту</a></div>';
        updateSummary(0, 0);
        return;
    }

    // Групуємо по типу
    var groups = {
        FLAVOR:      { label: 'Смаки',           icon: '🎂', items: [] },
        DESIGN:      { label: 'Дизайни',          icon: '👑', items: [] },
        LINE:        { label: 'Лінійка виробів',  icon: '🕯', items: [] },
        AI_DESIGN:   { label: 'ШІ-дизайн',        icon: '🤖', items: [] },
        CONSTRUCTOR: { label: 'Конструктор',       icon: '',   items: [] }
    };

    cart.forEach(function(item) {
        if (groups[item.type]) groups[item.type].items.push(item);
        else groups['LINE'].items.push(item);
    });

    var html = '';

    // Попередження якщо є дизайн без смаку
    if (hasDesignWithoutFlavor(cart)) {
        html += '<div class="cart-warning">⚠️ До дизайну обов\'язково оберіть смак торта!</div>';
    }

    Object.values(groups).forEach(function(group) {
        if (group.items.length === 0) return;

        html += '<div class="cart-group">';
        var titleIcon = group.icon ? group.icon + ' ' : '';
        html += '<div class="cart-group-title">' + titleIcon + group.label + '</div>';

        group.items.forEach(function(item) {
            var price      = (isVerified && item.discountPrice) ? item.discountPrice : item.unitPrice;
            var subtotal   = Math.round(price * item.quantity);
            var stepVal    = item.unit === 'кг' ? '0.5' : '1';
            var minVal     = item.unit === 'кг' ? '0.5' : '1';
            var isKg       = item.unit === 'кг';

            html += '<div class="cart-item" data-id="' + item.id + '" data-type="' + item.type + '">';

            // Фото
            if (item.type === 'CONSTRUCTOR' && item.imagePath) {
                html += '<div class="cake-preview">';
                html += '<img src="' + item.imagePath + '" alt="' + item.name + '">';
                html += '</div>';
            } else {
                html += '<div class="cart-item-img">';
                if (item.imagePath) {
                    html += '<img src="' + item.imagePath + '" alt="' + item.name + '">';
                } else {
                    html += '<div class="cart-item-img-placeholder">🎂</div>';
                }
                html += '</div>';
            }

            // Інфо
            html += '<div class="cart-item-info">';
            html += '<div class="cart-item-name">' + item.name + '</div>';

            if (item.type === 'CONSTRUCTOR' && item.description) {
                var layers = item.description.split(' | ');
                html += '<ol class="constructor-layers-list">';
                layers.forEach(function(layer) {
                    html += '<li>' + layer + '</li>';
                });
                html += '</ol>';
            } else if (item.description) {
                html += '<div class="cart-item-desc">' + item.description + '</div>';
            }

            // Ціна
            html += '<div class="cart-item-price">';
            if (item.type === 'CONSTRUCTOR') {
                html += '<span class="constructor-meta">' + item.priceUnit + '</span>';
                html += '<span class="constructor-price">' + price.toLocaleString('uk-UA') + ' грн</span>';
            } else {
                html += '<span>' + price + ' ' + item.priceUnit + '</span>';
                if (isVerified && item.discountPrice) {
                    html += ' <span class="cart-defenders">🇺🇦 Захисникам</span>';
                }
            }
            html += '</div>';

            // Кількість
            html += '<div class="cart-item-qty">';
            html += '<button class="qty-btn" onclick="changeQty(\'' + item.id + '\',-' + stepVal + ')" type="button">−</button>';
            html += '<input class="qty-input" type="number" value="' + item.quantity + '" min="' + minVal + '" step="' + stepVal + '" onchange="updateQty(\'' + item.id + '\',parseFloat(this.value))">';
            html += '<span class="qty-unit">' + item.unit + '</span>';
            html += '<button class="qty-btn" onclick="changeQty(\'' + item.id + '\',' + stepVal + ')" type="button">+</button>';
            html += '</div>';

            html += '</div>'; // cart-item-info

            // Підсумок + видалити
            html += '<div class="cart-item-right">';
            html += '<div class="cart-item-subtotal" id="subtotal_' + item.id + '">' + subtotal + ' грн</div>';
            html += '<button class="cart-item-remove" onclick="removeFromCart(\'' + item.id + '\')" type="button" title="Видалити"><img src="/img/recycle-icon.svg" class="cart-remove-icon" alt="Видалити" style="width:20px;height:20px;opacity:0.45;"></button>';
            html += '</div>';

            html += '</div>'; // cart-item
        });

        html += '</div>'; // cart-group
    });

    container.innerHTML = html;

    // Оновлюємо суму
    var total = calcCartTotal(cart, isVerified);
    updateSummary(total, cart.length);
    updateMinDate(cart);
}

function changeQty(id, delta) {
    var cart = getCart();
    var idx = cart.findIndex(function(c) { return c.id === id; });
    if (idx !== -1) {
        var newQty = Math.max(0.5, Math.round((cart[idx].quantity + delta) * 10) / 10);
        cart[idx].quantity = newQty;
        saveCart(cart);
        renderCartPage();
    }
}

function updateSummary(total, count) {
    var el = document.getElementById('cartTotalBase');
    if (el) el.textContent = Math.round(total) + ' грн';

    var militaryCheck = document.getElementById('militaryCheckbox');
    var militaryAdd = militaryCheck && militaryCheck.checked ? 100 : 0;

    var finalEl = document.getElementById('cartTotalFinal');
    if (finalEl) finalEl.textContent = Math.round(total + militaryAdd) + ' грн';

    // Активність кнопки оформлення
    var btn = document.getElementById('checkoutBtn');
    if (btn) {
        var cart   = getCart();
        var hasErr = hasDesignWithoutFlavor(cart);
        btn.disabled  = count === 0 || hasErr;
        btn.title     = hasErr ? 'Оберіть смак до дизайну!' : '';
    }
}

function updateMinDate(cart) {
    var minDate = calcMinDeliveryDate(cart);
    var dateInput = document.getElementById('deliveryDate');
    if (dateInput) {
        var y = minDate.getFullYear();
        var m = String(minDate.getMonth() + 1).padStart(2, '0');
        var d = String(minDate.getDate()).padStart(2, '0');
        dateInput.min   = y + '-' + m + '-' + d;
        dateInput.value = y + '-' + m + '-' + d;
    }
}

// ── Оплата PayPal-заглушка ────────────────────────────────────────────────────

function openPaymentModal() {
    var modal = document.getElementById('paymentModal');
    if (modal) modal.classList.add('open');
}

function closePaymentModal() {
    var modal = document.getElementById('paymentModal');
    if (modal) modal.classList.remove('open');
}

function simulatePayment() {
    var btn = document.getElementById('paypalPayBtn');
    if (btn) {
        btn.textContent = '⏳ Обробка...';
        btn.disabled = true;
    }
    setTimeout(function() {
        // Закриваємо PayPal
        closePaymentModal();

        // Показуємо успіх
        var success = document.getElementById('paymentSuccess');
        if (success) success.classList.add('open');

        // Виставляємо оплату в формі
        var payField = document.getElementById('paymentStatusField');
        if (payField) payField.value = 'PAID';

        // Через 2 секунди автоматично submit форми
        setTimeout(function() {
            var form = document.getElementById('checkoutForm');
            if (form) form.submit();
        }, 2000);
    }, 1800);
}

// ── Бонуси ────────────────────────────────────────────────────────────────────

var userBonusBalance = 0;

function loadBonusBalance() {
    fetch('/api/bonus-balance', { credentials: 'include' })
        .then(function(r) { return r.ok ? r.json() : { balance: 0 }; })
        .then(function(data) {
            userBonusBalance = data.balance || 0;
            var block = document.getElementById('bonusUseBlock');
            var hint  = document.getElementById('bonusUseHint');
            if (block && userBonusBalance > 0) {
                block.style.display = 'block';
                if (hint) hint.textContent = 'У вас ' + userBonusBalance + ' бонусів';
            }
        })
        .catch(function() {});
}

function toggleBonusUse() {
    var cb = document.getElementById('bonusCheckbox');
    var cart = getCart();
    var isV  = window.IS_VERIFIED === true;
    var base = calcCartTotal(cart, isV);
    var milCheck = document.getElementById('militaryCheckbox');
    if (milCheck && milCheck.checked) base += 100;

    var discRow    = document.getElementById('bonusDiscountRow');
    var discAmount = document.getElementById('bonusDiscountAmount');
    var finalEl    = document.getElementById('cartTotalFinal');
    var useBonuses = document.getElementById('useBonusesField');

    if (cb && cb.checked) {
        var maxBonus  = Math.floor(base * 0.30); // 30% від суми
        var toSpend   = Math.min(userBonusBalance, maxBonus);
        if (discRow)    discRow.style.display = 'flex';
        if (discAmount) discAmount.textContent = '- ' + toSpend + ' грн';
        if (finalEl)    finalEl.textContent = Math.round(base - toSpend) + ' грн';
        if (useBonuses) useBonuses.value = toSpend;
        var amountEl = document.getElementById('bonusUseAmount');
        if (amountEl)   amountEl.textContent = '- ' + toSpend + ' грн';
    } else {
        if (discRow)    discRow.style.display = 'none';
        if (finalEl)    finalEl.textContent = Math.round(base) + ' грн';
        if (useBonuses) useBonuses.value = 0;
        var amountEl = document.getElementById('bonusUseAmount');
        if (amountEl)   amountEl.textContent = '';
    }
}

// ── Ініціалізація ─────────────────────────────────────────────────────────────

document.addEventListener('DOMContentLoaded', function() {
    // Якщо щойно оформили замовлення (є параметр ?ordered=1) — очищуємо кошик
    if (window.location.search.indexOf('ordered=1') !== -1 ||
        sessionStorage.getItem('justOrdered') === '1') {
        clearCart();
        sessionStorage.removeItem('justOrdered');
    }

    updateCartBadge();
    renderCartPage();
    loadBonusBalance();

    // Чекбокс солодощів для ЗСУ
    var milCheck = document.getElementById('militaryCheckbox');
    if (milCheck) {
        milCheck.addEventListener('change', function() {
            var cart  = getCart();
            var isV   = window.IS_VERIFIED === true;
            var total = calcCartTotal(cart, isV);
            updateSummary(total, cart.length);
        });
    }

    // Радіо доставки
    var deliveryRadios = document.querySelectorAll('input[name="deliveryType"]');
    var addressBlock   = document.getElementById('addressBlock');
    deliveryRadios.forEach(function(r) {
        r.addEventListener('change', function() {
            if (addressBlock) {
                addressBlock.style.display = this.value === 'DELIVERY' ? 'block' : 'none';
            }
        });
    });

});