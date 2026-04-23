const ingredients = {
    sponge: [
        { name: 'Брауні', image: '/img/constructor/sponges/brownie.png', color: '#4A2511', pricePerKg: 300 },
        { name: 'Дакуаз (горіховий)', image: '/img/constructor/sponges/dakuaz.png', color: '#D7CCC8', pricePerKg: 350 },
        { name: 'Класичний ванільний', image: '/img/constructor/sponges/vanilla.png', color: '#FFF3E0', pricePerKg: 200 },
        { name: 'Листкові коржі', image: '/img/constructor/sponges/puff.png', color: '#FFE0B2', pricePerKg: 220 },
        { name: 'Маковий бісквіт', image: '/img/constructor/sponges/poppy.png', color: '#5D4037', pricePerKg: 260 },
        { name: 'Медові коржі', image: '/img/constructor/sponges/honey.png', color: '#FFB300', pricePerKg: 230 },
        { name: 'Морквяний', image: '/img/constructor/sponges/carrot.png', color: '#E65100', pricePerKg: 270 },
        { name: 'Фісташковий', image: '/img/constructor/sponges/pistachio.png', color: '#AED581', pricePerKg: 400 },
        { name: 'Червоний оксамит', image: '/img/constructor/sponges/red_velvet.png', color: '#8B0000', pricePerKg: 320 },
        { name: 'Шифоновий бісквіт', image: '/img/constructor/sponges/chiffon.png', color: '#FFECB3', pricePerKg: 240 },
        { name: 'Шоколадний бісквіт', image: '/img/constructor/sponges/choco.png', color: '#3E2723', pricePerKg: 250 }
    ],
    cream: [
        { name: 'Білосніжний крем-сир', image: '/img/constructor/creams/cream_cheese.png', color: '#FFFFF0', pricePerKg: 280 },
        { name: 'Ганаш (білий шок.)', image: '/img/constructor/creams/ganache_white.png', color: '#FFF8E1', pricePerKg: 350 },
        { name: 'Ганаш (молочний шок.)', image: '/img/constructor/creams/ganache_milk.png', color: '#795548', pricePerKg: 350 },
        { name: 'Ганаш (чорний шок.)', image: '/img/constructor/creams/ganache_dark.png', color: '#3E2723', pricePerKg: 350 },
        { name: 'Заварний (Патісьєр)', image: '/img/constructor/creams/patissier.png', color: '#FFF59D', pricePerKg: 200 },
        { name: 'Карамельний крем', image: '/img/constructor/creams/caramel_cream.png', color: '#FFB74D', pricePerKg: 250 },
        { name: 'Крем-чиз на вершках', image: '/img/constructor/creams/cheese_cream.png', color: '#FAFAFA', pricePerKg: 300 },
        { name: 'Крем-чиз на маслі', image: '/img/constructor/creams/cheese_butter.png', color: '#FFFDE7', pricePerKg: 320 },
        { name: 'Сметанний крем', image: '/img/constructor/creams/sour_cream.png', color: '#FFFFFF', pricePerKg: 180 },
        { name: 'Фісташковий крем', image: '/img/constructor/creams/pistachio_cream.png', color: '#C5E1A5', pricePerKg: 450 }
    ],
    filling: [
        { name: 'Безе/Меренга', image: '/img/constructor/fillings/meringue.png', color: '#F5F5F5', pricePerKg: 200 },
        { name: 'Вишневе конфі', image: '/img/constructor/fillings/cherry.png', color: '#C2185B', pricePerKg: 250 },
        { name: 'Карамелізовані горіхи', image: '/img/constructor/fillings/caramel_nuts.png', color: '#8D6E63', pricePerKg: 380 },
        { name: 'Кокосовий прошарок', image: '/img/constructor/fillings/coconut.png', color: '#FFFFFF', pricePerKg: 280 },
        { name: 'Крустілант', image: '/img/constructor/fillings/crustilant.png', color: '#5D4037', pricePerKg: 350 },
        { name: 'Лимонний курд', image: '/img/constructor/fillings/lemon_curd.png', color: '#FFEE58', pricePerKg: 240 },
        { name: 'Малинове компоте', image: '/img/constructor/fillings/raspberry.png', color: '#D81B60', pricePerKg: 270 },
        { name: 'Манго-маракуя', image: '/img/constructor/fillings/mango_passion.png', color: '#FBC02D', pricePerKg: 350 },
        { name: 'Полуничне кюлі', image: '/img/constructor/fillings/strawberry.png', color: '#E53935', pricePerKg: 260 },
        { name: 'Праліне', image: '/img/constructor/fillings/praline.png', color: '#3E2723', pricePerKg: 380 },
        { name: 'Роялтін', image: '/img/constructor/fillings/royaltin.png', color: '#6D4C41', pricePerKg: 360 },
        { name: 'Солона карамель', image: '/img/constructor/fillings/salt_caramel.png', color: '#E65100', pricePerKg: 300 },
        { name: 'Солона карамель з горіхами', image: '/img/constructor/fillings/salt_caramel_nuts.png', color: '#BF360C', pricePerKg: 400 },
        { name: 'Чорничне желе', image: '/img/constructor/fillings/blueberry.png', color: '#4527A0', pricePerKg: 280 },
        { name: 'Яблука з корицею', image: '/img/constructor/fillings/apple_cinnamon.png', color: '#D84315', pricePerKg: 220 }
    ]
};

let currentCake = [
    { type: 'sponge', title: 'Бісквіт', selectedIndex: 0 },
    { type: 'cream', title: 'Крем', selectedIndex: 0 },
    { type: 'filling', title: 'Начинка', selectedIndex: 0 },
    { type: 'sponge', title: 'Бісквіт', selectedIndex: 0 }
];

let draggedLayerIndex = null;

function renderConstructor() {
    const visualStack = document.getElementById('cake-visual-stack');
    const descList = document.getElementById('description-list');

    visualStack.innerHTML = '';
    descList.innerHTML = '';

    currentCake.forEach((layer, index) => {
        const options = ingredients[layer.type];
        const selectedItem = options[layer.selectedIndex];

        let blockHeight = '40px';
        if (layer.type === 'sponge') blockHeight = '120px';
        if (layer.type === 'filling') blockHeight = '60px';

        let imgStyle = 'width: 100%; height: auto; display: block; margin: 0; padding: 0; border: none;';

        const visualHTML = `
            <div class="draggable-layer" draggable="true" ondragstart="handleDragStart(${index})" ondragover="handleDragOver(event)" ondrop="handleDrop(${index})">
                <div class="drag-handle" title="Потягніть">&#8597;</div>
                <div class="cake-layer-row">
                    <button class="arrow-btn" onclick="changeLayer(${index}, -1)">&#8592;</button>
                    
                    <div class="layer-image-container">
                        <img src="${selectedItem.image || ''}" alt="${selectedItem.name}" style="${imgStyle} display: ${selectedItem.image ? 'block' : 'none'};">
                        <div style="width: 100%; height: ${blockHeight}; background-color: ${selectedItem.color}; display: ${selectedItem.image ? 'none' : 'block'}; margin: 0; padding: 0;"></div>
                    </div>

                    <button class="arrow-btn" onclick="changeLayer(${index}, 1)">&#8594;</button>
                </div>
            </div>
        `;
        visualStack.insertAdjacentHTML('beforeend', visualHTML);

        const isFirst = index === 0;
        const isLast = index === currentCake.length - 1;

        const descHTML = `
            <li class="draggable-layer" draggable="true" ondragstart="handleDragStart(${index})" ondragover="handleDragOver(event)" ondrop="handleDrop(${index})">
                <div class="move-arrows">
                    <button class="triangle-btn" onclick="moveLayer(${index}, ${index - 1})" style="visibility: ${isFirst ? 'hidden' : 'visible'};">&#9650;</button>
                    <button class="triangle-btn" onclick="moveLayer(${index}, ${index + 1})" style="visibility: ${isLast ? 'hidden' : 'visible'};">&#9660;</button>
                </div>
                <div style="flex-grow: 1; margin-left: 10px;">
                    <span style="color: #ccc; font-size: 20px;">&#8862;</span> ${index + 1} ${layer.title}: <strong>${selectedItem.name}</strong>
                </div>
                <button class="delete-btn" onclick="removeLayer(${index})" title="Видалити шар">&times;</button>
            </li>
        `;
        descList.insertAdjacentHTML('beforeend', descHTML);
    });

    calculatePrice();
}

// Функція додавання шару (тепер вона ще й закриває вікно!)
function addNewLayer(type, title, selectedIndex) {
    const newLayer = { type: type, title: title, selectedIndex: selectedIndex };
    const insertIndex = currentCake.length > 1 ? currentCake.length - 1 : currentCake.length;
    currentCake.splice(insertIndex, 0, newLayer);
    closeIngredientsModal(); // Закриваємо попап
    renderConstructor();
}

// Функції для відкриття/закриття красивого вікна
function openIngredientsModal() { document.getElementById('ingredientsModal').style.display = 'flex'; }
function closeIngredientsModal() { document.getElementById('ingredientsModal').style.display = 'none'; }

// Будуємо 3 колонки у вікні
function buildModalMenu() {
    const colSponge = document.getElementById('modal-col-sponge');
    const colCream = document.getElementById('modal-col-cream');
    const colFilling = document.getElementById('modal-col-filling');

    // Очищаємо колонки (залишаємо тільки заголовки)
    colSponge.innerHTML = '<div class="ingredients-category-title">🎂 Бісквіти</div>';
    colCream.innerHTML = '<div class="ingredients-category-title">🍦 Креми</div>';
    colFilling.innerHTML = '<div class="ingredients-category-title">🍒 Начинки</div>';

    // Наповнюємо Бісквіти
    ingredients.sponge.forEach((item, index) => {
        const safeName = item.name.replace(/'/g, "\\'");
        colSponge.insertAdjacentHTML('beforeend', `<button class="ingredient-btn" onclick="addNewLayer('sponge', 'Бісквіт', ${index})">${item.name}</button>`);
    });

    // Наповнюємо Креми
    ingredients.cream.forEach((item, index) => {
        const safeName = item.name.replace(/'/g, "\\'");
        colCream.insertAdjacentHTML('beforeend', `<button class="ingredient-btn" onclick="addNewLayer('cream', 'Крем', ${index})">${item.name}</button>`);
    });

    // Наповнюємо Начинки
    ingredients.filling.forEach((item, index) => {
        const safeName = item.name.replace(/'/g, "\\'");
        colFilling.insertAdjacentHTML('beforeend', `<button class="ingredient-btn" onclick="addNewLayer('filling', 'Начинка', ${index})">${item.name}</button>`);
    });
}

function removeLayer(index) {
    if (currentCake.length <= 2) { alert("Неможливо видалити! Торт повинен мати хоча б 2 шари."); return; }
    currentCake.splice(index, 1);
    renderConstructor();
}

function handleDragStart(index) { draggedLayerIndex = index; }
function handleDragOver(event) { event.preventDefault(); }
function handleDrop(targetIndex) {
    if (draggedLayerIndex !== null && draggedLayerIndex !== targetIndex) moveLayer(draggedLayerIndex, targetIndex);
    draggedLayerIndex = null;
}

function moveLayer(fromIndex, toIndex) {
    if (toIndex < 0 || toIndex >= currentCake.length) return;
    const movedItem = currentCake.splice(fromIndex, 1)[0];
    currentCake.splice(toIndex, 0, movedItem);
    renderConstructor();
}

function changeLayer(layerIndex, direction) {
    const layer = currentCake[layerIndex];
    const optionsLength = ingredients[layer.type].length;
    layer.selectedIndex += direction;
    if (layer.selectedIndex < 0) layer.selectedIndex = optionsLength - 1;
    if (layer.selectedIndex >= optionsLength) layer.selectedIndex = 0;
    renderConstructor();
}

function calculatePrice() {
    const guests = parseInt(document.getElementById('guests-count').value) || 1;
    const weightKg = (guests * 0.25).toFixed(2);
    document.getElementById('recommended-weight').innerText = weightKg + ' кг';
    let totalBasePrice = 0;
    currentCake.forEach(layer => { totalBasePrice += ingredients[layer.type][layer.selectedIndex].pricePerKg; });
    document.getElementById('total-price').innerText = Math.round((totalBasePrice / currentCake.length) * weightKg).toLocaleString('uk-UA') + ' грн';
}

document.addEventListener('DOMContentLoaded', () => {
    renderConstructor();
    buildModalMenu(); // Запускаємо побудову вікна
});