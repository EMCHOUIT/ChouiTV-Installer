<p align="center"><a href="README.md">🇲🇦 العربية</a> · <b>🇫🇷 Français</b> · <a href="README.en.md">🇬🇧 English</a></p>

<h1 align="center">ChouiTV Installer</h1>

<p align="center"><b>Installez ChouiTV sur votre télé depuis votre téléphone, sans ordinateur.</b></p>

<p align="center">
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest"><img src="https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=installer&color=D6AF6B&labelColor=0A1D14" alt="Installer version"></a>
<img src="https://img.shields.io/badge/Android-8%2B-3F7A5C?labelColor=0A1D14" alt="Android 8+">
<a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-D6AF6B?labelColor=0A1D14" alt="MIT"></a>
</p>

## ✨ Qu'est-ce que c'est ?

Une appli Android pour téléphone qui trouve votre télé sur le Wi-Fi, télécharge la **dernière version** de [ChouiTV](https://github.com/EMCHOUIT/ChouiTV), l'installe et la règle, en environ deux minutes, sans ordinateur ni commande.

## 🖼️ Captures

<table><tr><td align="center"><img src="docs/media/language.png" width="170"></td><td align="center"><img src="docs/media/prepare.png" width="170"></td><td align="center"><img src="docs/media/install.png" width="170"></td><td align="center"><img src="docs/media/customise.png" width="170"></td><td align="center"><img src="docs/media/done.png" width="170"></td><td align="center"><img src="docs/media/report.png" width="170"></td></tr></table>

## ⬇️ Télécharger

Téléchargez [**ChouiTV-Installer.apk**](https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk) sur un téléphone Android (8 ou plus) puis ouvrez-le. Le téléphone peut demander d'autoriser l'installation depuis cette source.

## 🚀 Comment ça marche

1. **Langue** : arabe, français ou anglais.
2. **Préparez la télé** : activez le « Débogage USB » et le « Débogage réseau » (expliqué dans l'appli).
3. **Connectez-vous** : l'appli trouve la télé sur le Wi-Fi ; vous appuyez sur « Autoriser » sur l'écran de la télé.
4. **Installation** : une barre de progression avec pourcentage et 8 étapes, de la vérification au contrôle final.
5. **Personnalisez** : ville, fuseau horaire (Africa/Casablanca), méthode de calcul de la prière, ou import d'un fichier de réglages.
6. **Terminé** : un merci, une demande d'étoile sur GitHub, et le partage facultatif du résultat de votre télé.

## 📦 Ce qui est installé

L'**écran d'accueil**, l'**écran de veille et les réglages**, et l'**application Chaînes** (311 chaînes), puis ChouiTV devient l'accueil par défaut. L'appli lit la version dans [`latest.json`](https://raw.githubusercontent.com/EMCHOUIT/ChouiTV/main/latest.json) et vérifie l'empreinte SHA-256 de chaque fichier.

## 🛡️ Sécurité

- **Filet de sécurité** : avant de couper l'accueil d'origine, l'appli arme sur la télé un script qui le rétablit tout seul au bout de 5 minutes si ChouiTV ne s'ouvre pas.
- Si une télé refuse de couper son accueil (comme la Nokia Streaming Box 8000), il est conservé et l'utilisateur est prévenu.
- Pas de compte, pas de publicité, pas de suivi. La clé qui identifie votre téléphone reste dans le téléphone.
- Désactivez le « Débogage réseau » sur la télé une fois fini.

## 📮 Partagez le résultat de votre télé

À la fin, vous pouvez partager le résultat de votre télé. **Si vous avez rencontré un problème**, votre rapport nous aide à trouver une solution. **Si tout s'est bien passé**, il nous aide à proposer de nouvelles fonctionnalités pour votre télé dans les prochaines versions. Vous voyez **tout ce qui sera envoyé avant l'envoi** (marque et modèle, Android, versions, résultat des étapes) ; **jamais** l'adresse IP, le nom du Wi-Fi, le numéro de série ni votre ville. C'est vous qui appuyez sur « Submit ». La liste est dans [Télés testées](https://github.com/EMCHOUIT/ChouiTV/blob/main/docs/TESTED-TVS.fr.md).

## 🛠️ Compiler

JDK 17 et Android SDK (plateforme 34) :

```bash
git clone https://github.com/EMCHOUIT/ChouiTV-Installer
cd ChouiTV-Installer
./gradlew :app:assembleRelease
```

## 💚 Crédits et licence

Licence **MIT** (EMChouit). Les polices et leurs licences : [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Les applis installées sont celles de [ChouiTV](https://github.com/EMCHOUIT/ChouiTV).
