# MOD_DEV.md — правила разработки Cumulative Cannons

Обязательны для любых изменений в этом репозитории. При конфликте с общими
практиками, примерами из интернета или памятью модели приоритет у этого файла.

## 1. Стек и версии

- Minecraft **1.21.1**, NeoForge **21.1.x** (в проекте `21.1.235`), Java **21**.
- Официальные маппинги **Mojang** (их использует ModDevGradle). Parchment
  `2024.11.17` только добавляет имена параметров и javadoc поверх Mojang.
- Запрещены Yarn, Intermediary, MCP и SRG-имена в коде, конфигах и примерах.
- Версии меняются только вместе: `gradle.properties` (`minecraft_version`,
  `neo_version`, `parchment_*`) → `build.gradle` → проверка сборки.
- Источник истины по API — декомпилированные исходники:
  `build/moddev/artifacts/neoforge-21.1.235-sources.jar`
  (ваниль + NeoForge, включая `net/minecraft/world/item/ItemStack.java`).
  Они уже сгенерированы; для ModDevGradle повторная синхронизация —
  `./gradlew neoForgeIdeSync`, а не `genSources`/`prepareRuns` (это задачи ForgeGradle).

## 2. Обязательная проверка API перед кодом

Перед написанием или изменением кода:

1. Найти класс/метод в `neoforge-21.1.235-sources.jar` или через MCP
   `minecraft-modding` (`inspect-minecraft`, `search-class-source`,
   `get-class-source`, `analyze-symbol`).
2. Сверить сигнатуру, пакет, сторону (client/server) и тип события.
3. Только после этого писать код. Не полагаться на память модели и примеры
   для 1.20.x/Forge.
4. После изменений выполнить `./gradlew compileJava`; для datagen —
   `./gradlew runData`; перед релизом — `./gradlew build`.

## 3. Регистрация контента

- Только `DeferredRegister` и его специализации:
  `DeferredRegister.create(...)`, `DeferredRegister.createBlocks(MOD_ID)`,
  `DeferredRegister.createItems(MOD_ID)`,
  `DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID)`.
- Регистрация — на **mod event bus**, который приходит в конструктор
  `@Mod`-класса. `FMLJavaModLoadingContext` в 1.21.1 не использовать.

```java
@Mod(CumulativeCannons.MOD_ID)
public final class CumulativeCannons {
    public CumulativeCannons(IEventBus modEventBus, ModContainer modContainer) {
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModDataComponents.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
    }
}
```

- Каждый реестр имеет свой `register(IEventBus bus)`; вызывать его из
  конструктора мода до любого обращения к содержимому.
- Использовать `DeferredHolder`/`DeferredBlock`/`DeferredItem`; прямые
  `Registry.register(...)` и строковые ID в игровой логике запрещены.
- ID: `lowercase_snake_case`, namespace — `cumulative_cannons`.
- `ResourceLocation` создаётся только через
  `ResourceLocation.fromNamespaceAndPath(MOD_ID, "path")` или
  `ResourceLocation.parse("namespace:path")`. Конструктор в 1.21.1 приватный.
- Творческие вкладки — через `BuildCreativeModeTabContentsEvent`, а не через
  удалённое поле `Item.Properties#tab`.

## 4. Предметы и данные: только Data Components

В 1.21.1 NBT-теги предмета удалены. Запрещено использовать:

- `ItemStack#getOrCreateTag()`, `ItemStack#getTag()`, `ItemStack#setTag()`;
- `CompoundTag` как хранилище состояния предмета;
- ручную запись/чтение NBT в обход `Codec`.

Правильно:

- Для ванильных данных — `DataComponents` (`CUSTOM_NAME`, `DAMAGE`,
  `MAX_STACK_SIZE`, `ENCHANTMENTS` и т.д.).
- Для собственных данных — зарегистрировать `DataComponentType`:

```java
public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, CumulativeCannons.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<JetData>> JET_DATA =
            DATA_COMPONENTS.registerComponentType("jet_data", builder -> builder
                    .persistent(JetData.CODEC)
                    .networkSynchronized(JetData.STREAM_CODEC));

    public static void register(IEventBus bus) {
        DATA_COMPONENTS.register(bus);
    }
}
```

- Компонент — неизменяемый `record` с `Codec<T>` и
  `StreamCodec<? super RegistryFriendlyByteBuf, T>`. Не хранить в компоненте
  `Level`, `Entity`, `ItemStack`, `BlockEntity` или другие живые объекты.
- Если данные нужны клиенту — `networkSynchronized(...)` обязателен.
  Только серверные данные всё равно должны иметь `persistent(...)`, если
  сохраняются на диск.
- Доступ к данным предмета только через `DataComponentHolder`:
  `stack.get(type)`, `stack.getOrDefault(type, def)`, `stack.has(type)`,
  `stack.set(type, value)`, `stack.remove(type)`.
- Не создавать компонент-дубликат уже существующего ванильного компонента.
- При портировании старого NBT-кода писать `Codec` с `optionalFieldOf`/
  `defaultValue` и предусматривать миграцию, а не читать `CompoundTag` вручную.

## 5. События

- NeoForge 1.21.1 сам определяет шину для `@EventBusSubscriber` по типу
  события (`IModBusEvent` → mod bus, остальные → game bus). Атрибут `bus`
  устарел — не указывать его.
- Для автоматической подписки:

```java
@EventBusSubscriber(modid = CumulativeCannons.MOD_ID)
public final class ModEvents {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // ...
    }
}
```

- Для ручной подписки: `modEventBus.addListener(this::onCommonSetup)` для
  lifecycle/registration/datagen и `NeoForge.EVENT_BUS.addListener(...)` для
  игровых событий.
- Методы `@SubscribeEvent` в `@EventBusSubscriber`-классах должны быть
  `public static`; клиентские классы помечать `value = Dist.CLIENT`.
- Запрещены `MinecraftForge.EVENT_BUS`, `@Mod.EventBusSubscriber`,
  `FMLJavaModLoadingContext` и старые Forge-шины.
- Отменять событие только при необходимости; после отмены не выполнять
  побочные эффекты, которые ожидались downstream-модами.

## 6. Data Generation

Все рецепты, теги, лут-таблицы, модели блоков/предметов, blockstate и lang
генерируются кодом. Ручные JSON в `src/main/resources/data/**` и
`src/main/resources/assets/**/models/**` запрещены (кроме намеренных
статических ресурсов, например текстур и `pack.mcmeta`).

- Точка входа — `GatherDataEvent` (mod bus):

```java
@EventBusSubscriber(modid = CumulativeCannons.MOD_ID)
public final class DataGenerators {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existing = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        generator.addProvider(event.includeServer(), new ModRecipeProvider(output, lookup));
        BlockTagsProvider blockTags = new ModBlockTagsProvider(
                output, lookup, CumulativeCannons.MOD_ID, existing);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ModItemTagsProvider(output, lookup, blockTags));
        generator.addProvider(event.includeServer(), new ModLootTableProvider(output, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(ModBlockLoot::new, LootContextParamSets.BLOCK)),
                lookup));
        generator.addProvider(event.includeClient(), new ModBlockStateProvider(
                output, CumulativeCannons.MOD_ID, existing));
        generator.addProvider(event.includeClient(), new ModItemModelProvider(
                output, CumulativeCannons.MOD_ID, existing));
    }
}
```

- Использовать провайдеры: `RecipeProvider`, NeoForge `BlockTagsProvider`,
  vanilla `ItemTagsProvider`, `LootTableProvider` + `BlockLootSubProvider`,
  `BlockStateProvider`, `ItemModelProvider`, `LanguageProvider`,
  `DatapackBuiltinEntriesProvider` для datapack-реестров.
- Учитывать `event.includeServer()` / `event.includeClient()`; не генерировать
  клиентские ресурсы в серверной части.
- Результат пишется в `src/generated/resources/` (подключён в `sourceSets`).
  Сгенерированные файлы не редактировать вручную — менять провайдер и
  запускать `./gradlew runData`.
- Рецепты и теги проверять на соответствие 1.21.1: `RecipeProvider` принимает
  `PackOutput` и `CompletableFuture<HolderLookup.Provider>`.

## 7. Сеть и стороны

- Сеть — только payload API NeoForge: `CustomPacketPayload`,
  `PayloadRegistrar` и `RegisterPayloadHandlersEvent` на mod bus.
  `SimpleChannel` и старый `NetworkRegistry` запрещены.
- Клиентский код не должен загружаться на dedicated server: отдельные классы,
  `Dist.CLIENT`, `@EventBusSubscriber(value = Dist.CLIENT, modid = ...)`.
- Общие данные синхронизировать через payload/Data Components, не через
  статические поля.

## 8. MCP и протокол памяти

### 8.1. Что и когда сохранять

Память проекта ведётся в двух слоях:

- **Obsidian** (`obsidian_memory`) — долговременные читаемые заметки и граф.
- **memory MCP** (`memory`) — короткие структурированные факты для агента.

Сохранять обязательно:

- архитектурные решения и причину выбора (не только «что», но и «почему»);
- ID блоков, предметов, сущностей, эффектов и партиклов, их назначение;
- проверенные сигнатуры API 1.21.1 и неочевидные ограничения;
- команды, которые реально сработали (сборка, datagen, MCP-обходы);
- изменения в MCP-конфигурации и путях доступа.

Не сохранять: токены и секреты, сырые логи, большие куски кода,
непроверенные догадки и то, что уже есть в ванильной документации.

Когда сохранять: после принятого архитектурного решения, после добавления
или переименования ID, после нетривиального фикса и в конце задачи.
Задача не считается завершённой, пока долговременное решение не записано
в Obsidian и memory MCP.

Перед архитектурной правкой: прочитать `Index.md` и профильные заметки,
чтобы не дублировать уже принятые решения.

### 8.2. Obsidian

- Доступ только к `/home/xpkotq/Obsidian/opencode-brain`.
  Папку `/home/xpkotq/Obsidian/Im` не читать и не изменять.
- Новое решение — отдельная компактная заметка; первая строка — теги
  (`#minecraft #neoforge #...`).
- Каждая заметка ссылается на `[[Index]]`, а `[[Index]]` — на неё.
  Связанные заметки связывать двусторонними `[[...]]`.
- Обновлять `Index.md`: список заметок, быстрые факты и стартовую структуру.
- Создавать и редактировать файлы через `obsidian_memory`
  (`read_text_file`, `write_file`, `edit_file`, `list_directory`,
  `directory_tree`, `search_files`).
- Существующие заметки: [[Minecraft_1.21.1_Stack]], [[DeferredRegister]],
  [[Data_Components_1.21.1]], [[NeoForge_Event_Bus]], [[Data_Generation]],
  [[Blocks_and_Items]], [[MCP_Toolchain]].
- `obsidian_memory` работает с `protocol: "2026-07-28"`. Не переключать на
  legacy: иначе MCP Roots подменит allowed directories на workspace.

### 8.3. memory MCP

- Сущность проекта — `cumulative_cannons` (тип `project`).
- После изменений вызывать `memory.add_observations` с форматом
  `registry: id — тип — назначение`.
- В memory MCP хранить только короткие факты и указатели; полные заметки —
  в Obsidian.

### 8.4. MCP-инструменты

- `minecraft-modding` — основной инструмент проверки API: `inspect-minecraft`,
  `search-class-source`, `get-class-source`, `analyze-symbol`,
  `validate-project`, `compare-minecraft`. Для этого проекта передавать
  `projectPath: /home/xpkotq/test/cumulative_cannons`; версия — 1.21.1.
- Если workspace-резолв ModDevGradle не срабатывает, читать локальный sources
  jar напрямую: `task: "file"` или `task: "search"` с
  `artifact: { type: "resolve-target", target: { kind: "jar", value: "/home/xpkotq/test/cumulative_cannons/build/moddev/artifacts/neoforge-21.1.235-sources.jar" } }`.
  Для MCP включён `NODE_OPTIONS=--dns-result-order=ipv4first` (иначе Node
  уходит в IPv6 и запросы к Mojang падают).
- `github` — поиск актуальных паттернов (`search_code`). Перед копированием
  проверять, что репозиторий действительно для 1.21.1/NeoForge (по
  `gradle.properties`/`build.gradle`), и соблюдать лицензию. Код без лицензии
  не копировать. `search_code` требует `GITHUB_TOKEN`; без него доступны
  только публичные чтения файлов/репозиториев.
- `memory` — граф фактов между сессиями; обновлять сущность
  `cumulative_cannons`.
- `obsidian_memory` — единственный путь записи долговременных заметок;
  см. раздел 8.2.

## 9. Текущие зарегистрированные ID

| Тип | ID |
| --- | --- |
| Block | `cumulative_shell`, `smoke_test_1`, `smoke_test_2`, `smoke_test_3` |
| Item | `cumulative_shell`, `smoke_test_1`, `smoke_test_2`, `smoke_test_3` |
| Block entity | `cumulative_shell` |
| Entity type | `cumulative_shell` |
| Mob effect | `bleeding` |
| Particle | `jet_flash`, `jet_shockwave`, `jet_dark_smoke`, `jet_smoke`, `jet_spall`, `jet_fragment` |

При добавлении новых ID обновлять эту таблицу, заметку `[[Blocks_and_Items]]`
в Obsidian и сущность `cumulative_cannons` в memory MCP.

## 10. Запрещено

- Писать код по памяти для API, которого нет в исходниках 1.21.1.
- Использовать `ItemStack#getOrCreateTag()` и предметный NBT.
- Создавать `ResourceLocation` через `new`.
- Регистрировать контент вне `DeferredRegister` или на game bus.
- Ручные JSON для рецептов, тегов, лута, моделей и blockstate.
- Смешивать клиентские классы с общим кодом.
- Обходить проверку `./gradlew compileJava` при сдаче изменений.
