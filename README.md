# CheatMode — mod Fabric (Minecraft 1.21.11)

Plus de 40 modules de cheat, un menu déplaçable (Shift droit) et des commandes dans le chat.
Marche **en solo** et **sur les serveurs multijoueur que tu autorises** (serveurs anarchie).

## Multijoueur (serveurs anarchie)
Le mod reste coupé sur un serveur tant que tu ne l'as pas autorisé :
- `-allow` : autorise le serveur où tu es (enregistré dans la config)
- `-disallow` : le retire de la liste
- `-servers` : liste des serveurs autorisés

⚠️ N'autorise que des serveurs où les cheats sont permis. Ailleurs, tu te fais bannir.

En multijoueur, c'est le serveur qui a le dernier mot, donc :
- **OP requis** (sur tes propres serveurs) : Reach, Timer, AntiVoid, GodMode, NoHunger, AntiFire, NoDrown, Magnet,
  FastBreak. Le mod envoie de vraies commandes (`/effect`, `/attribute`, `/tick`, `/tp`) : le serveur les accepte
  seulement si tu y es OP, sinon il les refuse et l'affiche dans le chat.
- **limités** : ClickTP et TpAura 8 blocs max, KillAura 5,5 blocs max, Nuker un bloc à la fois à vitesse normale
- `-gm` envoie `/gamemode`, qui ne marche que si tu es OP
- Fly redescend un peu toutes les 2 s pour éviter le kick « flying is not enabled » de base ;
  les serveurs avec un anti-cheat plus poussé peuvent quand même te rattraper.
- KillAura / KillAura Legit / TpAura : réglage **Joueurs** (0/1) pour attaquer aussi les joueurs.

## Installation
1. Installe [Fabric Loader](https://fabricmc.net/use/) pour la **1.21.11** et mets **Fabric API** (1.21.11) dans `mods/`.
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
