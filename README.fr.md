<p align="center"><img src="docs/media/logo.png" alt="ChouiTV Installer logo (Ch8TV)" width="132"></p>

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="docs/media/hero-fr.png" alt="ChouiTV Installer : installez ChouiTV sur votre télé depuis votre téléphone" width="100%"></a></p>

<p align="center"><a href="README.md">🇲🇦 العربية</a> &nbsp;·&nbsp; <b>🇫🇷 Français</b> &nbsp;·&nbsp; <a href="README.en.md">🇬🇧 English</a></p>

<p align="center">
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest"><img src="https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=installer&color=D6AF6B&labelColor=0A1D14" alt="version"></a>
<img src="https://img.shields.io/badge/Android-8%2B-3F7A5C?labelColor=0A1D14" alt="Android 8+">
<a href="LICENSE"><img src="https://img.shields.io/badge/licence-MIT-D6AF6B?labelColor=0A1D14" alt="MIT"></a>
<a href="https://github.com/EMCHOUIT/ChouiTV-Installer/stargazers"><img src="https://img.shields.io/github/stars/EMCHOUIT/ChouiTV-Installer?style=flat&color=D6AF6B&labelColor=0A1D14" alt="stars"></a>
</p>

<p align="center"><a href="#download">⬇️ Téléchargement</a> · <a href="#why">✨ Pourquoi</a> · <a href="#journey">🚀 Le parcours</a> · <a href="#installs">📦 Installé</a> · <a href="#safety">🛡️ Sécurité</a> · <a href="#cleanup">🧹 Nettoyage</a> · <a href="#faq">❓ FAQ</a> · <a href="#build">🛠️ Compiler</a></p>

Une petite appli sur votre téléphone qui trouve votre télé, télécharge la **dernière version** de [ChouiTV](https://github.com/EMCHOUIT/ChouiTV), l'installe et la règle. **Sans ordinateur, sans commande, en environ deux minutes.**

<p align="center"><a href="https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk"><img src="https://img.shields.io/badge/%E2%AC%87%20Télécharger%20ChouiTV%20Installer-D6AF6B?style=for-the-badge&labelColor=0A1D14" alt="download"></a><br><sub>Pour téléphone Android 8 ou plus · environ 1 Mo</sub></p>

<a id="download"></a>

## ⬇️ Téléchargement direct

<table>
<tr>
<td width="230" valign="top" align="center"><img src="docs/media/qr-download.png" alt="QR code" width="210"><br><sub>Scannez depuis votre ordinateur pour ouvrir le lien sur votre téléphone.</sub></td>
<td valign="top">

[**⬇ Télécharger l'APK**](https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest/download/ChouiTV-Installer.apk) · `ChouiTV-Installer.apk` · environ 1 Mo

[![version](https://img.shields.io/github/v/release/EMCHOUIT/ChouiTV-Installer?label=version&color=D6AF6B&labelColor=0A1D14)](https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest) [![downloads](https://img.shields.io/github/downloads/EMCHOUIT/ChouiTV-Installer/total?label=downloads&color=3F7A5C&labelColor=0A1D14)](https://github.com/EMCHOUIT/ChouiTV-Installer/releases)

- 📱 Android 8 ou plus
- 🔒 Signé, sans compte ni publicité
- 📦 [Toutes les versions](https://github.com/EMCHOUIT/ChouiTV-Installer/releases)

</td>
</tr>
</table>

### Installer en trois gestes

1. **Téléchargez** le fichier `ChouiTV-Installer.apk` sur votre téléphone (le bouton ci-dessus, ou le QR code depuis un autre écran).
2. **Ouvrez-le.** Si Android demande une autorisation, acceptez une fois que votre navigateur ou votre gestionnaire de fichiers installe des applis. Un avertissement « appli inconnue » peut apparaître : le code est ouvert et vous pouvez le compiler vous-même ([Compiler](#build)).
3. **Ouvrez ChouiTV Installer** et suivez les étapes.

<details><summary><b>🔍 Vérifier le fichier</b></summary>

La [page de la version](https://github.com/EMCHOUIT/ChouiTV-Installer/releases/latest) affiche l'empreinte SHA-256 à côté du fichier. Comparez-la au résultat de :

```bash
sha256sum ChouiTV-Installer.apk                       # Linux, macOS
certutil -hashfile ChouiTV-Installer.apk SHA256       # Windows
```

L'empreinte du certificat de signature (SHA-256) est la même pour toutes les versions :

```text
f29f993b7456170455dcc574a2bc5f1f74b97f7995a564f8a839175d068a79c1
```

Pour la vérifier :

```bash
apksigner verify --print-certs ChouiTV-Installer.apk
```
</details>

<a id="why"></a>

## ✨ Pourquoi cette appli ?

<table>
<tr><td width="50%" valign="top">💻<br><b>Sans ordinateur</b><br><sub>Tout se fait depuis le téléphone : connexion, téléchargement, installation, réglages.</sub></td><td width="50%" valign="top">📊<br><b>Vous voyez tout</b><br><sub>Un pourcentage global et huit étapes qui avancent une à une sous vos yeux.</sub></td></tr>
<tr><td width="50%" valign="top">🛡️<br><b>Avec filet de sécurité</b><br><sub>Si le nouvel accueil ne s'ouvre pas, la télé rétablit seule le sien.</sub></td><td width="50%" valign="top">🌍<br><b>Dans votre langue</b><br><sub>Arabe, français et anglais, avec le même soin.</sub></td></tr>
</table>

<a id="journey"></a>

## 🚀 Le parcours en cinq étapes

<img src="docs/media/journey-fr.png" alt="🚀 Le parcours en cinq étapes" width="100%">

1. **Langue** : arabe, français ou anglais.
2. **Préparez la télé** : activez le « Débogage USB » et le « Débogage réseau » ; c'est expliqué dans l'appli.
3. **Connectez-vous** : l'appli trouve la télé sur le Wi-Fi, vous appuyez sur « Autoriser » sur son écran.
4. **Installation** : une barre avec pourcentage et huit étapes, de la vérification au contrôle final.
5. **Personnalisez, nettoyez (facultatif), puis c'est fini** : ville, fuseau horaire, méthode de prière ; un nettoyage facultatif des applis préinstallées ; un merci.

<a id="installs"></a>

## 📦 Ce qui est installé

| Application | Son rôle |
|---|---|
| **Écran d'accueil** | le premier écran de la télé, avec prière, écoute et dhikr |
| **Écran de veille et réglages** | dhikr, tasbih guidé et sagesse de l'histoire |
| **Application Chaînes** | 311 chaînes arabes en direct |

L'appli lit la version dans [`latest.json`](https://raw.githubusercontent.com/EMCHOUIT/ChouiTV/main/latest.json) : elle installe donc toujours la **dernière version** sans être recompilée, et vérifie l'empreinte SHA-256 de chaque fichier avant de l'envoyer.

<a id="safety"></a>

## 🛡️ Sécurité

- **Filet de sécurité** : avant de couper l'accueil d'origine, l'appli arme sur la télé un script qui le rétablit tout seul au bout de 5 minutes si ChouiTV ne s'ouvre pas.
- **Respect de votre télé** : si elle refuse de couper son accueil (comme la Nokia Streaming Box 8000), il est conservé et vous en êtes informé.
- **Nettoyage sans risque** : il est facultatif, il coupe sans supprimer, vérifie la télé après chaque groupe et remet tout en place si elle va mal.
- **Vie privée** : pas de compte, pas de publicité, pas de suivi ; la clé qui identifie votre téléphone y reste.

> [!TIP]
> **Une fois fini** : désactivez le « Débogage réseau » sur la télé.


<a id="cleanup"></a>

## 🧹 Nettoyez votre télé en sécurité

<img src="docs/media/cleanup-fr.png" alt="🧹 Nettoyez votre télé en sécurité" width="100%">

Certaines télés (Xiaomi, TCL, etc.) arrivent avec des applis préinstallées dont vous n'avez pas besoin : suivi, statistiques, publicités, services qui ralentissent l'appareil. Après l'installation, l'appli vous propose un **nettoyage facultatif**.

- **On coupe, on ne supprime pas** : chaque appli revient en un geste depuis « Restaurer » sur le premier écran.
- **Vous voyez le plan avant tout** : les applis sont rangées par groupe (suivi, publicités, préinstallées, facultatives) et vous choisissez.
- **Des listes réfléchies** : la liste ChouiTV pour les TCL, des listes communautaires pour le modèle de votre télé, et si le modèle est inconnu, seulement ce sur quoi plusieurs listes s'accordent.
- **Toujours protégé** : ChouiTV, les services Google, les réglages et tout écran d'accueil.
- **La télé est vérifiée après chaque groupe** : si elle va mal, le groupe est remis en place et le nettoyage s'arrête ; si la connexion est perdue, la télé remet tout en place d'elle-même au bout de 5 minutes.

Le point de restauration est gardé sur le téléphone et sur la télé.

<a id="faq"></a>

## ❓ Questions fréquentes

<details><summary><b>Je ne trouve pas ma télé dans la liste</b></summary>

Vérifiez que le téléphone et la télé sont sur le **même Wi-Fi** et que le « Débogage réseau » est activé sur la télé. Vous pouvez aussi saisir l'adresse de la télé à la main.
</details>

<details><summary><b>La fenêtre « Autoriser » n'apparaît pas sur la télé</b></summary>

L'appli attend deux minutes. Si la fenêtre n'apparaît pas, redémarrez la télé puis réessayez : une demande précédente est peut-être restée bloquée.
</details>

<details><summary><b>Le bouton Home n'ouvre pas ChouiTV</b></summary>

Certaines télés imposent leur accueil. L'appli vous le dit à la fin, et vous trouvez ChouiTV dans les applications de la télé. Dans tous les cas, la télé rétablit seule son accueil d'origine si le nouveau ne fonctionne pas.
</details>

<details><summary><b>Comment revenir en arrière ?</b></summary>

Réactivez l'accueil de la télé dans ses réglages, ou utilisez `optimize/reset.sh` de [ChouiTV](https://github.com/EMCHOUIT/ChouiTV).
</details>

<details><summary><b>Le nettoyage est-il sûr ?</b></summary>

Il est facultatif, il coupe sans supprimer, vous montre le plan d'abord et vérifie la télé après chaque groupe. Mais une appli coupée ne se met plus à jour, et une mise à jour du système peut en réactiver. Certaines télés perdraient leur Wi-Fi enregistré si l'assistant de configuration était coupé : il est donc protégé lui aussi.
</details>

<details><summary><b>Comment annuler un nettoyage ?</b></summary>

Depuis le premier écran, choisissez « Restaurer après un nettoyage », puis remettez une appli ou toutes. Une copie en texte est aussi sur la télé : `/data/local/tmp/chouitv-restore.txt`.
</details>

<a id="build"></a>

## 🛠️ Compiler

JDK 17 et Android SDK (plateforme 34) :

```bash
git clone https://github.com/EMCHOUIT/ChouiTV-Installer
cd ChouiTV-Installer
./gradlew :app:assembleRelease
```

## 💚 Crédits et licence

Licence **MIT** (EMChouit). Les polices et leurs licences : [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). Les applis installées sont celles de [ChouiTV](https://github.com/EMCHOUIT/ChouiTV). **Le projet vous plaît ? Mettez-lui une étoile ⭐, ça nous encourage à continuer.**
