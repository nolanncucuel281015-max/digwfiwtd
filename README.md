# CheatMode — plugin Paper (1.21)

Mode « cheat » pour **ton propre serveur** (ou un serveur où tu es admin).
Réservé aux joueurs avec la permission `cheatmode.use` (les OP par défaut).

## Installation
```
mvn package
```
Copie `target/CheatMode-1.0.0.jar` dans le dossier `plugins/` de ton serveur Paper, puis redémarre.

## Commandes (à taper dans le chat)
Le message est intercepté : les autres joueurs ne le voient pas. `/cheatmode <cmd>` (ou `/cm`) marche aussi.

| Commande | Effet |
|---|---|
| `-gm creative` / `-gm survival` / `-gm spectator` / `-gm adventure` | change ton mode de jeu (aussi `c`, `s`, `sp`, `a`, `0-3`) |
| `-fly` | vol (+ pas de dégâts de chute) |
| `-autoclick` | frappe automatiquement l'entité que tu vises (CPS dans la config) |
| `-killaura` | frappe tous les mobs hostiles à portée |
| `-killaura legit` | une seule cible, courte portée, devant toi, en vue, au rythme du cooldown |
| `-tpaura` | se téléporte derrière le mob le plus proche et le frappe |
| `-clicktp` | clic droit avec une **plume** = téléportation là où tu regardes |
| `-xray` | les minerais brillent à travers les murs (visible que par toi) |
| `-sneak` | accroupi en permanence |
| `-x` | **x-ray + sneak + killaura legit** d'un coup (re-tape pour couper) |
| `-list` | modules actifs |
| `-off` | tout désactiver |
| `-help` | aide |

Réglages (portées, CPS, cibles joueurs/animaux, item du clicktp, rayon du x-ray…) dans `plugins/CheatMode/config.yml`.
