# Powers NJ — ASSET_REQUIREMENTS

Contrato entre o código e a produção de assets. Cada arquivo abaixo é procurado pelo código **exatamente** neste caminho
(namespace `powersnj`, raiz `src/main/resources/assets/powersnj/`). Basta salvar o arquivo final no mesmo caminho do placeholder.

## Regras gerais

- **Placeholders**: todos os PNGs marcados como _placeholder_ foram gerados por `tools/placeholder-assets/generate_placeholders.py`.
  O script **nunca sobrescreve** arquivos existentes (use `--force` só para regenerar placeholders).
- **Troca automática**: modelos GeckoLib, texturas de HUD, fundo/nós da Skill Tree e formas do simbionte são detectados em tempo de execução
  (`render/AssetAvailability`). Ao adicionar o arquivo, o renderer final passa a ser usado (F3+T recarrega). Sem o arquivo, o placeholder continua funcionando.
- PNG RGBA de 8 bits. Texturas de item/bloco/ícone: pixel art 16x16 (múltiplos de 16 aceitos para itens/ícones de habilidade).
- Nomes sempre em `snake_case` minúsculo.
- Coluna **Carregado por** indica a classe/sistema que lê o arquivo.

## 1. Trajes (por personagem)

Modelos, texturas e animações de trajes saem do pipeline em `tools/suit-assets` (veja o README de lá): o artista trabalha
num `.bbmodel` no **rig de traje** (`root` → `head`, `body`, braços, pernas como no Minecraft, ossos extras como capa e
antebraços) e o exportador gera a geometria de armadura GeckoLib, a textura e os clipes. Com o traje completo vestido, o
poder esconde a skin do jogador (`palladium:hide_body_part`) e as animações do personagem tocam para todos os jogadores.

### Thragg (`thragg`)

| Resource location | Resolução | Uso | Carregado por | Status |
|---|---|---|---|---|
| `powersnj:textures/suits/thragg/thragg_layer_1.png` | 64x32 (layout de armadura vanilla) | Fallback: só usado se o modelo GeckoLib for removido (ex.: resource pack) | `SuitArmorItem#getArmorTexture` → `SuitAssets.armorTexture` | placeholder |
| `powersnj:textures/suits/thragg/thragg_layer_2.png` | 64x32 | Fallback das calças | `SuitArmorItem#getArmorTexture` | placeholder |
| `powersnj:geo/suits/thragg.geo.json` | gerado de `art/suits/thragg/thragg.bbmodel` | Modelo do Thragg (35 cubos: cabelo, bigode, gola de pelo, ombreiras, braceletes, cinto, tabardo, capa em 3 partes, botas). Ossos locais: `cape`, `cape_mid`, `cape_lower`, `tabard_right/left`, `right/left_forearm`, `right/left_shin` | `SuitGeoModel` / `GeckoSuitRenderer` | **pronto** |
| `powersnj:textures/suits/thragg/thragg_geo.png` | 256x256 | Textura do modelo (enviada pelo autor) | `SuitGeoModel#getTextureResource` | **pronto** |
| `powersnj:animations/suits/thragg.animation.json` | Bedrock | 21 clipes `animation.thragg.*`: parado, andar, correr, agachar, defender, saltar, decolar, flutuar, voar, voo_rapido, pousar, soco_direito, soco_esquerdo, chute, combo, uppercut, investida, impacto_solo, onda_de_choque, recuar, vitoria | `SuitAnimationLibrary` | **pronto** |
| `powersnj:animation_sets/thragg.json` | JSON | Estados, golpes (soco direito/esquerdo, chute após correr) e eventos (recuar, vitória) | `SuitAnimationLibrary` → `SuitAnimator` | **pronto** |
| `powersnj:textures/item/thragg_helmet.png` | 16x16 | Ícone do item | `models/item/thragg_helmet.json` | placeholder |
| `powersnj:textures/item/thragg_chestplate.png` | 16x16 | Ícone do item | `models/item/thragg_chestplate.json` | placeholder |
| `powersnj:textures/item/thragg_leggings.png` | 16x16 | Ícone do item | `models/item/thragg_leggings.json` | placeholder |
| `powersnj:textures/item/thragg_boots.png` | 16x16 | Ícone do item | `models/item/thragg_boots.json` | placeholder |

### Venom (`venom`)

| Resource location | Resolução | Uso | Carregado por | Status |
|---|---|---|---|---|
| `powersnj:textures/suits/venom/venom_layer_1.png` | 64x32 (layout de armadura vanilla) | Capacete, peitoral/braços e botas no modelo vanilla | `SuitArmorItem#getArmorTexture` → `SuitAssets.armorTexture` | placeholder |
| `powersnj:textures/suits/venom/venom_layer_2.png` | 64x32 | Calças | `SuitArmorItem#getArmorTexture` | placeholder |
| `powersnj:geo/suits/venom.geo.json` | gerado por `tools/suit-assets/export_suit.py` | Modelo do traje no **rig de traje** (ver `tools/suit-assets/README.md`); o exportador cria os ossos `armorHead`...`armorLeftBoot` | `SuitGeoModel` / `GeckoSuitRenderer` (ativado por `SuitArmorClientExtensions`) | **faltando** (usa vanilla) |
| `powersnj:textures/suits/venom/venom_geo.png` | conforme UV do modelo (recomendado 256x256) | Textura do modelo GeckoLib (embutida no `.bbmodel`, extraída pelo exportador) | `SuitGeoModel#getTextureResource` | **faltando** |
| `powersnj:animations/suits/venom.animation.json` | Bedrock (gerado pelo exportador) | Clipes `animation.venom.*` do personagem (ver papéis em `tools/suit-assets/README.md`) | `SuitAnimationLibrary` (via `animation_sets/venom.json`) | **faltando** (poses vanilla) |
| `powersnj:animation_sets/venom.json` | JSON | Liga os clipes aos estados (parado, andar, voar...), golpes e eventos | `SuitAnimationLibrary` → `SuitAnimator` | **faltando** |
| `powersnj:textures/item/venom_helmet.png` | 16x16 | Ícone do item | `models/item/venom_helmet.json` | placeholder |
| `powersnj:textures/item/venom_chestplate.png` | 16x16 | Ícone do item | `models/item/venom_chestplate.json` | placeholder |
| `powersnj:textures/item/venom_leggings.png` | 16x16 | Ícone do item | `models/item/venom_leggings.json` | placeholder |
| `powersnj:textures/item/venom_boots.png` | 16x16 | Ícone do item | `models/item/venom_boots.json` | placeholder |

### Reverse-Flash (`reverse_flash`)

| Resource location | Resolução | Uso | Carregado por | Status |
|---|---|---|---|---|
| `powersnj:textures/suits/reverse_flash/reverse_flash_layer_1.png` | 64x32 (layout de armadura vanilla) | Capacete, peitoral/braços e botas no modelo vanilla | `SuitArmorItem#getArmorTexture` → `SuitAssets.armorTexture` | placeholder |
| `powersnj:textures/suits/reverse_flash/reverse_flash_layer_2.png` | 64x32 | Calças | `SuitArmorItem#getArmorTexture` | placeholder |
| `powersnj:geo/suits/reverse_flash.geo.json` | gerado por `tools/suit-assets/export_suit.py` | Modelo do traje no **rig de traje** (ver `tools/suit-assets/README.md`); o exportador cria os ossos `armorHead`...`armorLeftBoot` | `SuitGeoModel` / `GeckoSuitRenderer` (ativado por `SuitArmorClientExtensions`) | **faltando** (usa vanilla) |
| `powersnj:textures/suits/reverse_flash/reverse_flash_geo.png` | conforme UV do modelo (recomendado 256x256) | Textura do modelo GeckoLib (embutida no `.bbmodel`, extraída pelo exportador) | `SuitGeoModel#getTextureResource` | **faltando** |
| `powersnj:animations/suits/reverse_flash.animation.json` | Bedrock (gerado pelo exportador) | Clipes `animation.reverse_flash.*` do personagem (ver papéis em `tools/suit-assets/README.md`) | `SuitAnimationLibrary` (via `animation_sets/reverse_flash.json`) | **faltando** (poses vanilla) |
| `powersnj:animation_sets/reverse_flash.json` | JSON | Liga os clipes aos estados (parado, andar, voar...), golpes e eventos | `SuitAnimationLibrary` → `SuitAnimator` | **faltando** |
| `powersnj:textures/item/reverse_flash_helmet.png` | 16x16 | Ícone do item | `models/item/reverse_flash_helmet.json` | placeholder |
| `powersnj:textures/item/reverse_flash_chestplate.png` | 16x16 | Ícone do item | `models/item/reverse_flash_chestplate.json` | placeholder |
| `powersnj:textures/item/reverse_flash_leggings.png` | 16x16 | Ícone do item | `models/item/reverse_flash_leggings.json` | placeholder |
| `powersnj:textures/item/reverse_flash_boots.png` | 16x16 | Ícone do item | `models/item/reverse_flash_boots.json` | placeholder |

### Formas parciais do Venom

| Resource location | Resolução | Uso | Carregado por | Status |
|---|---|---|---|---|
| `powersnj:textures/suits/venom/forms/claws.png` | 64x64 (layout de skin de jogador) | Overlay da transformação parcial `claws` | `SymbioteFormLayer` (só desenha se existir) | **faltando** |
| `powersnj:textures/suits/venom/forms/blade_arm.png` | 64x64 (layout de skin de jogador) | Overlay da transformação parcial `blade_arm` | `SymbioteFormLayer` (só desenha se existir) | **faltando** |

## 2. Ícones de habilidades e skills

16x16 (ou 32x32). Usados pelo Palladium (barra de habilidades, Ability Wheel, tela de poderes — campo `icon` em
`data/powersnj/palladium/powers/<traje>.json`) e pela Skill Tree (campo `icon` em `data/powersnj/powersnj/suits/<traje>.json`).

| Resource location | Resolução | Carregado por | Status |
|---|---|---|---|
| `powersnj:textures/abilities/thragg/attack_wheel.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/heavy_punch.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/ground_slam.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/shockwave.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/charge.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/devastating_punch.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/flight_boost.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/high_speed_flight.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/super_strength.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/durability.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/toughness.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/knockback_resistance.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/fall_resistance.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/grand_regent.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/thragg/regeneration.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/tendril_wheel.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/tendril_grab.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/tendril_pull.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/tendril_swing.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/wall_crawl.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/symbiote_shield.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/claws.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/blade_arm.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/camouflage.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/enhanced_strength.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/durability.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/enhanced_movement.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/high_jump.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/fall_resistance.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/we_are_venom.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/regeneration.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/heat_weakness.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/venom/sonic_weakness.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/super_speed.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/speed_up.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/speed_down.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/speed_wheel.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/speed_punch.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/speed_dash.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/phase.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/rapid_attack.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/vibration.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/vibration_drain.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/vortex.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/wall_running.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/water_running.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/reflexes.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/durability.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/fall_resistance.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/negative_speed_force.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |
| `powersnj:textures/abilities/reverse_flash/accelerated_healing.png` | 16x16 | Palladium `IconSerializer` / `SkillTreeScreen` | placeholder |

## 3. GUI

| Resource location | Resolução | Uso | Carregado por | Status |
|---|---|---|---|---|
| `powersnj:textures/gui/suit_forge.png` | 256x256 (área usada 176x222) | Fundo da Suit Forge | `SuitForgeScreen` | placeholder |
| `powersnj:textures/gui/skill_tree/background.png` | 256x256 (repetida) | Fundo da Skill Tree | `SkillTreeScreen` | **faltando** (fundo escuro) |
| `powersnj:textures/gui/skill_tree/node_unlocked.png` | 26x26 | Moldura do nó (unlocked) | `SkillTreeScreen#nodeFrame` | **faltando** (moldura colorida) |
| `powersnj:textures/gui/skill_tree/node_available.png` | 26x26 | Moldura do nó (available) | `SkillTreeScreen#nodeFrame` | **faltando** (moldura colorida) |
| `powersnj:textures/gui/skill_tree/node_reachable.png` | 26x26 | Moldura do nó (reachable) | `SkillTreeScreen#nodeFrame` | **faltando** (moldura colorida) |
| `powersnj:textures/gui/skill_tree/node_locked.png` | 26x26 | Moldura do nó (locked) | `SkillTreeScreen#nodeFrame` | **faltando** (moldura colorida) |
| `powersnj:textures/gui/hud/default/energy_bar.png` | 110x10 (fundo v=0..4, preenchimento v=5..9) | Barra de energia do HUD | `PowersHudOverlay` / `HudProfile` | **faltando** (retângulos) |
| `powersnj:textures/gui/hud/default/xp_bar.png` | 110x6 (fundo v=0..2, preenchimento v=3..5) | Barra de XP do traje | `PowersHudOverlay` / `HudProfile` | **faltando** |
| `powersnj:textures/gui/hud/default/ability_frame.png` | 24x24 | Moldura da habilidade selecionada (reservado) | `HudProfile#abilityFrameTexture` | **faltando** |
| `powersnj:textures/gui/hud/viltrumite/energy_bar.png` | 110x10 (fundo v=0..4, preenchimento v=5..9) | Barra de energia do HUD | `PowersHudOverlay` / `HudProfile` | **faltando** (retângulos) |
| `powersnj:textures/gui/hud/viltrumite/xp_bar.png` | 110x6 (fundo v=0..2, preenchimento v=3..5) | Barra de XP do traje | `PowersHudOverlay` / `HudProfile` | **faltando** |
| `powersnj:textures/gui/hud/viltrumite/ability_frame.png` | 24x24 | Moldura da habilidade selecionada (reservado) | `HudProfile#abilityFrameTexture` | **faltando** |
| `powersnj:textures/gui/hud/symbiote/energy_bar.png` | 110x10 (fundo v=0..4, preenchimento v=5..9) | Barra de energia do HUD | `PowersHudOverlay` / `HudProfile` | **faltando** (retângulos) |
| `powersnj:textures/gui/hud/symbiote/xp_bar.png` | 110x6 (fundo v=0..2, preenchimento v=3..5) | Barra de XP do traje | `PowersHudOverlay` / `HudProfile` | **faltando** |
| `powersnj:textures/gui/hud/symbiote/ability_frame.png` | 24x24 | Moldura da habilidade selecionada (reservado) | `HudProfile#abilityFrameTexture` | **faltando** |
| `powersnj:textures/gui/hud/speedster/energy_bar.png` | 110x10 (fundo v=0..4, preenchimento v=5..9) | Barra de energia do HUD | `PowersHudOverlay` / `HudProfile` | **faltando** (retângulos) |
| `powersnj:textures/gui/hud/speedster/xp_bar.png` | 110x6 (fundo v=0..2, preenchimento v=3..5) | Barra de XP do traje | `PowersHudOverlay` / `HudProfile` | **faltando** |
| `powersnj:textures/gui/hud/speedster/ability_frame.png` | 24x24 | Moldura da habilidade selecionada (reservado) | `HudProfile#abilityFrameTexture` | **faltando** |

Coordenadas dos slots da Suit Forge (`SuitForgeMenu`), relativas ao canto superior esquerdo:

| Slot | Posição (x, y) |
|---|---|
| Blueprint | (8, 20) |
| Power Core | (8, 56) |
| Materiais 3x2 | (30 + col*18, 20 + linha*18) |
| Saída 2x2 (capacete, peitoral, calças, botas) | (116 + col*18, 20 + linha*18) |
| Barra de progresso | (88, 33), 24x6 |
| Painel de informações | (6, 74) até (170, 127) |
| Botão Fabricate | (114, 58), 56x14 |
| Inventário do jogador | (8, 140); hotbar (8, 198) |

## 4. Blocos

| Resource location | Resolução | Uso | Carregado por | Status |
|---|---|---|---|---|
| `powersnj:textures/block/viltrumite_ore.png` | 16x16 | Textura `cube_all` | `models/block/viltrumite_ore.json` | placeholder |
| `powersnj:textures/block/deepslate_viltrumite_ore.png` | 16x16 | Textura `cube_all` | `models/block/deepslate_viltrumite_ore.json` | placeholder |
| `powersnj:textures/block/speed_crystal_ore.png` | 16x16 | Textura `cube_all` | `models/block/speed_crystal_ore.json` | placeholder |
| `powersnj:textures/block/deepslate_speed_crystal_ore.png` | 16x16 | Textura `cube_all` | `models/block/deepslate_speed_crystal_ore.json` | placeholder |
| `powersnj:textures/block/viltrumite_block.png` | 16x16 | Textura `cube_all` | `models/block/viltrumite_block.json` | placeholder |
| `powersnj:textures/block/suit_forge_top.png` | 16x16 | Suit Forge (`front_on` = fabricando) | `models/block/suit_forge*.json` | placeholder |
| `powersnj:textures/block/suit_forge_side.png` | 16x16 | Suit Forge (`front_on` = fabricando) | `models/block/suit_forge*.json` | placeholder |
| `powersnj:textures/block/suit_forge_front.png` | 16x16 | Suit Forge (`front_on` = fabricando) | `models/block/suit_forge*.json` | placeholder |
| `powersnj:textures/block/suit_forge_front_on.png` | 16x16 | Suit Forge (`front_on` = fabricando) | `models/block/suit_forge*.json` | placeholder |
| `powersnj:textures/block/suit_stand.png` | 16x16 | Suit Stand | `models/block/suit_stand.json` | placeholder |
| `powersnj:models/block/suit_stand.json` | Blockbench JSON | Modelo do pedestal (o traje é desenhado por cima pelo `SuitStandRenderer`) | blockstate `suit_stand` | placeholder (elementos simples) |
| `powersnj:models/block/suit_forge.json` / `suit_forge_working.json` | Blockbench JSON | Modelo da forja (estado `working`) | blockstate `suit_forge` | placeholder (`orientable`) |

## 5. Itens (materiais e blueprints)

| Resource location | Resolução | Carregado por | Status |
|---|---|---|---|
| `powersnj:textures/item/power_core.png` | 16x16 | `models/item/power_core.json` | placeholder |
| `powersnj:textures/item/advanced_circuit.png` | 16x16 | `models/item/advanced_circuit.json` | placeholder |
| `powersnj:textures/item/reinforced_fabric.png` | 16x16 | `models/item/reinforced_fabric.json` | placeholder |
| `powersnj:textures/item/energy_conductor.png` | 16x16 | `models/item/energy_conductor.json` | placeholder |
| `powersnj:textures/item/raw_viltrumite.png` | 16x16 | `models/item/raw_viltrumite.json` | placeholder |
| `powersnj:textures/item/viltrumite_ingot.png` | 16x16 | `models/item/viltrumite_ingot.json` | placeholder |
| `powersnj:textures/item/viltrumite_alloy.png` | 16x16 | `models/item/viltrumite_alloy.json` | placeholder |
| `powersnj:textures/item/reinforced_viltrumite_fabric.png` | 16x16 | `models/item/reinforced_viltrumite_fabric.json` | placeholder |
| `powersnj:textures/item/organic_sample.png` | 16x16 | `models/item/organic_sample.json` | placeholder |
| `powersnj:textures/item/biomass.png` | 16x16 | `models/item/biomass.json` | placeholder |
| `powersnj:textures/item/symbiotic_fiber.png` | 16x16 | `models/item/symbiotic_fiber.json` | placeholder |
| `powersnj:textures/item/organic_compound.png` | 16x16 | `models/item/organic_compound.json` | placeholder |
| `powersnj:textures/item/speed_crystal.png` | 16x16 | `models/item/speed_crystal.json` | placeholder |
| `powersnj:textures/item/negative_energy_fragment.png` | 16x16 | `models/item/negative_energy_fragment.json` | placeholder |
| `powersnj:textures/item/conductive_fabric.png` | 16x16 | `models/item/conductive_fabric.json` | placeholder |
| `powersnj:textures/item/advanced_conductor.png` | 16x16 | `models/item/advanced_conductor.json` | placeholder |
| `powersnj:textures/item/viltrumite_blueprint.png` | 16x16 | `models/item/viltrumite_blueprint.json` | placeholder |
| `powersnj:textures/item/symbiote_blueprint.png` | 16x16 | `models/item/symbiote_blueprint.json` | placeholder |
| `powersnj:textures/item/speedster_blueprint.png` | 16x16 | `models/item/speedster_blueprint.json` | placeholder |

## 6. Entidades

| Resource location | Resolução | Uso | Carregado por | Status |
|---|---|---|---|---|
| `powersnj:textures/entity/tendril.png` | 16x64 (repete ao longo do comprimento, U = largura, V = comprimento) | Fita do tentáculo (placeholder procedural) | `TendrilRenderer` | placeholder |
| `powersnj:geo/entity/tendril.geo.json` | Blockbench (GeckoLib) | Modelo da ponta do tentáculo | `TendrilGeoRenderer` (ativado se existir) | **faltando** |
| `powersnj:textures/entity/tendril_geo.png` | conforme UV | Textura do modelo acima | `TendrilGeoRenderer` | **faltando** |
| `powersnj:animations/entity/tendril.animation.json` | GeckoLib | `animation.tendril.extend` (play once), `animation.tendril.idle` (loop) | `TendrilEntity#registerControllers` | **faltando** |

## 7. Partículas

Cada partícula usa 4 quadros 8x8 (`<nome>_0..3.png`) listados em `assets/powersnj/particles/<nome>.json` (`PowersParticle`).

| Partícula | Arquivos | Uso |
|---|---|---|
| `powersnj:speed_spark` | `textures/particle/speed_spark_0.png` … `_3.png` (8x8) | faíscas de velocidade / colisões |
| `powersnj:negative_lightning` | `textures/particle/negative_lightning_0.png` … `_3.png` (8x8) | relâmpago vermelho da Negative Speed Force (rastro, phasing) |
| `powersnj:symbiote_goo` | `textures/particle/symbiote_goo_0.png` … `_3.png` (8x8) | gosma do simbionte |
| `powersnj:shockwave_dust` | `textures/particle/shockwave_dust_0.png` … `_3.png` (8x8) | poeira de ground slam/shockwave |
| `powersnj:viltrumite_impact` | `textures/particle/viltrumite_impact_0.png` … `_3.png` (8x8) | impactos viltrumitas |

## 8. Sons

`assets/powersnj/sounds.json` aponta hoje para **eventos vanilla** (placeholders). Para os sons finais, salve os `.ogg` (mono para sons posicionais)
e troque a entrada correspondente para `{"name": "powersnj:<caminho>"}` (sem `"type": "event"`).

| Evento | Arquivo final sugerido | Tocado por |
|---|---|---|
| `powersnj:suit.equip` | `sounds/suit/equip.ogg` | `PowerManager (equipar), SuitStand` |
| `powersnj:suit.fabricate` | `sounds/suit/fabricate.ogg` | `SuitForgeBlockEntity` |
| `powersnj:progression.level_up` | `sounds/progression/level_up.ogg` | `ProgressionAPI` |
| `powersnj:progression.skill_unlock` | `sounds/progression/skill_unlock.ogg` | `ProgressionAPI` |
| `powersnj:thragg.ground_slam` | `sounds/thragg/ground_slam.ogg` | `GroundSlamAbility` |
| `powersnj:thragg.shockwave` | `sounds/thragg/shockwave.ogg` | `ShockwaveAbility` |
| `powersnj:thragg.heavy_punch` | `sounds/thragg/heavy_punch.ogg` | `HeavyPunchAbility` |
| `powersnj:thragg.flight_boost` | `sounds/thragg/flight_boost.ogg` | `FlightBoostAbility, ChargeAbility` |
| `powersnj:venom.tendril_shoot` | `sounds/venom/tendril_shoot.ogg` | `TendrilEntity` |
| `powersnj:venom.tendril_retract` | `sounds/venom/tendril_retract.ogg` | `TendrilEntity` |
| `powersnj:venom.symbiote_shield` | `sounds/venom/symbiote_shield.ogg` | `SymbioteShieldAbility, CombatEvents` |
| `powersnj:venom.symbiote_hiss` | `sounds/venom/symbiote_hiss.ogg` | `SymbioteEvents, SymbioteFormAbility` |
| `powersnj:reverse_flash.speed_start` | `sounds/reverse_flash/speed_start.ogg` | `SpeedsterMovementController` |
| `powersnj:reverse_flash.speed_stop` | `sounds/reverse_flash/speed_stop.ogg` | `SpeedsterMovementController` |
| `powersnj:reverse_flash.dash` | `sounds/reverse_flash/dash.ogg` | `SpeedDashAbility` |
| `powersnj:reverse_flash.phase` | `sounds/reverse_flash/phase.ogg` | `PhasingService` |
| `powersnj:reverse_flash.vortex` | `sounds/reverse_flash/vortex.ogg` | `VortexAbility` |

## 9. Opcionais do Palladium (sem código adicional)

- Textura personalizada da Ability Wheel: propriedade `"texture"` das habilidades `attack_wheel`, `tendril_wheel`, `speed_wheel` nos JSONs de poder (TextureReference do Palladium).
- Fundo da tela de poderes: campo `"background"` do poder; barra de habilidades: `"ability_bar_texture"`.
- Rastros/afterimages do speedster: `SpeedsterVisualHooks.register(...)` (código cliente) ou habilidade `palladium:trail` com definição de trail do Palladium.

## 10. Localização

`assets/powersnj/lang/en_us.json` e `pt_br.json` contêm todas as chaves usadas. O teste `ResourceConsistencyTest` falha se um idioma tiver chaves que o outro não tem
ou se o código/dados referenciarem uma chave inexistente.
