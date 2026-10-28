package ma.chouitv.installer;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Every text of the installer in Arabic, French and English. The language is chosen on the first screen. */
final class Strings {
    static String lang = "ar";
    private static final Map<String, String[]> M = new HashMap<>();

    private static void k(String key, String ar, String fr, String en) { M.put(key, new String[] {ar, fr, en}); }

    static String t(String key) {
        String[] v = M.get(key);
        if (v == null) return key;
        return v[lang.equals("fr") ? 1 : lang.equals("en") ? 2 : 0];
    }

    static String t(String key, Object... args) { return String.format(Locale.ENGLISH, t(key), args); }

    static boolean rtl() { return lang.equals("ar"); }

    static {
        k("tagline", "تلفزيونك بلمسة مغربية", "Votre télé, avec une touche marocaine", "Your TV, with a Moroccan touch");
        k("lang_title", "اختر لغتك", "Choisissez votre langue", "Choose your language");
        k("continue", "متابعة", "Continuer", "Continue");
        k("next", "التالي", "Suivant", "Next");
        k("prep_title", "جهّز التلفزيون", "Préparez la télé", "Prepare your TV");
        k("prep_sub", "ثلاث خطوات تستغرق دقيقة", "Trois étapes, une minute", "Three steps, one minute");
        k("prep1a", "الإعدادات ← النظام ← حول", "Paramètres → Système → À propos", "Settings → System → About");
        k("prep1b", "اضغط OK على Build سبع مرات", "Appuyez 7 fois sur OK sur « Build »", "Press OK on Build seven times");
        k("prep2a", "خيارات المطوّر", "Options pour les développeurs", "Developer options");
        k("prep2b", "فعّل «تصحيح USB»", "Activez « Débogage USB »", "Turn on \"USB debugging\"");
        k("prep3a", "تصحيح الشبكة (Network debugging)", "Débogage réseau (Network debugging)", "Network debugging");
        k("prep3b", "واترك التلفزيون على نفس الواي فاي", "Gardez la télé sur le même Wi-Fi", "Keep the TV on the same Wi-Fi");
        k("conn_title", "اتصل بالتلفزيون", "Connectez-vous à la télé", "Connect to your TV");
        k("conn_sub", "ابحث عنه على الواي فاي", "Recherche sur le Wi-Fi", "Looking for it on Wi-Fi");
        k("found", "تلفزيونات وُجدت", "Télés trouvées", "TVs found");
        k("searching", "جارٍ البحث على الواي فاي…", "Recherche en cours…", "Searching…");
        k("none_found", "لم يُعثر على تلفزيون. تأكد أن «تصحيح الشبكة» مفعّل، أو اكتب العنوان.",
            "Aucune télé trouvée. Vérifiez que le « Débogage réseau » est activé, ou saisissez l'adresse.",
            "No TV found. Check that \"Network debugging\" is on, or type the address.");
        k("tv", "تلفزيون", "Télé", "TV");
        k("or_type", "أو اكتب العنوان", "Ou saisissez l'adresse", "Or type the address");
        k("on_tv", "على التلفزيون", "Sur la télé", "On your TV");
        k("allow1", "اضغط «السماح» عند ظهور النافذة", "Appuyez sur « Autoriser » quand la fenêtre apparaît", "Press \"Allow\" when the window appears");
        k("allow2", "وفعّل «السماح دائمًا من هذا الجهاز» لتتذكّره", "Cochez « Toujours autoriser » pour qu'elle s'en souvienne", "Tick \"Always allow\" so it remembers this phone");
        k("connect", "اتصال", "Se connecter", "Connect");
        k("pick_tv", "اختر تلفزيونًا أو اكتب عنوانه", "Choisissez une télé ou saisissez son adresse", "Pick a TV or type its address");
        k("connecting", "جارٍ الاتصال… اقبل الطلب على شاشة التلفزيون (%d ث)", "Connexion… Acceptez la demande sur la télé (%d s)", "Connecting… Accept the request on the TV (%d s)");
        k("err_allow", "لم يصل رد من التلفزيون. تأكد أنك ضغطت «السماح» على شاشته، ثم أعد المحاولة.",
            "La télé n'a pas répondu. Vérifiez que vous avez appuyé sur « Autoriser » sur son écran, puis réessayez.",
            "The TV did not answer. Make sure you pressed \"Allow\" on its screen, then try again.");
        k("err_unreach", "تعذّر الوصول إلى %s. تأكد أن التلفزيون يعمل وعلى نفس الواي فاي وأن «تصحيح الشبكة» مفعّل.",
            "Impossible de joindre %s. Vérifiez que la télé est allumée, sur le même Wi-Fi, avec le « Débogage réseau » activé.",
            "Cannot reach %s. Check that the TV is on, on the same Wi-Fi, with \"Network debugging\" on.");
        k("err_net", "تعذّر التنزيل. تأكد أن الهاتف متصل بالإنترنت ثم أعد المحاولة.",
            "Téléchargement impossible. Vérifiez que le téléphone a Internet, puis réessayez.",
            "Download failed. Check that the phone has internet, then try again.");
        k("err_generic", "حدث خطأ: %s", "Une erreur est survenue : %s", "Something went wrong: %s");
        k("install_title", "تثبيت ChouiTV", "Installation de ChouiTV", "Installing ChouiTV");
        k("install_sub", "لا تُغلق التطبيق", "Ne fermez pas l'application", "Do not close the app");
        k("row_home", "الواجهة الرئيسية", "Écran d'accueil", "Home screen");
        k("row_saver", "شاشة التوقف والإعدادات", "Écran de veille et réglages", "Screensaver and settings");
        k("row_channels", "تطبيق القنوات", "Application Chaînes", "Channels app");
        k("ch_sub", "311 قناة", "311 chaînes", "311 channels");
        k("row_config", "ضبط الشاشة والصلاحيات", "Réglage de l'écran et permissions", "Screen setup and permissions");
        k("version", "الإصدار", "Version", "Version");
        k("installing", "جارٍ التثبيت…", "Installation…", "Installing…");
        k("retry", "أعد المحاولة", "Réessayer", "Try again");
        k("err_install", "تعذّر التثبيت. %s", "Échec de l'installation. %s", "Install failed. %s");
        k("custom_title", "خصّصه لبيتك", "Personnalisez", "Make it yours");
        k("custom_sub", "يمكنك تغييرها لاحقًا", "Modifiable plus tard", "You can change this later");
        k("city", "المدينة", "Ville", "City");
        k("tz", "المنطقة الزمنية", "Fuseau horaire", "Time zone");
        k("method", "طريقة حساب الصلاة", "Méthode de calcul de la prière", "Prayer calculation method");
        k("adhan", "الأذان التلقائي", "Adhan automatique", "Automatic adhan");
        k("on", "يعمل", "Activé", "On");
        k("off", "متوقف", "Désactivé", "Off");
        k("import", "استيراد ملف إعدادات", "Importer un fichier de réglages", "Import a settings file");
        k("import_sub", "chouitv-settings.json من تلفزيون آخر", "chouitv-settings.json d'une autre télé", "chouitv-settings.json from another TV");
        k("import_ready", "جاهز: يُطبَّق عند الضغط على «تطبيق»", "Prêt : appliqué au clic sur « Appliquer »", "Ready: applied when you press \"Apply\"");
        k("apply", "تطبيق", "Appliquer", "Apply");
        k("applying", "جارٍ التطبيق…", "Application…", "Applying…");
        k("region", "الجهة", "Région", "Region");
        k("cities_unavail", "قائمة المدن غير متاحة", "Liste des villes indisponible", "City list unavailable");
        k("will_change", "سيتغيّر على التلفزيون", "Ce qui va changer sur la télé", "What will change on the TV");
        k("no_change", "لا شيء يتغيّر", "Rien ne change", "Nothing changes");
        k("use_file", "استخدم هذا الملف", "Utiliser ce fichier", "Use this file");
        k("cancel", "إلغاء", "Annuler", "Cancel");
        k("err_apply", "تعذّر التطبيق: %s", "Échec de l'application : %s", "Could not apply: %s");
        k("err_read", "تعذّر قراءة الملف: %s", "Lecture du fichier impossible : %s", "Could not read the file: %s");
        k("file_big", "الملف كبير جدًا", "Fichier trop gros", "File too large");
        k("done_ready", "ChouiTV جاهز", "ChouiTV est prêt", "ChouiTV is ready");
        k("done_sub", "التلفزيون يعمل الآن بإعداداتك\nوالتحديثات القادمة تصلك وحدها",
            "La télé fonctionne avec vos réglages\net les prochaines mises à jour arrivent seules",
            "Your TV now runs with your settings\nand future updates arrive by themselves");
        k("tip", "نصيحة", "Conseil", "Tip");
        k("tip_text", "أغلق «تصحيح الشبكة» على التلفزيون عند الانتهاء", "Désactivez le « Débogage réseau » sur la télé une fois fini", "Turn \"Network debugging\" off on the TV when you are done");
        k("open_tv", "افتح على التلفزيون", "Ouvrir sur la télé", "Open on the TV");
        k("step_of", "الخطوة %d من %d", "Étape %d sur %d", "Step %d of %d");
        k("eta_lt1", "أقل من دقيقة", "moins d'une minute", "under a minute");
        k("eta_min", "نحو %d د", "environ %d min", "about %d min");
        k("mb", "م.ب", "Mo", "MB");
        k("s_check", "فحص التلفزيون", "Vérification de la télé", "Checking the TV");
        k("s_download", "تنزيل الإصدار %s", "Téléchargement de la version %s", "Downloading version %s");
        k("s_saver", "شاشة التوقف والإعدادات", "Écran de veille et réglages", "Screensaver and settings");
        k("s_channels", "تطبيق القنوات", "Application Chaînes", "Channels app");
        k("s_home", "الواجهة الرئيسية", "Écran d'accueil", "Home screen");
        k("s_perms", "الصلاحيات", "Permissions", "Permissions");
        k("s_defaults", "الواجهة الافتراضية", "Accueil par défaut", "Default home");
        k("s_verify", "التحقق من التثبيت", "Vérification de l'installation", "Checking the install");
        k("sub_send", "إرسال %d%%", "Envoi %d%%", "Sending %d%%");
        k("sub_install", "تثبيت…", "Installation…", "Installing…");
        k("sub_perms", "الموقع · المنبّهات", "Position · alarmes", "Location · alarms");
        k("sub_defaults", "مع شاشة التوقف", "et écran de veille", "and screensaver");
        k("sub_verify", "الإصدارات والواجهة", "Versions et accueil", "Versions and home");
        k("thanks_title", "شكرًا لاختيارك ChouiTV", "Merci d'avoir choisi ChouiTV", "Thank you for choosing ChouiTV");
        k("thanks_sub", "تلفزيونك جاهز، والتحديثات تصلك وحدها", "Votre télé est prête, les mises à jour arrivent seules", "Your TV is ready and updates arrive by themselves");
        k("star_title", "أعجبك التطبيق؟", "ChouiTV vous plaît ?", "Enjoying ChouiTV?");
        k("star_text", "نجمة على GitHub تدعمنا وتشجّعنا على الاستمرار", "Une étoile sur GitHub nous soutient et nous encourage à continuer", "A star on GitHub supports us and keeps us going");
        k("star_btn", "ضع نجمة للمشروع", "Mettre une étoile au projet", "Star the project");
        k("z_Africa/Casablanca", "المغرب · GMT (Africa/Casablanca)", "Maroc · GMT (Africa/Casablanca)", "Morocco · GMT (Africa/Casablanca)");
        k("sub_switch", "تعطيل الواجهة الأصلية مع شبكة أمان", "Accueil d'origine coupé, avec filet de sécurité", "Original home off, with a safety net");
        k("home_ok", "الزر Home يفتح ChouiTV", "Le bouton Home ouvre ChouiTV", "The Home button opens ChouiTV");
        k("home_warn", "تحقق من الزر Home", "Vérifiez le bouton Home", "Check the Home button");
        k("home_kept", "هذا التلفزيون يحتفظ بواجهته الأصلية: افتح ChouiTV من قائمة التطبيقات.",
            "Cette télé garde son accueil d'origine : ouvrez ChouiTV depuis ses applications.",
            "This TV keeps its original home: open ChouiTV from its apps.");
        k("home_unsure", "تعذّر التأكد من الزر Home. إن لم يفتح ChouiTV، فالتلفزيون يعيد واجهته الأصلية وحده خلال 5 دقائق.",
            "Impossible de confirmer le bouton Home. S'il n'ouvre pas ChouiTV, la télé rétablit seule son accueil d'origine d'ici 5 minutes.",
            "Could not confirm the Home button. If it does not open ChouiTV, the TV puts its original home back by itself within 5 minutes.");
        k("cl_title", "تنظيف اختياري", "Nettoyage facultatif", "Optional cleanup");
        k("cl_sub", "أوقف التطبيقات المثبّتة مسبقًا التي تبطئ التلفزيون", "Coupez les applis préinstallées qui ralentissent la télé", "Turn off the preinstalled apps that slow your TV down");
        k("cl_b1_t", "إيقاف لا حذف", "On coupe, on ne supprime pas", "Turned off, never deleted");
        k("cl_b1_d", "كل تطبيق يمكن إرجاعه بلمسة", "Chaque appli se remet en un geste", "Every app can be put back in one tap");
        k("cl_b2_t", "ترى القائمة قبل أي شيء", "Vous voyez la liste avant tout", "You see the list first");
        k("cl_b2_d", "وتختار المجموعات التي تريدها", "et vous choisissez les groupes voulus", "and choose the groups you want");
        k("cl_b3_t", "تطبيقاتك محمية", "Vos applis sont protégées", "Your apps are protected");
        k("cl_b3_d", "ChouiTV وGoogle والإعدادات لا تُمَس أبدًا", "ChouiTV, Google et les réglages ne sont jamais touchés", "ChouiTV, Google and the settings are never touched");
        k("cl_b4_t", "شبكة أمان على التلفزيون", "Un filet de sécurité sur la télé", "A safety net on the TV");
        k("cl_b4_d", "إن تعطّل شيء يعود كل شيء خلال 5 دقائق", "Si quelque chose casse, tout revient en 5 minutes", "If something breaks, everything comes back in 5 minutes");
        k("cl_skip", "تخطَّ، بدون تنظيف", "Passer, sans nettoyage", "Skip, no cleanup");
        k("cl_show", "اعرض الخطة", "Voir le plan", "Show me the plan");
        k("cl_loading", "جارٍ قراءة تطبيقات التلفزيون…", "Lecture des applis de la télé…", "Reading the TV's apps…");
        k("cl_err", "تعذّر: %s", "Échec : %s", "Failed: %s");
        k("cl_plan_title", "الخطة", "Le plan", "The plan");
        k("cl_plan_sub", "لن يتغير شيء قبل موافقتك", "Rien ne change avant votre accord", "Nothing changes before you agree");
        k("cl_src_ours", "قائمة ChouiTV لتلفزيونات TCL", "Liste ChouiTV pour les TCL", "ChouiTV's list for TCL");
        k("cl_src_model", "قائمة المجتمع لهذا الطراز", "Liste communautaire pour ce modèle", "Community list for this model");
        k("cl_src_amb", "طراز ملتبس: فقط ما اتفقت عليه القوائم", "Modèle ambigu : seulement ce sur quoi les listes s'accordent", "Ambiguous model: only what the lists agree on");
        k("cl_src_cons", "طراز غير معروف: فقط ما اتفقت عليه القوائم", "Modèle inconnu : seulement ce sur quoi les listes s'accordent", "Unknown model: only what the lists agree on");
        k("cl_already", "%d موقوفة سابقًا", "%d déjà coupées", "%d already off");
        k("cl_none", "لا شيء لتنظيفه: تلفزيونك نظيف", "Rien à nettoyer : votre télé est propre", "Nothing to clean: your TV is tidy");
        k("cl_continue", "متابعة", "Continuer", "Continue");
        k("cl_g_tracking", "التتبع والإحصاءات", "Suivi et statistiques", "Tracking and analytics");
        k("cl_g_ads", "الإعلانات", "Publicités", "Ads");
        k("cl_g_pre", "تطبيقات مثبّتة مسبقًا", "Applis préinstallées", "Preinstalled apps");
        k("cl_g_opt", "اختيارية (غير مفعّلة)", "Facultatives (décochées)", "Optional (off by default)");
        k("cl_count", "%d تطبيقًا", "%d applis", "%d apps");
        k("cl_tap", "المس لعرض الأسماء", "touchez pour voir les noms", "tap to see the names");
        k("cl_protected", "%d تطبيقًا محميًا", "Applis protégées : %d", "Protected apps: %d");
        k("cl_protected_d", "ChouiTV وGoogle وواجهات الإعداد لا تُمَس أبدًا", "ChouiTV, Google et la configuration ne sont jamais touchés", "ChouiTV, Google and setup are never touched");
        k("cl_run", "نظّف %d تطبيقًا", "Nettoyer %d applis", "Clean %d apps");
        k("cl_run_title", "جارٍ التنظيف", "Nettoyage en cours", "Cleaning");
        k("cl_run_sub", "أبقِ التطبيق مفتوحًا والتلفزيون شغّالًا", "Gardez l'appli ouverte et la télé allumée", "Keep the app open and the TV on");
        k("cl_rp", "نقطة استرجاع محفوظة", "Point de restauration enregistré", "Restore point saved");
        k("cl_rp_d", "على الهاتف وعلى التلفزيون", "sur le téléphone et sur la télé", "on the phone and on the TV");
        k("cl_checked", "كل مجموعة تُفحص بعد إيقافها", "Chaque groupe est vérifié après coupure", "Each group is checked after it is turned off");
        k("cl_running", "جارٍ العمل…", "En cours…", "Working…");
        k("cl_net_lost", "انقطع الاتصال. شبكة الأمان على التلفزيون تُرجع كل شيء خلال 5 دقائق.", "Connexion perdue. Le filet de sécurité de la télé remet tout en place en 5 minutes.", "Connection lost. The TV's safety net puts everything back in 5 minutes.");
        k("cl_res_title", "تم التنظيف", "Nettoyage terminé", "Cleanup done");
        k("cl_res_off", "موقوفة", "coupées", "turned off");
        k("cl_res_kept", "محمية", "protégées", "protected");
        k("cl_res_del", "محذوفة", "supprimées", "deleted");
        k("cl_rolled", "لم يعد التلفزيون سليمًا بعد «%s»: أُعيدت وتوقف التنظيف.", "La télé n'allait plus bien après « %s » : remis en place, nettoyage arrêté.", "The TV was not healthy after \"%s\": put back, cleanup stopped.");
        k("cl_refused", "%d رفضها التلفزيون وبقيت كما هي", "%d refusées par la télé, laissées en place", "%d refused by the TV, left on");
        k("cl_res_undo_t", "يمكنك التراجع دائمًا", "Vous pouvez toujours annuler", "You can always undo");
        k("cl_res_undo_d", "من الشاشة الأولى ← استرجاع", "depuis le premier écran → Restaurer", "from the first screen → Restore");
        k("cl_undo_all", "تراجع عن كل شيء", "Tout annuler", "Undo everything");
        k("cl_restored", "تمت الاستعادة ✓", "Restauré ✓", "Restored ✓");
        k("cl_restore_title", "استرجاع", "Restauration", "Restore");
        k("cl_restore_sub", "أعد ما أوقفه التنظيف", "Remettez ce que le nettoyage a coupé", "Put back what the cleanup turned off");
        k("cl_restore_none", "لا توجد نقطة استرجاع على هذا الهاتف", "Aucun point de restauration sur ce téléphone", "No restore point on this phone");
        k("cl_restore_one", "أعِد", "Remettre", "Put back");
        k("cl_restore_all", "أعِد الكل", "Tout remettre", "Put everything back");
        k("cl_restore_link", "استرجاع ما أوقفه التنظيف", "Restaurer après un nettoyage", "Restore after a cleanup");
        k("cl_pre_h", "تطبيقات مثبّتة مسبقًا", "Applis préinstallées", "Preinstalled apps");
        k("cl_pre_d", "مؤشَّرة مسبقًا لك. ألغِ علامة ما تستعمله.", "Déjà cochées pour vous. Décochez celles que vous utilisez.", "Already ticked for you. Untick the ones you use.");
        k("cl_str_h", "البث والترفيه", "Streaming et divertissement", "Streaming and entertainment");
        k("cl_str_d", "ضع علامة على ما لا تستعمله: كثيرون لا يفتحون هذه التطبيقات أبدًا.", "À cocher si vous ne les utilisez pas : beaucoup de gens n'ouvrent jamais ces applis.", "Tick the ones you do not use: many people never open these apps.");
        k("cl_usr_h", "تطبيقاتك المثبّتة", "Vos applis installées", "Your installed apps");
        k("cl_usr_d", "بعضها يبدأ مع التلفزيون ويبطئه. أشّرنا على ما ننصح بإيقافه.", "Certaines se lancent au démarrage de la télé et la ralentissent. Nous avons coché celles que nous conseillons de couper.", "Some start with the TV and slow it down. We ticked the ones we advise turning off.");
        k("cl_g_noise", "التتبّع والإعلانات", "Suivi et publicités", "Tracking and ads");
        k("cl_g_usr", "تطبيقاتك", "Vos applis", "Your apps");
        k("cl_ticked", "%d من %d", "%d sur %d cochées", "%d of %d ticked");
        k("cl_tick_all", "تأشير الكل", "Tout cocher", "Tick all");
        k("cl_untick_all", "إلغاء الكل", "Tout décocher", "Untick all");
        k("cl_conf_h", "قبل أن تتابع", "Avant de continuer", "Before you continue");
        k("cl_conf_stream", "أغلب الناس لا يستعملون Netflix أو Prime Video أو YouTube Music: أشّر عليها لتخفيف التلفزيون.", "La plupart des gens n'utilisent pas Netflix, Prime Video ou YouTube Music : cochez-les pour libérer la télé.", "Most people do not use Netflix, Prime Video or YouTube Music: tick them to free up the TV.");
        k("cl_conf_mine", "راجع تطبيقاتك: اترك بلا علامة ما تستعمله.", "Relisez vos applis : laissez décochées celles que vous utilisez.", "Look over your apps: leave unticked the ones you use.");
        k("cl_conf_keep", "ما تتركه بلا علامة يبقى كما هو.", "Ce que vous laissez décoché reste intact.", "Whatever you leave unticked stays untouched.");
        k("cl_conf_check", "راجعت القائمة: أحتفظ بما أستعمله", "J'ai vérifié la liste : je garde ce que j'utilise", "I checked the list: I keep what I use");
        k("cl_conf_needed", "أكّد أولًا أنك راجعت القائمة", "Confirmez d'abord que vous avez vérifié la liste", "First confirm that you checked the list");
        k("cl_chip_rec", "ننصح", "Conseillé", "Advised");
        k("cl_chip_boot", "يبدأ مع التلفزيون", "Démarre avec la télé", "Starts with the TV");
        k("cl_chip_ram", "%d م.ب", "%d Mo", "%d MB");
        k("cl_chip_recent", "استُعمل مؤخرًا", "Utilisée récemment", "Used lately");
        k("hub_title", "ماذا تريد؟", "Que faire ?", "What to do?");
        k("hub_sub", "اختر مساحة", "Choisissez un espace", "Pick a space");
        k("hub_clean_t", "نظّف تلفزيوني", "Nettoyer ma télé", "Clean my TV");
        k("hub_clean_d", "ابدأ من هنا: أوقف التطبيقات غير النافعة لتلفزيون أسرع. ترى القائمة وتختار.", "Commencez ici : coupez les applis inutiles pour une télé plus rapide. Vous voyez la liste et vous choisissez.", "Start here: turn off the useless apps for a faster TV. You see the list and you choose.");
        k("hub_clean_tag", "الخطوة 1 · مُستحسن", "Étape 1 · Conseillé", "Step 1 · Advised");
        k("hub_inst_t", "ثبّت ChouiTV", "Installer ChouiTV", "Install ChouiTV");
        k("hub_inst_d", "بعدها: يثبّت التطبيق ChouiTV على تلفزيونك ويضبطه.", "Ensuite : l'appli installe et règle ChouiTV sur votre télé.", "Then: the app installs and sets up ChouiTV on your TV.");
        k("hub_inst_tag", "الخطوة 2", "Étape 2", "Step 2");
        k("hub_react_t", "أعِد تفعيل تطبيقات", "Réactiver des applis", "Turn apps back on");
        k("hub_react_d", "عند الحاجة: أعِد تطبيقًا أوقفته.", "Au besoin : remettez une appli que vous avez coupée.", "When needed: put back an app you turned off.");
        k("hub_react_tag", "عند الحاجة", "Au besoin", "When needed");
        k("hub_lang", "اللغة", "Langue", "Language");
        k("cl_next_install", "تابع: ثبّت ChouiTV", "Continuer : installer ChouiTV", "Continue: install ChouiTV");
        k("cl_home", "العودة إلى الرئيسية", "Retour à l'accueil", "Back to the home");
        k("cl_notyet_t", "لم تنظّف بعد؟", "Pas encore nettoyé ?", "Not cleaned yet?");
        k("cl_notyet_d", "تلفزيون نظيف أسرع. ترى القائمة قبل أي تغيير.", "Une télé nettoyée est plus rapide. Vous voyez la liste avant tout changement.", "A cleaned TV is faster. You see the list before any change.");
        k("cl_notyet_btn", "نظّف الآن", "Nettoyer maintenant", "Clean now");
        k("re_title", "إعادة تفعيل تطبيقات", "Réactiver des applis", "Turn apps back on");
        k("re_sub", "أشّر على ما تريد إرجاعه", "Cochez celles que vous voulez remettre", "Tick the ones you want back");
        k("re_loading", "جارٍ قراءة التطبيقات الموقوفة…", "Lecture des applis coupées…", "Reading the apps that are off…");
        k("re_ours_h", "أوقفها ChouiTV", "Coupées par ChouiTV", "Turned off by ChouiTV");
        k("re_ours_d", "قرأناها من التلفزيون: ما زالت موقوفة.", "Lues sur la télé : elles sont encore coupées.", "Read from the TV: they are still off.");
        k("re_oth_h", "تطبيقات موقوفة أخرى", "Autres applis désactivées", "Other apps that are off");
        k("re_oth_d", "أوقفتها الشركة المصنّعة أو أنت. لا تُعِدها إلا إن كنت تعرف السبب.", "Coupées par le constructeur ou par vous. À réactiver seulement si vous savez pourquoi.", "Turned off by the manufacturer or by you. Turn them back on only if you know why.");
        k("re_none", "لا تطبيقات موقوفة: كل شيء في مكانه", "Aucune appli coupée : tout est en place", "No app is off: everything is in place");
        k("re_btn", "أعِد تفعيل %d", "Réactiver %d applis", "Turn %d apps back on");
        k("re_pick", "اختر تطبيقًا", "Choisissez une appli", "Pick an app");
        k("re_busy", "جارٍ العمل…", "En cours…", "Working…");
        k("re_done_t", "أُعيد تفعيلها", "Applis réactivées", "Apps back on");
        k("re_done_d", "%d تطبيقًا عاد", "%d applis de retour", "%d apps are back");
        k("re_fail", "%d لم تُفعَّل: حاول مجددًا", "%d n'ont pas voulu se réactiver : réessayez", "%d would not turn back on: try again");
        k("re_more", "أعِد تفعيل تطبيقات أخرى", "Réactiver d'autres applis", "Turn more apps back on");
        k("hub_set_t", "إعداداتي", "Mes réglages", "My settings");
        k("hub_set_d", "احفظ إعدادات تلفزيون باسم، وأرسلها إلى تلفزيون آخر.", "Enregistrez les réglages d'une télé sous un nom, envoyez-les à une autre.", "Save the settings of a TV under a name, send them to another one.");
        k("hub_set_tag", "جديد", "Nouveau", "New");
        k("st_title", "إعداداتي", "Mes réglages", "My settings");
        k("st_sub", "احفظ، سمِّ، أرسل", "Enregistrez, nommez, envoyez", "Save, name, send");
        k("st_save_h", "احفظ إعدادات هذا التلفزيون", "Enregistrer cette télé", "Save this TV");
        k("st_save_d", "أعطِ اسمًا لتجده لاحقًا. يبقى الملف على هاتفك فقط.", "Donnez un nom pour la retrouver plus tard. Le fichier reste sur votre téléphone, nulle part ailleurs.", "Give it a name to find it later. The file stays on your phone, nowhere else.");
        k("st_save_btn", "احفظ الإعدادات", "Enregistrer les réglages", "Save the settings");
        k("st_saved", "تم الحفظ ✓", "Enregistré ✓", "Saved ✓");
        k("st_err", "تعذّر: %s", "Échec : %s", "Failed: %s");
        k("st_list_h", "إعدادات محفوظة", "Réglages enregistrés", "Saved settings");
        k("st_empty", "لا شيء محفوظ بعد.", "Rien d'enregistré pour l'instant.", "Nothing saved yet.");
        k("st_send", "إرسال ›", "Envoyer ›", "Send ›");
        k("st_rename", "إعادة تسمية", "Renommer", "Rename");
        k("st_delete", "حذف", "Supprimer", "Delete");
        k("st_rename_h", "اسم جديد", "Nouveau nom", "New name");
        k("st_del_q", "حذف «%s»؟", "Supprimer « %s » ?", "Delete \"%s\"?");
        k("st_ok", "حسنًا", "OK", "OK");
        k("st_send_title", "إرسال «%s»", "Envoyer « %s »", "Send \"%s\"");
        k("st_send_sub", "تحقّق قبل التطبيق", "Vérifiez avant d'appliquer", "Check before applying");
        k("st_to", "إلى: %s", "Vers : %s", "To: %s");
        k("st_count", "%d تغييرًا · لم يتغيّر شيء بعد", "%d changements · rien n'est encore modifié", "%d changes · nothing is changed yet");
        k("st_nothing", "هذا التلفزيون يملك هذه الإعدادات بالفعل", "Cette télé a déjà ces réglages", "This TV already has these settings");
        k("st_err_import", "تعذّر على التلفزيون قراءة الملف. هل ChouiTV 3.173 أو أحدث عليه؟", "La télé n'a pas pu lire ce fichier. A-t-elle ChouiTV 3.173 ou plus ?", "The TV could not read this file. Does it have ChouiTV 3.173 or newer?");
        k("st_keep", "يحتفظ الإرسال بنسخة من إعدادات هذا التلفزيون الحالية: يمكنك التراجع.", "L'envoi garde une copie des réglages actuels de cette télé : vous pouvez annuler l'envoi.", "The send keeps a copy of this TV's current settings: you can undo it.");
        k("st_apply", "طبّق %d تغييرًا", "Appliquer les %d changements", "Apply the %d changes");
        k("st_sent_t", "أُرسلت الإعدادات", "Réglages envoyés", "Settings sent");
        k("st_sent_d", "طُبّق %d تغييرًا على %s", "%d changements appliqués sur %s", "%d changes applied on %s");
        k("st_undo", "تراجع عن الإرسال", "Annuler l'envoi", "Undo the send");
        k("st_undone", "عادت الإعدادات السابقة ✓", "Réglages précédents remis ✓", "Previous settings restored ✓");
        k("st_back", "العودة إلى إعداداتي", "Retour à mes réglages", "Back to my settings");
        // time zones and prayer methods
        k("z_Africa/Abidjan", "المغرب · GMT (Africa/Abidjan)", "Maroc · GMT (Africa/Abidjan)", "Morocco · GMT (Africa/Abidjan)");
        k("z_city", "حسب المدينة", "Selon la ville", "By city");
        k("z_Africa/Algiers", "الجزائر", "Algérie", "Algeria");
        k("z_Africa/Tunis", "تونس", "Tunisie", "Tunisia");
        k("z_Africa/Cairo", "مصر", "Égypte", "Egypt");
        k("z_Asia/Riyadh", "السعودية", "Arabie saoudite", "Saudi Arabia");
        k("z_Asia/Dubai", "الإمارات", "Émirats", "UAE");
        k("z_Europe/Paris", "فرنسا", "France", "France");
        k("m_21", "وزارة الأوقاف المغربية", "Ministère des Habous (Maroc)", "Moroccan Ministry of Awqaf");
        k("m_auto", "تلقائي حسب المنطقة", "Automatique selon la zone", "Automatic by zone");
        k("m_19", "الجزائر", "Algérie", "Algeria");
        k("m_18", "تونس", "Tunisie", "Tunisia");
        k("m_5", "الهيئة المصرية", "Autorité égyptienne", "Egyptian authority");
        k("m_4", "أم القرى", "Oumm al-Qura", "Umm al-Qura");
        k("m_3", "رابطة العالم الإسلامي", "Ligue islamique mondiale", "Muslim World League");
    }
}
