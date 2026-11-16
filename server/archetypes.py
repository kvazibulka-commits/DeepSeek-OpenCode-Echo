"""Архетипы ЭХО: образ рождается из смысла трека, а не из случайного хеша.

Алгоритм:

1. Название, альбом и артист токенизируются и ищутся в словаре мотивов
   (север, ночной город, море, космос, огонь, дождь, машина, храм, дом…).
   Поле ``title`` весит больше, чем ``album``, а ``artist`` — меньше всего.
2. Жанр из плеера или каталога добавляет мотиву сильный бонус.
3. Тон мотива (epic / urban / quiet / cosmic / warm / ritual / road) и
   акустика трека (энергия × настроение) выбирают роль.
4. Заголовок = роль + образ в родительном падеже: «Скальд драккара»,
   «Штурман панельных дворов», «Хранитель северного ветра».

Один и тот же трек всегда даёт один и тот же архетип, а разные треки из
одного мотива получают разные роли и образы.
"""

from __future__ import annotations

try:
    from .genres import genre_label
    from .resonance import Acoustics, seed_int
except ImportError:
    from genres import genre_label
    from resonance import Acoustics, seed_int

# ── Мотивы ───────────────────────────────────────────────────────────────────
# keywords — устойчивые основы слов (русские и английские).
# images — образы в родительном падеже, чтобы сочетаться с любой ролью.

MOTIFS: tuple[dict, ...] = (
    {
        "id": "nordic",
        "tone": "epic",
        "genres": ("nordic folk", "dark folk", "folk", "metal"),
        "keywords": (
            "драккар", "drakkar", "викинг", "viking", "скандин", "norse", "nord",
            "валгалл", "valhalla", "фьорд", "fjord", "рун", "rune", "скальд", "skald",
            "сага", "saga", "берсерк", "berserk", "ярл", "jarl", "тролл", "troll",
            "один", "odin", "валькир", "valkyr", "danheim", "wardruna", "heilung",
            "север", "north", "ярн", "jarn", "дракон", "dragon",
        ),
        "images": (
            "драккара", "северного ветра", "рунных камней", "фьорда",
            "зимних пастбищ", "валгаллы", "солёного берега",
        ),
        "observations": (
            "Слышит в гуле вёсел тот же ритм, что и в любимом треке.",
            "Идёт по северному берегу так, будто каждый шаг — удар в щит.",
        ),
        "palette": (0xFF34D399, 0xFF38BDF8, 0xFFFFD700),
    },
    {
        "id": "nightcity",
        "tone": "urban",
        "genres": ("post-punk", "darkwave", "indie", "hip-hop"),
        "keywords": (
            "панел", "двор", "ноч", "фонар", "город", "улиц", "подъезд", "асфальт",
            "троллейбус", "трамва", "бетон", "neon", "неон", "city", "street",
            "night", "panel", "block", "molchat", "молчат", "ploho", "плохо",
            "buerak", "буерак", "motorama", "nurnberg", "kino", "кино",
            "joy division", "cure", "coldwave",
        ),
        "images": (
            "панельных дворов", "ночных фонарей", "мокрого асфальта",
            "пустых подъездов", "трамвайных рельсов", "последнего троллейбуса",
        ),
        "observations": (
            "Слушает город на частоте гудящих фонарей.",
            "Знает, как звучит пустой двор после полуночи.",
        ),
        "palette": (0xFF38BDF8, 0xFFA855F7, 0xFF00F0FF),
    },
    {
        "id": "space",
        "tone": "cosmic",
        "genres": ("ambient", "electronic", "soundtrack", "synthwave"),
        "keywords": (
            "космос", "звезд", "звёзд", "орбит", "спутник", "space", "cosmos",
            "orbit", "satellite", "astronaut", "космонавт", "луна", "moon",
            "планет", "planet", "галактик", "galaxy", "sputnik", "невесом",
        ),
        "images": (
            "дальней орбиты", "погасших спутников", "лунной дорожки",
            "чужой планеты", "звёздной пыли", "ночного неба",
        ),
        "observations": (
            "Смотрит на панельки как на орбитальные станции.",
            "Держит в плеере вакуумную тишину между треками.",
        ),
        "palette": (0xFF00F0FF, 0xFFA855F7, 0xFF38BDF8),
    },
    {
        "id": "sea",
        "tone": "quiet",
        "genres": ("ambient", "post-rock", "indie", "classical"),
        "keywords": (
            "мор", "океан", "волн", "берег", "прибой", "sea", "ocean", "wave",
            "tide", "harbour", "гаван", "парус", "sail", "ship", "корабл",
            "lighthouse", "маяк", "шторм", "storm",
        ),
        "images": (
            "ночного прибоя", "солёного ветра", "дальнего маяка",
            "тихой гавани", "морской пены", "пустого причала",
        ),
        "observations": (
            "Оставляет музыку на скамейках у воды и не ждёт ответа.",
            "Считает волны вместо тактов.",
        ),
        "palette": (0xFF38BDF8, 0xFF34D399, 0xFF00F0FF),
    },
    {
        "id": "forest",
        "tone": "quiet",
        "genres": ("folk", "nordic folk", "dark folk", "ambient", "post-rock"),
        "keywords": (
            "лес", "дерев", "тайг", "wood", "forest", "роща", "река", "river",
            "озер", "lake", "болот", "мох", "moss", "листв", "хвоя", "pine",
            "ветв", "branch",
        ),
        "images": (
            "северного леса", "мшистых камней", "тихой реки",
            "лесной тропы", "хвойного дыма", "старой рощи",
        ),
        "observations": (
            "Слышит в шуме деревьев тот же ритм, что и в любимом треке.",
            "Идёт без фонаря: лес сам подсказывает дорогу.",
        ),
        "palette": (0xFF34D399, 0xFFA855F7, 0xFF38BDF8),
    },
    {
        "id": "winter",
        "tone": "quiet",
        "genres": ("ambient", "classical", "dark folk", "nordic folk"),
        "keywords": (
            "зим", "снег", "холод", "лёд", "лед", "мороз", "frost", "winter",
            "snow", "ice", "cold", "blizzard", "вьюг", "метел", "метёл", "иней",
        ),
        "images": (
            "зимнего безмолвия", "инея на стекле", "долгой метели",
            "белого поля", "мёрзлой земли", "первого снега",
        ),
        "observations": (
            "Слушает тишину между снежинками.",
            "Дышит в такт морозному воздуху.",
        ),
        "palette": (0xFF38BDF8, 0xFF00F0FF, 0xFFA855F7),
    },
    {
        "id": "rain",
        "tone": "urban",
        "genres": ("trip-hop", "indie", "lo-fi", "post-punk"),
        "keywords": (
            "дожд", "осен", "груст", "печал", "слёз", "слез", "rain", "autumn",
            "sad", "tear", "melanchol", "тоск", "луж", "туман", "fog", "mist",
            "cloud", "облак",
        ),
        "images": (
            "осеннего дождя", "мокрых витрин", "серого утра",
            "чужого зонта", "луж под фонарями", "поздней осени",
        ),
        "observations": (
            "Знает, как звучит дождь по жестяному карнизу.",
            "Собирает чужие зонты на остановке.",
        ),
        "palette": (0xFF38BDF8, 0xFF64748B, 0xFFA855F7),
    },
    {
        "id": "road",
        "tone": "road",
        "genres": ("rock", "indie", "post-rock", "folk"),
        "keywords": (
            "дорог", "пустын", "трасс", "road", "desert", "highway", "journey",
            "путь", "way", "wander", "странств", "поезд", "train", "вокзал",
            "station", "перрон", "шоссе", "dune", "дюн", "кочев",
        ),
        "images": (
            "ночной трассы", "пустого перрона", "дальнего поезда",
            "пыльной дороги", "чужого вокзала", "рассветного шоссе",
        ),
        "observations": (
            "Меряет вечер не минутами, а километрами.",
            "Любит музыку, которая звучит как последний поезд.",
        ),
        "palette": (0xFFFFB800, 0xFFF87171, 0xFF38BDF8),
    },
    {
        "id": "fire",
        "tone": "ritual",
        "genres": ("dark folk", "nordic folk", "metal", "folk"),
        "keywords": (
            "огн", "пламя", "костёр", "костер", "fire", "flame", "burn", "ritual",
            "ритуал", "алтар", "altar", "пепел", "ash", "жертв", "кост", "bone",
            "факел", "torch", "погреб",
        ),
        "images": (
            "ночного костра", "старого алтаря", "тлеющих углей",
            "огненной реки", "холодного пепла", "погребального огня",
        ),
        "observations": (
            "Слушает треск огня громче, чем слова.",
            "Согревает руки о чужие мелодии.",
        ),
        "palette": (0xFFFFB800, 0xFFF87171, 0xFFFFD700),
    },
    {
        "id": "machine",
        "tone": "urban",
        "genres": ("industrial", "electronic", "metal", "rock"),
        "keywords": (
            "машин", "завод", "металл", "сталь", "industrial", "machine", "factory",
            "techno", "steel", "robot", "робот", "механик", "шел", "турбин", "шестер",
            "синтетик", "synthetic", "бетон",
        ),
        "images": (
            "заводских турбин", "стального шума", "ночной смены",
            "ржавых механизмов", "холодных цехов", "гула конвейера",
        ),
        "observations": (
            "Слышит ритм в работе станков.",
            "Считает удары пресса вместо тактов.",
        ),
        "palette": (0xFF64748B, 0xFF38BDF8, 0xFFF87171),
    },
    {
        "id": "dream",
        "tone": "quiet",
        "genres": ("ambient", "dream-pop", "downtempo", "trip-hop", "lo-fi"),
        "keywords": (
            "сон", "сновид", "дрем", "dream", "sleep", "полудрём", "дым",
            "smoke", "vapor", "пар", "нежн", "tender", "мягк", "soft", "плавн",
            "drift", "плыв", "грёз", "grez",
        ),
        "images": (
            "утреннего тумана", "сонных облаков", "дымных комнат",
            "полудрёмы", "мягкого света", "тихих сновидений",
        ),
        "observations": (
            "Засыпает под музыку и просыпается в ней же.",
            "Держит вечер на мягком свете.",
        ),
        "palette": (0xFFA855F7, 0xFFC084FC, 0xFF38BDF8),
    },
    {
        "id": "love",
        "tone": "warm",
        "genres": ("dream-pop", "pop", "soul", "indie", "jazz"),
        "keywords": (
            "любов", "сердц", "нежн", "heart", "love", "kiss", "объят", "поцелу",
            "влюбл", "girl", "boy", "девушк", "парн", "свидан", "ты и я",
        ),
        "images": (
            "тёплых ладоней", "позднего свидания", "тихой нежности",
            "чужого письма", "забытого танца", "утреннего света",
        ),
        "observations": (
            "Слушает так, будто рядом кто-то дышит.",
            "Помнит не трек, а момент, в котором он играл.",
        ),
        "palette": (0xFFF87171, 0xFFC084FC, 0xFFFFB800),
    },
    {
        "id": "club",
        "tone": "warm",
        "genres": ("disco", "funk", "electronic", "pop", "hip-hop"),
        "keywords": (
            "танц", "клуб", "disco", "dance", "club", "house", "groove", "funk",
            "boogie", "диско", "вечерин", "party", "floor", "бас", "bass",
        ),
        "images": (
            "ночного танцпола", "зеркального шара", "чужих ритмов",
            "танцующей толпы", "поздней дискотеки", "тёплого бара",
        ),
        "observations": (
            "Ловит ритм раньше, чем включается свет.",
            "Танцует так, будто никто не смотрит.",
        ),
        "palette": (0xFFFFB800, 0xFFF87171, 0xFF00F0FF),
    },
    {
        "id": "jazz",
        "tone": "warm",
        "genres": ("jazz", "soul", "downtempo", "trip-hop"),
        "keywords": (
            "джаз", "саксофон", "jazz", "sax", "lounge", "whisky", "виски", "бар",
            "bar", "blues", "блюз", "piano", "рояль", "сигар", "cigar",
        ),
        "images": (
            "прокуренного бара", "позднего саксофона", "льда в стакане",
            "тихого джаза", "ночного клуба", "жёлтого света лампы",
        ),
        "observations": (
            "Превращает случайный шум в тему вечера.",
            "Знает бар, где играют только вечером.",
        ),
        "roles": {
            "dark_high": ("Пианист", "Ловец", "Заводила", "Штурман"),
            "dark_low": ("Бармен", "Свидетель", "Хранитель", "Молчальник"),
            "light_high": ("Пианист", "Запевала", "Заводила", "Скоморох"),
            "light_low": ("Созерцатель", "Мечтатель", "Собиратель", "Сказитель"),
        },
        "palette": (0xFFFFB800, 0xFFF87171, 0xFFC084FC),
    },
    {
        "id": "folk",
        "tone": "ritual",
        "genres": ("folk", "dark folk", "nordic folk", "classical"),
        "keywords": (
            "деревн", "поле", "обряд", "folk", "village", "field", "harvest",
            "языч", "оберег", "сказан", "былин", "солнцестоя", "жатв", "праздник",
            "хоровод", "гусл", "гусл", "волынк",
        ),
        "images": (
            "деревенских полей", "старых обрядов", "летнего солнцестояния",
            "ржаных полей", "бабушкиных сказок", "полевых трав",
        ),
        "observations": (
            "Помнит песни, которых нет в интернете.",
            "Слышит в поле тот же ритм, что и в наушниках.",
        ),
        "palette": (0xFF34D399, 0xFFFFB800, 0xFFF87171),
    },
    {
        "id": "war",
        "tone": "epic",
        "genres": ("metal", "rock", "industrial"),
        "keywords": (
            "войн", "бой", "battle", "war", "fight", "оруж", "sword", "меч",
            "кров", "blood", "армия", "army", "битв", "копь", "spear", "осад",
        ),
        "images": (
            "зимней битвы", "ржавого меча", "забытых знамён",
            "дымного поля", "чужой войны", "разбитого щита",
        ),
        "observations": (
            "Держит ритм крепче, чем оружие.",
            "Слышит в гуле толпы далёкий бой.",
        ),
        "palette": (0xFFF87171, 0xFF64748B, 0xFFFFB800),
    },
    {
        "id": "retro",
        "tone": "cosmic",
        "genres": ("synthwave", "electronic", "pop", "disco"),
        "keywords": (
            "ретро", "кассет", "винил", "retro", "cassette", "vinyl", "synthwave",
            "80", "восьмидесят", "магнитофон", "tape", "neon", "неон", "аркад",
            "arcade", "дискет",
        ),
        "images": (
            "старых кассет", "ночного неона", "восьмидесятых",
            "виниловых тресков", "далёких огней", "магнитной ленты",
        ),
        "observations": (
            "Коллекционирует не пластинки, а моменты, когда игла находит канавку.",
            "Перематывает вечер кассетным карандашом.",
        ),
        "palette": (0xFF00F0FF, 0xFFC084FC, 0xFFFFB800),
    },
    {
        "id": "temple",
        "tone": "ritual",
        "genres": ("classical", "ambient", "soul", "dark folk"),
        "keywords": (
            "храм", "молитв", "свят", "church", "temple", "pray", "sacred",
            "angel", "archangel", "архангел", "хор", "choir", "госпел", "gospel",
            "орган", "свеч", "candle", "икон", "литург",
        ),
        "images": (
            "пустого храма", "тихой молитвы", "хора в темноте",
            "свечного света", "сводов собора", "старой иконы",
        ),
        "observations": (
            "Слушает тишину так же внимательно, как и музыку.",
            "Знает, как звучит эхо под куполом.",
        ),
        "roles": {
            "dark_high": ("Звонарь", "Певец", "Дозорный", "Хранитель"),
            "dark_low": ("Смотритель", "Молчальник", "Свидетель", "Хранитель"),
            "light_high": ("Певец", "Звонарь", "Запевала", "Вожатый"),
            "light_low": ("Смотритель", "Сказитель", "Собиратель", "Мечтатель"),
        },
        "palette": (0xFFFFD700, 0xFFA855F7, 0xFF38BDF8),
    },
    {
        "id": "home",
        "tone": "warm",
        "genres": ("lo-fi", "indie", "downtempo", "pop"),
        "keywords": (
            "дом", "детств", "мам", "home", "childhood", "семья", "family",
            "кухн", "kitchen", "окно", "window", "чай", "tea", "уют", "cozy",
            "комнат", "room",
        ),
        "images": (
            "тёплой кухни", "детских окон", "старого дома",
            "вечернего чая", "родного двора", "жёлтой лампы",
        ),
        "observations": (
            "Прячет любимые треки между страницами старых книг.",
            "Слушает музыку так, как другие пьют чай.",
        ),
        "palette": (0xFFFFB800, 0xFFF87171, 0xFF34D399),
    },
    {
        "id": "mythic",
        "tone": "epic",
        "genres": ("dark folk", "nordic folk", "metal", "folk", "ambient"),
        "keywords": (
            "волк", "wolf", "ворон", "raven", "змей", "зме", "snake", "дракон",
            "dragon", "звер", "beast", "дух", "spirit", "призрак", "ghost",
            "легенд", "legend", "миф", "myth", "ведьм", "witch",
        ),
        "images": (
            "серого волка", "ночного ворона", "древнего змея",
            "лесного зверя", "забытого духа", "старой легенды",
        ),
        "observations": (
            "Слышит шаги там, где остальные слышат ветер.",
            "Знает сказки, которые рассказывают только ночью.",
        ),
        "palette": (0xFFA855F7, 0xFF34D399, 0xFF64748B),
    },
    {
        "id": "digital",
        "tone": "cosmic",
        "genres": ("electronic", "industrial", "hip-hop", "synthwave"),
        "keywords": (
            "цифр", "интернет", "кибер", "cyber", "digital", "pixel", "пиксел",
            "screen", "экран", "network", "сет", "virtual", "виртуал", "код",
            "code", "данн", "data", "сигнал", "signal",
        ),
        "images": (
            "цифрового шума", "экранного света", "сетевых огней",
            "виртуальных комнат", "битых пикселей", "ночного эфира",
        ),
        "observations": (
            "Слышит музыку в шуме вентиляторов.",
            "Живёт в эфире и считает это домом.",
        ),
        "palette": (0xFF00F0FF, 0xFF38BDF8, 0xFFA855F7),
    },
)

# ── Роли ─────────────────────────────────────────────────────────────────────
# quadrant = (настроение: light/dark) + (энергия: high/low)

ROLES: dict[str, dict[str, tuple[str, ...]]] = {
    "epic": {
        "dark_high": ("Воевода", "Кормчий", "Гонец", "Дозорный"),
        "dark_low": ("Хранитель", "Скальд", "Молчальник", "Свидетель"),
        "light_high": ("Вожатый", "Запевала", "Гонец", "Скоморох"),
        "light_low": ("Сказитель", "Пастух", "Собиратель", "Мечтатель"),
    },
    "urban": {
        "dark_high": ("Штурман", "Гонец", "Дозорный", "Ловец"),
        "dark_low": ("Сторож", "Проводник", "Свидетель", "Молчальник"),
        "light_high": ("Заводила", "Вожатый", "Гонец", "Штурман"),
        "light_low": ("Картограф", "Собиратель", "Мечтатель", "Прохожий"),
    },
    "quiet": {
        "dark_high": ("Проводник", "Дозорный", "Ловец", "Кормчий"),
        "dark_low": ("Смотритель", "Хранитель", "Молчальник", "Свидетель"),
        "light_high": ("Вожатый", "Запевала", "Гонец", "Пастух"),
        "light_low": ("Созерцатель", "Собиратель", "Мечтатель", "Сказитель"),
    },
    "cosmic": {
        "dark_high": ("Штурман", "Гонец", "Ловец", "Дозорный"),
        "dark_low": ("Смотритель", "Хранитель", "Молчальник", "Проводник"),
        "light_high": ("Вожатый", "Запевала", "Штурман", "Гонец"),
        "light_low": ("Созерцатель", "Мечтатель", "Картограф", "Собиратель"),
    },
    "warm": {
        "dark_high": ("Заводила", "Ловец", "Гонец", "Штурман"),
        "dark_low": ("Хранитель", "Свидетель", "Собиратель", "Сторож"),
        "light_high": ("Запевала", "Заводила", "Вожатый", "Скоморох"),
        "light_low": ("Мечтатель", "Собиратель", "Сказитель", "Созерцатель"),
    },
    "ritual": {
        "dark_high": ("Воевода", "Гонец", "Дозорный", "Кормчий"),
        "dark_low": ("Хранитель", "Скальд", "Молчальник", "Свидетель"),
        "light_high": ("Запевала", "Вожатый", "Гонец", "Скоморох"),
        "light_low": ("Сказитель", "Собиратель", "Мечтатель", "Пастух"),
    },
    "road": {
        "dark_high": ("Гонец", "Кормчий", "Ловец", "Дозорный"),
        "dark_low": ("Проводник", "Свидетель", "Молчальник", "Смотритель"),
        "light_high": ("Вожатый", "Заводила", "Гонец", "Штурман"),
        "light_low": ("Странник", "Собиратель", "Мечтатель", "Сказитель"),
    },
}

# Жанр → мотив по умолчанию, когда в тексте нет зацепок.
GENRE_MOTIFS: dict[str, str] = {
    "post-punk": "nightcity",
    "darkwave": "nightcity",
    "synthwave": "retro",
    "ambient": "dream",
    "electronic": "digital",
    "indie": "nightcity",
    "rock": "road",
    "metal": "war",
    "jazz": "jazz",
    "soul": "temple",
    "lo-fi": "home",
    "classical": "temple",
    "trip-hop": "rain",
    "folk": "folk",
    "nordic folk": "nordic",
    "dark folk": "mythic",
    "dream-pop": "dream",
    "post-rock": "sea",
    "downtempo": "dream",
    "hip-hop": "nightcity",
    "pop": "club",
    "funk": "club",
    "disco": "club",
    "industrial": "machine",
    "soundtrack": "space",
    "demo": "nightcity",
}

MOTIF_BY_ID = {motif["id"]: motif for motif in MOTIFS}


def _words(text: str) -> str:
    probe = (text or "").lower().replace("ё", "е")
    return " ".join(probe.split())


def motif_score(motif: dict, artist: str, title: str, album: str, genre: str) -> tuple[float, float]:
    """Возвращает (общий балл, балл по словам трека)."""
    fields = (
        (_words(title), 3.0),
        (_words(album), 2.0),
        (_words(artist), 1.0),
    )
    text_score = 0.0
    for keyword in motif["keywords"]:
        needle = keyword.lower().replace("ё", "е")
        for text, weight in fields:
            if needle and needle in text:
                text_score += weight * (1.0 + min(len(needle), 12) / 12.0)
    genre_bonus = 5.0 if genre and genre in motif["genres"] else 0.0
    return text_score + genre_bonus, text_score


def pick_motif(artist: str, title: str, album: str, genre: str) -> dict:
    """Выбирает мотив по смыслу, затем по жанру, затем по хешу."""
    ranked = sorted(
        (
            (motif_score(motif, artist, title, album, genre), motif["id"])
            for motif in MOTIFS
        ),
        reverse=True,
    )
    (_, text_score), best_id = ranked[0]
    if text_score > 0:
        return MOTIF_BY_ID[best_id]

    fallback = GENRE_MOTIFS.get(genre or "")
    if fallback:
        return MOTIF_BY_ID[fallback]

    seed = seed_int(artist, title, album)
    soft = ("nightcity", "dream", "road", "sea")
    return MOTIF_BY_ID[soft[seed % len(soft)]]


def archetype_for(
    artist: str,
    title: str,
    album: str = "",
    genre: str = "",
    label: str = "",
    acoustics: Acoustics | None = None,
) -> dict:
    """Возвращает архетип трека: заголовок, наблюдение, цвет, лепестки."""
    motif = pick_motif(artist, title, album, genre)
    seed = seed_int(artist, title, album)

    energy = acoustics.energy if acoustics else 0.55
    valence = acoustics.valence if acoustics else 0.50
    quadrant = ("light" if valence >= 0.5 else "dark") + (
        "_high" if energy >= 0.5 else "_low"
    )
    roles = motif.get("roles") or ROLES.get(motif["tone"], ROLES["quiet"])
    role = roles[quadrant][(seed >> 3) % len(roles[quadrant])]
    image = motif["images"][(seed >> 11) % len(motif["images"])]
    observation = motif["observations"][(seed >> 19) % len(motif["observations"])]
    color = motif["palette"][(seed >> 27) % len(motif["palette"])]
    petals = 4 + (seed >> 33) % 5

    return {
        "personaTitle": f"{role} {image}",
        "moodLabel": label or genre_label(genre),
        "personaObservation": observation,
        "colorHex": color,
        "avatarPetals": petals,
        "motif": motif["id"],
    }


__all__ = ["MOTIFS", "MOTIF_BY_ID", "ROLES", "archetype_for", "motif_score", "pick_motif"]
