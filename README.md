# Powers NJ

Mod de super-heróis para **Minecraft Java 1.20.1 / Forge 47 / Java 17**, construído sobre o **Palladium** (núcleo de poderes)
com **GeckoLib** para animações. Fluxo de jogo:

```
traje equipado (4 peças) → Power Set (poder Palladium) → passivas → Ability Wheel / barra → habilidades → progressão (nível 1–20, skill tree)
```

Personagens iniciais: **Thragg**, **Venom** e **Reverse-Flash**.

## Dependências

| Mod | Versão | Papel |
|---|---|---|
| Minecraft Forge | 47.3.0+ (1.20.1) | loader |
| Palladium (ThreeTAG) | 4.5.4+ (`net.threetag:palladium-forge`, inclui PalladiumCore) | habilidades, teclas, barra, Ability Wheel, suit sets, tela de poderes |
| GeckoLib | 4.4.4+ (`software.bernie.geckolib:geckolib-forge-1.20.1`) | modelos/animações de trajes e tentáculos |

O Palladium **não é modificado**: Powers NJ registra tipos de habilidade, condições e suit sets nos registries públicos do Palladium
e define os poderes por dados (`data/powersnj/palladium/...`).

## Build

```bash
./gradlew build            # compila, roda os testes JUnit do core e gera o jar
./gradlew runClient        # cliente de desenvolvimento
./gradlew runGameTestServer  # roda os GameTests in-game e encerra
```

Jar final: `build/libs/powersnj-1.20.1-0.1.0.jar` (reobfuscado pelo ForgeGradle). Coloque-o em `mods/` junto com Palladium e GeckoLib.

O build precisa acessar: `maven.minecraftforge.net`, `piston-meta.mojang.com` / `piston-data.mojang.com` / `libraries.minecraft.net`,
`maven.threetag.net`, `dl.cloudsmith.io` (GeckoLib), Maven Central e o Gradle Plugin Portal. Java 17 é provisionado pelo toolchain
(foojay) se não estiver instalado.

### Verificação sem acesso aos mavens do Minecraft

`tools/core-verification` é um build Gradle independente que compila e testa **apenas** o domínio puro (`com.powersnj.core`),
usando só Maven Central:

```bash
cd tools/core-verification && gradle test
```

## Arquitetura

```
com.powersnj
├── PowersNJ.java          entrada do mod (só wiring)
├── core/                  DOMÍNIO PURO (sem Minecraft) — testado por JUnit
│   ├── ability/           AbilityGate (validação servidor), CooldownTracker
│   ├── combat/            CombatRules (PvP/PvE/boss)
│   ├── config/            PowersSettings (snapshot imutável da config)
│   ├── data/              DataNode (persistência neutra ↔ NBT)
│   ├── destruction/       DestructionTier, DestructionClassifier, DestructionPlanner
│   ├── energy/            EnergyType, EnergyPool, EnergyBank, EnergySpec
│   ├── fabrication/       FabricationSpec/Matcher (Suit Forge)
│   ├── flight/            FlightController (genérico, não exclusivo do Thragg)
│   ├── net/               payloads de rede (ByteBuf, compatíveis com FriendlyByteBuf)
│   ├── phasing/           PhasePlanner (trajeto seguro, anti-preso)
│   ├── progression/       LevelCurve, SuitProgress, ProgressionLedger, XpRules
│   ├── skill/             SkillTree, SkillNode, SkillRequirement
│   ├── speedster/         SpeedsterEngine (aceleração, curvas, colisão, validação)
│   └── suit/              SuitDefinition + parser JSON
├── registry/              DeferredRegisters (itens, blocos, BEs, menus, sons, partículas, aba, efeitos, entidades, receitas, loot, worldgen)
├── config/                PowersServerConfig (ForgeConfigSpec → PowersSettings)
├── suit/                  SuitKind/SuitKinds, SuitArmorItem (GeckoLib-ready), detector, loader de definições
├── player/                capability PowersPlayerData (progressão, energia, cooldowns, estado) + ciclo de vida
├── power/                 PowerManager (tick servidor), PowerSystems (regeneração...), ServerTasks
├── ability/               AbilityExecutor (gate → efeito → custo → XP → feedback)
├── progression/ skill/    ProgressionAPI (addSuitXp/getSuitLevel/unlockSkill/isSkillUnlocked), fontes de XP, SkillService
├── combat/ destruction/   CombatService, Targeting, DestructionEngine + DestructionScheduler (blocos por tick)
├── movement/ flight/ speedster/ symbiote/ phasing/   controladores de movimento e sistemas especiais
├── network/               canal + pacotes (sync incremental, sem spam por frame)
├── block/ menu/ recipe/   Suit Forge, Suit Stand, receita powersnj:suit_fabrication
├── world/                 placement configurável, loot modifiers
├── client/ hud/ render/ animation/   telas, HUD API, renderers, animações
├── compat/palladium/      tipos de habilidade + condições + suit sets do Palladium
├── compat/geckolib/       modelos/renderers GeckoLib (ativam sozinhos quando os assets existirem)
├── command/               /powersnj (admin/debug)
└── gametest/              GameTests in-game
```

### Data-driven

| O quê | Onde |
|---|---|
| Definição do traje (rating, níveis, energia, movimento, sistemas, skill tree, HUD, XP) | `data/powersnj/powersnj/suits/<traje>.json` |
| Habilidades/passivas/teclas/Ability Wheel (Palladium) | `data/powersnj/palladium/powers/<traje>.json` |
| Traje completo → poder | `data/powersnj/palladium/suit_set_powers/<traje>.json` |
| Receita da Suit Forge | `data/powersnj/recipes/suit_forge/<traje>.json` |
| Classes de destruição | tags `powersnj:destruction/{fragile,normal,hard,extreme,protected}` |
| Blocos impossíveis de atravessar | tag `powersnj:phase_proof` |
| Loot (blueprints raros, amostras, fragmentos; tabelas prontas para estruturas/desafios/bosses) | `data/powersnj/loot_tables/{rewards,chests,challenges,entities}`, `data/powersnj/loot_modifiers` |

### Tipos de habilidade Palladium registrados por Powers NJ

`powersnj:trait`, `scaled_attribute`, `ground_slam`, `shockwave`, `heavy_punch`, `charge`, `flight_boost`, `high_speed_flight`,
`tendril`, `symbiote_shield`, `symbiote_form`, `camouflage`, `super_speed`, `speed_level`, `speed_dash`, `speed_punch`,
`rapid_attack`, `phase`, `vortex`.

Propriedades comuns: `energy_cost`, `energy_per_tick`, `cooldown_ticks` (escalado por `cooldownMultiplier`), `required_skill`,
`min_level`, `xp_reward`.

Condições: `powersnj:skill_unlocked`, `powersnj:suit_level`, `powersnj:has_energy`, `powersnj:speedster_speed`, `powersnj:symbiote_stable`.

## Controles

| Ação | Tecla |
|---|---|
| Ability Wheel / barra de habilidades | teclas de habilidade do Palladium (configuráveis em Controles) |
| Voar (Thragg) | pular duas vezes (voo do Palladium) |
| Nível de velocidade (Reverse-Flash, com Super Speed ativa) | roda do mouse |
| Árvore de habilidades | **K** |
| Mostrar/ocultar HUD de poderes | configurável (sem tecla padrão) |

## Roteiro do primeiro milestone (em jogo)

1. Abra a aba criativa **Powers NJ** (ou obtenha materiais por mineração/loot).
2. Coloque uma **Suit Forge**, abra a GUI, insira o **Blueprint**, o **Power Core** e os materiais mostrados (verde = suficiente).
3. Clique **Fabricate** → as 4 peças aparecem nos slots de saída.
4. Coloque um **Suit Stand**, clique com uma peça para guardar/exibir; clique com a mão vazia para vestir/trocar o traje inteiro.
5. Com as 4 peças, o Power Profile ativa (mensagem + HUD: nível, XP, energia).
6. Ganhe XP (abates, dano, uso de habilidades, deslocamento) ou use `/powersnj xp add @s 500`.
7. Pressione **K** e desbloqueie a primeira skill (nível 1 já dá 1 ponto).
8. Habilidades funcionais sem skill: soco pesado + voo (Thragg), super velocidade (Reverse-Flash);
   primeira skill: Ground Slam / Tendril Grab / Speed Punch.
9. Salve, saia e entre de novo: progressão, energia e cooldowns persistem (capability do jogador).

Comandos (op): `/powersnj info`, `/powersnj suit give <jogador> <traje>`, `/powersnj xp add`, `/powersnj level set`,
`/powersnj skill unlock|reset`, `/powersnj energy fill`.

## Config do servidor (`serverconfig/powersnj-server.toml`)

`enablePvP`, `pvpDamageMultiplier`, `pveDamageMultiplier`, `bossDamageMultiplier`, `enableDestruction`, `maxDestroyedBlocksPerAttack`,
`maxDestroyedBlocksPerTick`, `allowObsidianDestruction`, `xpMultiplier`, `maxSpeedMultiplier`, `energyRegenerationMultiplier`,
`cooldownMultiplier`, `enablePhasing`, `maxPhaseDistance`, `enableVenomWeaknesses`, `worldgenEnabled`, `viltrumiteVeinsPerChunk`,
`speedCrystalVeinsPerChunk`.

`keepInventory`: trajes são armadura comum — seguem a gamerule vanilla (mantidos com `true`, dropados com `false`).
Progressão/energia ficam no jogador e nunca se perdem.

## Adicionar um novo personagem

1. `SuitKinds`: uma linha `register(new SuitKind(...))` (gera as 4 peças, o suit set do Palladium e a entrada da aba).
2. `data/powersnj/powersnj/suits/<nome>.json` (definição + skill tree).
3. `data/powersnj/palladium/powers/<nome>.json` + `suit_set_powers/<nome>.json` (habilidades, reutilizando os tipos acima).
4. `data/powersnj/recipes/suit_forge/<nome>.json` + blueprint (item).
5. Traduções e assets conforme `ASSET_REQUIREMENTS.md`.

Novas mecânicas: `MovementControllers.register(...)`, `PowerSystems.register(...)`, novos tipos em `compat/palladium/ability`.

## Testes

- **JUnit** (`src/test/java/com/powersnj/core`): progressão, skill tree, energia, gate de habilidades, serialização (DataNode/NBT),
  serialização de rede (byte-compatível com `FriendlyByteBuf`), classificação/planejamento de destruição, receita da Suit Forge,
  SpeedsterEngine, FlightController, phasing, combate, parser de definições, isolamento do core e consistência de recursos
  (traduções en/pt, ícones, modelos, sons, partículas, skills referenciadas, itens referenciados nos dados).
- **GameTests** (`com.powersnj.gametest`): registries, fabricação real na Suit Forge, recusa sem blueprint, Suit Stand + NBT/sync,
  persistência da capability, classificação de destruição em blocos reais.

## Assets

Ver **[ASSET_REQUIREMENTS.md](ASSET_REQUIREMENTS.md)**. Placeholders: `python3 tools/placeholder-assets/generate_placeholders.py`.
