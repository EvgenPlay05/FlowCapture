package com.example.settings

object LiveTranslator {
    
    // Available Languages
    val LANGUAGES = listOf("English", "Українська (Beta) 🇺🇦", "Русский (Beta) 🇷🇺")

    private val translations = mapOf(
        "English" to mapOf(
            "tab_capture" to "Capture",
            "tab_gallery" to "Gallery",
            "tab_settings" to "Settings",
            "tab_about" to "About",
            
            "app_version" to "v1.0.1 Unstable Beta",
            "app_subtitle" to "Capture screen, games and annotations in style",
            "active_profile" to "ACTIVE PROFILE",
            "hd_details" to "Details density",
            "standby" to "System Standby",
            "ready_desc" to "Pipelines configured and prepared for overlays.",
            
            "hdr_quick" to "Quick Adjustments",
            "hdr_rec_settings" to "Capture Settings",
            "hdr_perf" to "REAL-TIME DIAGNOSTICS",
            
            "btn_start" to "Start Projection",
            "btn_stop" to "Stop Capturing",
            "btn_perm_mic" to "Allow Mic Audio Line",
            "btn_perm_notify" to "Background Alerts",
            
            "gallery_title" to "Gallery",
            "gallery_sub" to "Captured Clips",
            "no_clips" to "No recorded sessions discovered inside internal folders.",
            "load_demo" to "Load Test Clips",
            "demo_generated" to "Demo recordings generated for testing!",
            
            "sett_video" to "Video Customization",
            "sett_res" to "Resolution quality",
            "sett_res_desc" to "Choose details density",
            "sett_fps" to "Frame Rate (FPS)",
            "sett_fps_desc" to "Higher feels smoother, consumes battery",
            "sett_bitrate" to "Video Bitrate (Mbps)",
            "sett_bitrate_desc" to "Controls stream depth and compression footprint",
            "sett_codec" to "Video Codec",
            "sett_codec_desc" to "Compression standard for active file encoder",
            "sett_audio" to "Audio Source Mode",
            "sett_audio_desc" to "Line level audio recordings setup",
            
            "sett_orientation" to "Video Orientation",
            "sett_orientation_desc" to "Forced physical angle of output video file",
            "sett_lang" to "Interface Language",
            "sett_lang_desc" to "Select preferred system translation overlays (Beta)",
            
            "sett_bubble_hdr" to "Draggable Floating Bubble",
            "sett_bubble_show" to "Show Floating Overlay Bubble",
            "sett_bubble_show_desc" to "Keep quick actions accessible anywhere",
            "sett_bubble_size" to "Bubble Dimensions",
            "sett_bubble_size_desc" to "Select floating trigger dimensions",
            "sett_bubble_alpha" to "Opacity transparency depth",
            "sett_bubble_hide" to "Exclude Bubble from Final Video",
            "sett_bubble_hide_desc" to "Keeps final projections looking professional",
            "sett_drawing" to "Canvas Drawing Toolkit",
            "sett_drawing_desc" to "Toggle pen marks overlay during screen cast",
            
            "about_title" to "About",
            "about_sub" to "App Info & Specs",
            "about_specs" to "Efficiency Statistics",
            "about_ram" to "RAM Standby",
            "about_ram_desc" to "Uses <20 MB standby system memory.",
            "about_battery" to "Battery Shield",
            "about_battery_desc" to "Completely avoids background sync hooks.",
            "about_gpu" to "GPU Metrics",
            "about_gpu_desc" to "Hardware-accelerated rendering stats.",
            
            "about_faq" to "Frequently Asked Questions",
            "about_credits" to "App Credits",
            "about_credits_desc" to "Built with Material You Design System 3 on modern Jetpack Compose.",
            
            "update_check" to "Check for Updates",
            "update_latest" to "Checking version tags...",
            "update_up_to_date" to "App is up to date!",
            "update_new_avail" to "New Update Available! v1.0.2 Stable"
        ),
        
        "Українська (Beta) 🇺🇦" to mapOf(
            "tab_capture" to "Захоплення",
            "tab_gallery" to "Галерея",
            "tab_settings" to "Налаштування",
            "tab_about" to "Про додаток",
            
            "app_version" to "v1.0.1 Unstable Beta",
            "app_subtitle" to "Записуйте екран, ігри та малюйте зі стилем",
            "active_profile" to "АКТИВНИЙ ПРОФІЛЬ",
            "hd_details" to "Щільність пікселів",
            "standby" to "Режим Очікування",
            "ready_desc" to "Процеси запису готові до запуску оверлеїв.",
            
            "hdr_quick" to "Швидкі Налаштування",
            "hdr_rec_settings" to "Параметри Запису",
            "hdr_perf" to "ДІАГНОСТИКА В РЕАЛЬНОМУ ЧАСІ",
            
            "btn_start" to "Почати Запис",
            "btn_stop" to "Зупинити Запис",
            "btn_perm_mic" to "Дозволити Мікрофон",
            "btn_perm_notify" to "Дозволити Сповіщення",
            
            "gallery_title" to "Галерея",
            "gallery_sub" to "Збережені кліпи",
            "no_clips" to "Не знайдено записів у внутрішній папці.",
            "load_demo" to "Завантажити Тестові Кліпи",
            "demo_generated" to "Демонстраційні кліпи створено!",
            
            "sett_video" to "Властивості Відео",
            "sett_res" to "Якість роздільної здатності",
            "sett_res_desc" to "Оберіть роздільну здатність екрана",
            "sett_fps" to "Частота кадрів (FPS)",
            "sett_fps_desc" to "Більше FPS виглядає плавніше, але споживає батарею",
            "sett_bitrate" to "Бітрейт відео (Mbps)",
            "sett_bitrate_desc" to "Керує глибиною потоку та розміром файлу",
            "sett_codec" to "Відео кодек",
            "sett_codec_desc" to "Стандарт стиснення для активного енкодера",
            "sett_audio" to "Джерело звуку",
            "sett_audio_desc" to "Налаштування ліній запису звуку",
            
            "sett_orientation" to "Орієнтація відео",
            "sett_orientation_desc" to "Фіксований кут нахилу вихідного відео",
            "sett_lang" to "Мова інтерфейсу",
            "sett_lang_desc" to "Обрати системну мову перекладу (Beta)",
            
            "sett_bubble_hdr" to "Плаваюча Кнопка Керування",
            "sett_bubble_show" to "Показувати плаваючу кнопку",
            "sett_bubble_show_desc" to "Будівельник меню швидких дій на екрані",
            "sett_bubble_size" to "Розмір кнопки",
            "sett_bubble_size_desc" to "Оберіть діаметр плаваючої кнопки",
            "sett_bubble_alpha" to "Рівень прозорості",
            "sett_bubble_hide" to "Приховати кнопку з запису",
            "sett_bubble_hide_desc" to "Стилістично виключає оверлей з фінального відео",
            "sett_drawing" to "Візуальне малювання pen-tools",
            "sett_drawing_desc" to "Малюйте поверх екрана під час трансляції",
            
            "about_title" to "Інфо",
            "about_sub" to "Специфікації додатка",
            "about_specs" to "Показники Ефективності",
            "about_ram" to "Збережений RAM",
            "about_ram_desc" to "Використовує менше 20 MB в режимі очікування.",
            "about_battery" to "Захист Батареї",
            "about_battery_desc" to "Повністю уникає фонових циклів синхронізації.",
            "about_gpu" to "Показники GPU",
            "about_gpu_desc" to "Апаратне прискорення рендеру прямо зараз.",
            
            "about_faq" to "Часті Питання та Відповіді",
            "about_credits" to "Автори та Технології",
            "about_credits_desc" to "Створено з Material You 3 на базі сучасного Jetpack Compose.",
            
            "update_check" to "Перевірити наявність оновлень",
            "update_latest" to "Перевірка версій...",
            "update_up_to_date" to "У вас остання версія!",
            "update_new_avail" to "Знайдено нове оновлення! v1.0.2 Stable"
        ),
        
        "Русский (Beta) 🇷🇺" to mapOf(
            "tab_capture" to "Захват",
            "tab_gallery" to "Галерея",
            "tab_settings" to "Настройки",
            "tab_about" to "О приложении",
            
            "app_version" to "v1.0.1 Unstable Beta",
            "app_subtitle" to "Записывайте экран, игры и рисуйте со стилем",
            "active_profile" to "АКТИВНЫЙ ПРОФИЛЬ",
            "hd_details" to "Плотность пикселей",
            "standby" to "Режим Ожидания",
            "ready_desc" to "Процессы записи готовы к трансляции оверлеев.",
            
            "hdr_quick" to "Быстрые Настройки",
            "hdr_rec_settings" to "Параметры Записи",
            "hdr_perf" to "ДИАГНОСТИКА В РЕАЛЬНОМ ВРЕМЕНИ",
            
            "btn_start" to "Начать Запись",
            "btn_stop" to "Остановить Запись",
            "btn_perm_mic" to "Разрешить Микрофон",
            "btn_perm_notify" to "Разрешить Уведомления",
            
            "gallery_title" to "Галерея",
            "gallery_sub" to "Сохранённые клипы",
            "no_clips" to "Записей во внутренней папке не обнаружено.",
            "load_demo" to "Загрузить Тестовые Клипы",
            "demo_generated" to "Демонстрационные клипы созданы!",
            
            "sett_video" to "Свойства Видео",
            "sett_res" to "Качество разрешения",
            "sett_res_desc" to "Выберите разрешение экрана",
            "sett_fps" to "Частота кадров (FPS)",
            "sett_fps_desc" to "Больше FPS выглядит плавнее, но расходует батарею",
            "sett_bitrate" to "Битрейт видео (Mbps)",
            "sett_bitrate_desc" to "Управление глубиной потока и размером файла",
            "sett_codec" to "Видео кодек",
            "sett_codec_desc" to "Стандарт сжатия активного энкодера",
            "sett_audio" to "Источник звука",
            "sett_audio_desc" to "Настройка линий записи звука",
            
            "sett_orientation" to "Ориентация видео",
            "sett_orientation_desc" to "Фиксированный угол направления видео",
            "sett_lang" to "Язык интерфейса",
            "sett_lang_desc" to "Выбрать предпочтительный язык перевода (Beta)",
            
            "sett_bubble_hdr" to "Плавающая Кнопка Управления",
            "sett_bubble_show" to "Показывать плавающую кнопку",
            "sett_bubble_show_desc" to "Позволяет быстро совершать действия на экране",
            "sett_bubble_size" to "Размер кнопки",
            "sett_bubble_size_desc" to "Выберите диаметр плавающей кнопки",
            "sett_bubble_alpha" to "Уровень прозрачности",
            "sett_bubble_hide" to "Скрыть кнопку из записи",
            "sett_bubble_hide_desc" to "Исключает оверлей управления из финального видео",
            "sett_drawing" to "Художественные pen-tools",
            "sett_drawing_desc" to "Рисуйте поверх экрана во время вещания",
            
            "about_title" to "Инфо",
            "about_sub" to "О приложении",
            "about_specs" to "Показатели Эффективности",
            "about_ram" to "Сбереженный RAM",
            "about_ram_desc" to "Использует менее 20 MB ОЗУ в режиме ожидания.",
            "about_battery" to "Защита Батареи",
            "about_battery_desc" to "Полностью избегает фоновых циклов синхронизации.",
            "about_gpu" to "Показатели GPU",
            "about_gpu_desc" to "Аппаратное ускорение рендера прямо сейчас.",
            
            "about_faq" to "Частые Вопросы и Ответы",
            "about_credits" to "Разработчики и Спеки",
            "about_credits_desc" to "Создано с Material You 3 на базе современного Jetpack Compose.",
            
            "update_check" to "Проверить наличие обновлений",
            "update_latest" to "Проверка версий...",
            "update_up_to_date" to "У вас последняя версия!",
            "update_new_avail" to "Найдено новое обновление! v1.0.2 Stable"
        )
    )

    fun translate(key: String, selectedLang: String): String {
        val dict = translations[selectedLang] ?: translations["English"]!!
        return dict[key] ?: translations["English"]?.get(key) ?: key
    }
}
