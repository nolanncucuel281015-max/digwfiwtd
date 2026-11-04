# CheatMode — mod Fabric (Minecraft 1.21.4)

Mod client : fly, speed, nofall, autoclick, killaura (normale + legit), tpaura, click TP, x-ray, sneak, et `-gm`.
**Fonctionne uniquement en monde solo** (se désactive tout seul en multijoueur).

## Installation
1. Installe [Fabric Loader](https://fabricmc.net/use/) pour la 1.21.4 et mets **Fabric API** dans `mods/`.
2. Compile : `./gradlew build` (Windows : `gradlew.bat build`) — il faut Java 21.
3. Copie `build/libs/cheatmode-1.0.0.jar` dans ton dossier `.minecraft/mods/`.

## Touches
- **Shift droit** → ouvre le menu.
- **X** → x-ray + sneak + killaura legit (rappuie pour couper).

Modifiables dans Options → Commandes → CheatMode.

## Menu (Shift droit)
Un panneau par catégorie : **Combat**, **Mouvement**, **Rendu**, **Joueur** (boutons de mode de jeu + preset X).
- Clic gauche sur un module → ON / OFF (surligné quand actif).
- Clic droit sur un module → ses réglages (portée, CPS, niveau de speed, rayon du x-ray…). Sur un réglage : clic gauche = +, clic droit = −.
- **Modifier** → active le déplacement : tire les panneaux par leur titre pour les placer où tu veux.
- Clic droit sur un titre → replier / déplier le panneau.
- **Sauvegarder** → enregistre les positions et les réglages dans `config/cheatmode.json` (rechargés au prochain lancement).
- **Réinitialiser** → remet les panneaux à leur place d'origine.

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
