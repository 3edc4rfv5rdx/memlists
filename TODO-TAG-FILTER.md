# Tag Cloud Filter — план реализации

## Шаги

### 1. MemosUiState — добавить selectedTags: Set<String>
### 2. MemosViewModel — setTagFilter(), clearTagFilter(), фильтрация AND
### 3. Repository — loadTagCloud(hiddenMode): Map<String, Int>
### 4. TagCloudScreen.kt — новый экран (FlowRow chips, 5 размеров, мульти-выбор, AppBar: Clear/Apply/Cancel)
### 5. Локализация i18n.json
### 6. Навигация MemListsApp.kt — route + wiring через savedStateHandle
### 7. MemosHomeScreen — подключить меню, индикатор (All)/(T), фильтрация visibleItems

## Порядок: 1 → 2 → 3 → 4 → 5 → 6 → 7
