# CheatMode — mod Fabric (Minecraft 1.21.4)

Plus de 40 modules de cheat, un menu déplaçable (Shift droit) et des commandes dans le chat.
**Fonctionne uniquement en monde solo** (se désactive tout seul en multijoueur).

## Installation
1. Installe [Fabric Loader](https://fabricmc.net/use/) pour la 1.21.4 et mets **Fabric API** dans `mods/`.
2. Compile : `./gradlew build` (Windows : `gradlew.bat build`) — il faut Java 21.
3. Copie `build/libs/cheatmode-1.0.0.jar` dans ton dossier `.minecraft/mods/`.

## Touches (modifiables dans Options → Commandes → CheatMode)
- **Shift droit** → menu
- **X** → X-Ray + Sneak + KillAura Legit (rappuie pour couper)
- **C** → zoom (quand le module Zoom est actif)

## Menu
Un panneau par catégorie. Clic gauche = ON/OFF, clic droit = réglages (gauche +, droit −),
survol = description. **Modifier** pour déplacer les panneaux, **Sauvegarder** pour garder positions
et réglages (`config/cheatmode.json`), **Réinitialiser** pour les remettre en place, clic droit sur un titre pour replier.

## Modules (`-nom` dans le chat pour ON/OFF, `-nom valeur` pour régler)

**Combat** — `-killaura`, `-killaura legit`, `-tpaura`, `-autoclick`, `-triggerbot`, `-aimassist`,
`-reach` (ex. `-reach 7`), `-antiknockback`, `-autototem`

**Mouvement** — `-fly`, `-speed` (ex. `-speed 5`), `-sprint`, `-nofall`, `-clicktp` (plume),
`-sneak`, `-jesus`, `-spider`, `-highjump`, `-step`, `-bunnyhop`, `-airjump`, `-timer` (ex. `-timer 60`), `-antivoid`

**Rendu** — `-xray`, `-storageesp`, `-mobesp`, `-fullbright`, `-zoom`, `-nohurtcam`, `-alwaysday`, `-noweather`

**Joueur** — `-god`, `-nohunger`, `-autorespawn`, `-antifire`, `-nodrown`, `-autotool`
+ boutons de mode de jeu dans le menu

**Monde** — `-nuker`, `-scaffold`, `-magnet`, `-fastbreak`, `-cheststealer`

**Autres** — `-gm creative|survival|spectator|adventure`, `-x`, `-list`, `-off`, `-menu`, `-help`

Les modules actifs s'affichent en haut à gauche, dans la couleur de leur catégorie.
