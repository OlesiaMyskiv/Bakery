// src/main/resources/static/js/cart.js
// JavaScript для сторінки кошика

document.addEventListener('DOMContentLoaded', (event) => {
    // JavaScript для динамічного оновлення суми та показу/приховування полів адреси

    const quantityInputs = document.querySelectorAll('.quantity-input');
    const totalPriceDisplay = document.getElementById('total-price-display');
    // itemTotalElements шукаються всередині циклу оновлення суми

    // Приховуємо кнопку "Оновити" поруч з кількістю (вона не потрібна при AJAX)
    document.querySelectorAll('.quantity-form button[type="submit"]').forEach(button => {
        button.style.display = 'none';
    });


    // JavaScript для показу/приховування полів адреси - ВИКОРИСТОВУЄМО classList
    const deliveryMethodRadios = document.querySelectorAll('input[name="deliveryMethod"]');
    const addressFields = document.getElementById('address-fields');
    const cityInput = document.getElementById('city');
    const streetInput = document.getElementById('street');
    const houseInput = document.getElementById('house');

    function toggleAddressFields() {
        // Перевіряємо, чи елементи форми оформлення замовлення існують
        if (!addressFields || !cityInput || !streetInput || !houseInput || deliveryMethodRadios.length === 0) {
            // console.warn("Не всі елементи форми оформлення замовлення знайдені.");
            return; // Виходимо, якщо елементи не знайдені (наприклад, якщо користувач анонімний)
        }

        if (document.getElementById('delivery').checked) {
            addressFields.classList.remove('hidden'); // Видаляємо клас 'hidden', щоб показати
            // Робимо поля адреси обов'язковими при виборі доставки
            cityInput.required = true;
            streetInput.required = true;
            houseInput.required = true;
        } else {
            addressFields.classList.add('hidden'); // Додаємо клас 'hidden', щоб приховати
            // Робимо поля адреси необов'язковими при виборі самовивозу
            cityInput.required = false;
            streetInput.required = false;
            houseInput.required = false;
        }
    }

    // Додаємо обробник подій до радіо-кнопок (якщо вони існують)
    if (deliveryMethodRadios.length > 0) {
        deliveryMethodRadios.forEach(radio => {
            radio.addEventListener('change', toggleAddressFields);
        });
    }

    // Викликаємо функцію при завантаженні сторінки, щоб встановити початковий стан полів адреси
    // Перевіряємо, чи потрібні елементи існують перед викликом
    if (addressFields && deliveryMethodRadios.length > 0) {
        toggleAddressFields();
    }


    // ***** ЛОГІКА ДИНАМІЧНОГО ОНОВЛЕННЯ КІЛЬКОСТІ ТА СУМИ *****

    // Обробник події 'input' для МИТТЄВОГО ВІЗУАЛЬНОГО оновлення суми на стороні клієнта
    quantityInputs.forEach(input => {
        input.addEventListener('input', function() {
            const itemContainer = this.closest('.bg-white');
            const unitPriceElement = itemContainer.querySelector('.unit-price');
            const itemTotalElement = itemContainer.querySelector('.item-total');

            if (unitPriceElement && itemTotalElement) {
                const unitPrice = parseFloat(unitPriceElement.textContent.replace(',', '.')) || 0;
                const quantity = parseInt(this.value) || 0;
                const newItemTotal = quantity * unitPrice;
                itemTotalElement.textContent = newItemTotal.toFixed(2);

                // Перерахунок та оновлення ЗАГАЛЬНОЇ суми на стороні клієнта
                let newOverallTotal = 0;
                document.querySelectorAll('.item-total').forEach(totalElem => {
                    newOverallTotal += parseFloat(totalElem.textContent.replace(',', '.')) || 0;
                });
                if (totalPriceDisplay) { // Перевіряємо, чи елемент загальної суми існує
                    totalPriceDisplay.textContent = newOverallTotal.toFixed(2);
                }
            }
        });
    });

    // Обробник події 'blur' для ВІДПРАВКИ AJAX запиту на сервер для збереження оновленої кількості
    quantityInputs.forEach(input => {
        input.addEventListener('blur', function() { // Поле втратило фокус
            const form = this.closest('.quantity-form'); // Форма для цього елемента кошика
            const assortmentIdInput = form.querySelector('input[name="assortmentId"]');
            const quantity = parseInt(this.value) || 0;
            const assortmentId = assortmentIdInput ? assortmentIdInput.value : null;

            // Перевіряємо, чи отримали потрібні дані
            if (assortmentId && quantity >= 1) { // Перевіряємо, що кількість >= 1 перед відправкою
                // Отримуємо CSRF токен та ім'я заголовка з HTML (зазвичай з прихованого поля в головній формі)
                const csrfTokenInput = document.querySelector('input[name="_csrf"]'); // Зазвичай ім'я параметра _csrf
                const csrfToken = csrfTokenInput ? csrfTokenInput.value : null;
                const csrfHeader = 'X-CSRF-TOKEN'; // Стандартне ім'я заголовка для Spring Security CSRF

                if (!csrfToken) {
                    console.error("CSRF token not found. AJAX update cannot be sent.");
                    alert("Помилка безпеки: Не знайдено CSRF токен. Оновлення неможливе.");
                    return; // Припиняємо виконання, якщо токен відсутній
                }

                // Створюємо об'єкт FormData для відправки даних як form-urlencoded
                const formData = new FormData();
                formData.append('assortmentId', assortmentId);
                formData.append('quantity', quantity);

                // Використовуємо Fetch API для надсилання POST запиту
                fetch('/cart/update', {
                    method: 'POST',
                    headers: {
                        // 'Content-Type': 'application/x-www-form-urlencoded', // Fetch з FormData сам встановить правильний Content-Type
                        [csrfHeader]: csrfToken // Додаємо CSRF токен як заголовок
                    },
                    body: formData // Відправляємо дані форми
                })
                    .then(response => {
                        if (!response.ok) {
                            // Якщо відповідь сервера не OK (наприклад, 400, 401, 500)
                            console.error('Server responded with status: ' + response.status);
                            // Можна прочитати тіло відповіді для більш детальної інформації від сервера
                            return response.text().then(text => {
                                console.error('Server response body:', text);
                                alert('Помилка оновлення кошика. Спробуйте ще раз або перезавантажте сторінку.');
                                // optionally, revert the input value here
                                // const originalQuantity = itemTotalElement.getAttribute('data-original-quantity'); // Requires storing original quantity
                                // this.value = originalQuantity;
                                throw new Error('Server update failed'); // Кидаємо помилку, щоб потрапити в блок catch
                            });
                        }
                        return response.text(); // Сервер повертає загальну суму як текст
                    })
                    .then(newTotalText => {
                        // Цей блок виконується при УСПІШНІЙ відповіді сервера
                        console.log('Cart item quantity updated successfully.');
                        // Візуальне оновлення вже відбулося на подію 'input'.
                        // newTotalText містить нову загальну суму з сервера. Можна її використати,
                        // щоб остаточно оновити відображення загальної суми, хоча клієнтська логіка вже це зробила.
                        if (totalPriceDisplay) {
                            try {
                                const serverCalculatedTotal = parseFloat(newTotalText);
                                if (!isNaN(serverCalculatedTotal)) {
                                    // Оновлюємо загальну суму на випадок розбіжностей з клієнтською логікою
                                    // totalPriceDisplay.textContent = serverCalculatedTotal.toFixed(2);
                                }
                            } catch (e) {
                                console.warn("Failed to parse server total response:", e);
                            }
                        }
                    })
                    .catch(error => {
                        // Цей блок виконується при помилці Fetch або помилці, кинутій у попередньому .then
                        console.error('Fetch error during cart update:', error);
                        // alert('Сталася помилка при оновленні кошика.'); // Або більш конкретне повідомлення вже було показано в .then
                    });

            } else if (quantity < 1) {
                // Якщо користувач ввів 0 або від'ємне число, можна встановити 1
                this.value = 1;
                // І надіслати AJAX для оновлення на сервері з кількістю 1
                // (або викликати логіку відправки AJAX повторно з новим значенням)
                // Для простоти, можна просто викликати removeItem, якщо quantity === 0
                // Але краще дозволити мінімум 1 і не давати ставити 0
                console.warn("Quantity must be at least 1. Setting to 1.");
                // Відправити оновлення з кількістю 1 на сервер, якщо потрібно
                // (Це вже робиться завдяки newQuantity = quantity > 0 ? quantity : 1; у сервісі)
            }
        });
    });

    // ***** КІНЕЦЬ ЛОГІКИ ДИНАМІЧНОГО ОНОВЛЕННЯ *****

}); // Закриття DOMContentLoaded