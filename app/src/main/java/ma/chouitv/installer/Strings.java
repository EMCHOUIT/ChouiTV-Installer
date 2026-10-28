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
        k("cl_res_mb", "ميغابايت RAM", "Mo de RAM", "MB of RAM");
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
