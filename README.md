<p align="center"><img src="docs/media/logo.png" alt="ChouiTV Installer logo (Ch8TV)" width="132"></p>

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="docs/media/hero-ar.png" alt="ChouiTV Installer: ثبّت ChouiTV على تلفزيونك من هاتفك" width="100%"></a></p>

<p align="center"><b>🇲🇦 العربية</b> &nbsp;·&nbsp; <a href="README.fr.md">🇫🇷 Français</a> &nbsp;·&nbsp; <a href="README.en.md">🇬🇧 English</a></p>

<p align="center">
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest"><img src="https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=installer&color=D6AF6B&labelColor=0A1D14" alt="version"></a>
<img src="https://img.shields.io/badge/Android-8%2B-3F7A5C?labelColor=0A1D14" alt="Android 8+">
<a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-D6AF6B?labelColor=0A1D14" alt="MIT"></a>
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/stargazers"><img src="https://img.shields.io/github/stars/EMCHOUIT/ChouiTV-Installer?style=flat&color=D6AF6B&labelColor=0A1D14" alt="stars"></a>
</p>

<p align="center"><a href="#why">✨ لماذا</a> · <a href="#journey">🚀 الرحلة</a> · <a href="#installs">📦 ما يثبّته</a> · <a href="#safety">🛡️ الأمان</a> · <a href="#cleanup">🧹 التنظيف</a> · <a href="#faq">❓ أسئلة</a> · <a href="#build">🛠️ البناء</a></p>

<div dir="rtl" align="right">

تطبيق صغير على هاتفك يجد تلفزيونك، وينزّل **أحدث إصدار** من [ChouiTV](https://github.com/EMCHOUIT/ChouiTV)، ويثبّته ويضبطه. **بلا كمبيوتر، بلا أوامر، في نحو دقيقتين.**

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="https://img.shields.io/badge/%E2%AC%87%20نزّل%20ChouiTV%20Installer-D6AF6B?style=for-the-badge&labelColor=0A1D14" alt="download"></a><br><sub>لهاتف أندرويد 8 أو أحدث · الملف نحو 1 ميغابايت</sub></p>

<a id="why"></a>

## ✨ لماذا هذا التطبيق؟

<table dir="rtl">
<tr><td width="50%" valign="top">💻<br><b>بلا كمبيوتر</b><br><sub>كل شيء من الهاتف: الاتصال والتنزيل والتثبيت والضبط.</sub></td><td width="50%" valign="top">📊<br><b>تتابع كل شيء</b><br><sub>نسبة مئوية عامة، وثماني خطوات تراها تتقدّم واحدة بعد الأخرى.</sub></td></tr>
<tr><td width="50%" valign="top">🛡️<br><b>بشبكة أمان</b><br><sub>إن لم تفتح الواجهة الجديدة، يعيد التلفزيون واجهته الأصلية وحده.</sub></td><td width="50%" valign="top">🌍<br><b>بلغتك</b><br><sub>العربية والفرنسية والإنجليزية، بنفس العناية.</sub></td></tr>
</table>

<a id="journey"></a>

## 🚀 الرحلة في خمس خطوات

</div>

<img src="docs/media/journey-ar.png" alt="🚀 الرحلة في خمس خطوات" width="100%">

<div dir="rtl" align="right">

1. **اللغة**: اختر العربية أو الفرنسية أو الإنجليزية.
2. **جهّز التلفزيون**: فعّل «تصحيح USB» و«تصحيح الشبكة»؛ الشرح داخل التطبيق.
3. **اتصل**: يجد التطبيق تلفزيونك على الواي فاي، وتضغط «السماح» على شاشته.
4. **ثبّت**: شريط تقدّم ونسبة مئوية وثماني مراحل، من الفحص إلى التحقق.
5. **خصّص، نظّف اختياريًا، ثم تمّ**: المدينة والمنطقة الزمنية وطريقة حساب الصلاة؛ ثم تنظيف اختياري للتطبيقات المثبّتة مسبقًا؛ ثم شكر.

<a id="installs"></a>

## 📦 ماذا يثبّت؟

| التطبيق | دوره |
|---|---|
| **الواجهة الرئيسية** | شاشة التلفزيون الأولى، مع الصلاة والاستماع والأذكار |
| **شاشة التوقف والإعدادات** | الأذكار والتسبيح الموجَّه وحكمة من التاريخ |
| **تطبيق القنوات** | 311 قناة عربية مباشرة |

يقرأ التطبيق رقم الإصدار من [`latest.json`](https://raw.githubusercontent.com/EMCHOUIT/ChouiTV/main/latest.json)، فيثبّت دائمًا **آخر إصدار** دون أن يُعاد بناؤه، ويتحقق من بصمة SHA-256 لكل ملف قبل إرساله.

<a id="safety"></a>

## 🛡️ الأمان

- **شبكة أمان**: قبل تعطيل واجهة التلفزيون الأصلية يضع التطبيق عليه سكربتًا يعيدها وحده بعد 5 دقائق إن لم يفتح ChouiTV.
- **احترام تلفزيونك**: إن رفض تعطيل واجهته (مثل Nokia Streaming Box 8000) تبقى كما هي ويُخبَر المستخدم.
- **تنظيف بلا مخاطرة**: التنظيف اختياري، يوقف التطبيقات ولا يحذفها، ويفحص التلفزيون بعد كل مجموعة ويعيدها وحده إن ساء الحال.
- **خصوصية**: بلا حسابات ولا إعلانات ولا تتبّع، والمفتاح الذي يعرّف هاتفك يبقى فيه.

</div>

> [!TIP]
> **عند الانتهاء**: أغلق «تصحيح الشبكة» على التلفزيون.

<div dir="rtl" align="right">


<a id="cleanup"></a>

## 🧹 نظّف تلفزيونك بأمان

</div>

<img src="docs/media/cleanup-ar.png" alt="🧹 نظّف تلفزيونك بأمان" width="100%">

<div dir="rtl" align="right">

تعرض عليك بعض التلفزيونات (Xiaomi وTCL وغيرها) تطبيقات مثبّتة مسبقًا لا تحتاجها: تتبّع وإحصاءات وإعلانات وخدمات تبطئ الجهاز. بعد التثبيت يقترح عليك التطبيق **تنظيفًا اختياريًا**.

- **يوقف ولا يحذف**: كل تطبيق يعود بلمسة واحدة من «استرجاع» في الشاشة الأولى.
- **ترى الخطة قبل أي شيء**: الأسماء مجمّعة (التتبع، الإعلانات، المثبّتة مسبقًا، الاختيارية) وتختار ما تريد.
- **قوائم مدروسة**: قائمة ChouiTV لتلفزيونات TCL، وقوائم مجتمعية لطراز تلفزيونك، وإن لم يُعرف الطراز فقط ما اتفقت عليه عدة قوائم.
- **محمي دائمًا**: ChouiTV وخدمات Google والإعدادات وأي واجهة رئيسية.
- **يفحص التلفزيون بعد كل مجموعة**: إن ساء الحال تُعاد المجموعة وحدها ويتوقف التنظيف، وإن انقطع الاتصال أعاد التلفزيون كل شيء خلال 5 دقائق.

نقطة الاسترجاع تُحفظ على الهاتف وعلى التلفزيون.

<a id="faq"></a>

## ❓ أسئلة

<details><summary><b>لا أجد تلفزيوني في القائمة</b></summary>

تأكد أن الهاتف والتلفزيون على **نفس الواي فاي** وأن «تصحيح الشبكة» مفعّل على التلفزيون. يمكنك أيضًا كتابة عنوان التلفزيون يدويًا.
</details>

<details><summary><b>لا تظهر نافذة «السماح» على التلفزيون</b></summary>

ينتظر التطبيق دقيقتين. إن لم تظهر النافذة، أعد تشغيل التلفزيون ثم حاول مرة أخرى: قد يكون طلب سابق عالقًا.
</details>

<details><summary><b>الزر Home لا يفتح ChouiTV</b></summary>

بعض التلفزيونات تفرض واجهتها. يخبرك التطبيق بذلك في النهاية، وتجد ChouiTV في قائمة التطبيقات. وفي كل حال يعيد التلفزيون واجهته الأصلية وحده إن لم تعمل الجديدة.
</details>

<details><summary><b>كيف أتراجع؟</b></summary>

أعد تفعيل واجهة التلفزيون من إعداداته، أو استعمل `optimize/reset.sh` من [ChouiTV](https://github.com/EMCHOUIT/ChouiTV).
</details>

<details><summary><b>هل التنظيف آمن؟</b></summary>

هو اختياري ويوقف ولا يحذف، ويعرض عليك الخطة أولًا ويفحص التلفزيون بعد كل مجموعة. لكن التطبيقات الموقوفة لا تتحدّث، وقد يعيد تحديث النظام تفعيل بعضها. وبعض التلفزيونات تفقد الواي فاي المحفوظ إن أُوقفت خدمة الإعداد، لذلك تُحمى هي أيضًا.
</details>

<details><summary><b>كيف أتراجع عن التنظيف؟</b></summary>

من الشاشة الأولى اختر «استرجاع ما أوقفه التنظيف»، ثم أعد تطبيقًا واحدًا أو الكل. وعلى التلفزيون نسخة نصية في `/data/local/tmp/chouitv-restore.txt`.
</details>

<a id="build"></a>

## 🛠️ البناء

JDK 17 وAndroid SDK (المنصة 34):

```bash
git clone https://github.com/EMCHOUIT/ChouiTV-Installer
cd ChouiTV-Installer
./gradlew :app:assembleRelease
```

## 💚 الشكر والترخيص

ترخيص **MIT** (EMChouit). الخطوط وتراخيصها في [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). التطبيقات المثبَّتة هي [ChouiTV](https://github.com/EMCHOUIT/ChouiTV). **أعجبك المشروع؟ ضع له نجمة ⭐ فهي تشجّعنا على الاستمرار.**

</div>
