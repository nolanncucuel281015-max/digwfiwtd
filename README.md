# CheatMode — mod Fabric (Minecraft 1.21.4)

Mod client : fly, speed, nofall, autoclick, killaura (normale + legit), tpaura, click TP, x-ray, sneak, et `-gm`.
**Fonctionne uniquement en monde solo** (se désactive tout seul en multijoueur).

## Installation
1. Installe [Fabric Loader](https://fabricmc.net/use/) pour la 1.21.4 et mets **Fabric API** dans `mods/`.
2. Compile : `./gradlew build` (Windows : `gradlew.bat build`) — il faut Java 21.
3. Copie `build/libs/cheatmode-1.0.0.jar` dans ton dossier `.minecraft/mods/`.

## Touche
**X** → x-ray + sneak + killaura legit (rappuie pour couper). Modifiable dans Options → Commandes → CheatMode.

## Commandes (dans le chat, le message n'est pas envoyé)
| Commande | Effet |
|---|---|
| `-gm creative` / `-gm survival` / `-gm spectator` / `-gm adventure` | mode de jeu (marche même sans « cheats autorisés ») |
| `-fly` | vol |
| `-speed` / `-speed 5` | vitesse (niveau 3 par défaut, 1 à 10) |
| `-nofall` | aucun dégât de chute |
| `-autoclick` | maintiens clic gauche → 12 clics/s |
| `-killaura` | frappe tous les monstres à 5,5 blocs |
| `-killaura legit` | une seule cible, 3 blocs, devant toi, visible, coup chargé |
| `-tpaura` | se TP derrière le monstre le plus proche (16 blocs) et le frappe |
| `-clicktp` | clic droit avec une **plume** = TP là où tu regardes |
| `-xray` | minerais en surbrillance à travers les murs, couleur par minerai |
| `-sneak` | accroupi en permanence (ne tombe pas des bords) |
| `-x` | comme la touche X |
| `-list` / `-off` / `-help` | modules actifs / tout couper / aide |

Les modules actifs s'affichent en haut à gauche de l'écran.
