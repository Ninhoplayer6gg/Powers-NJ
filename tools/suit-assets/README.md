# Pipeline de trajes (Blockbench → GeckoLib + animações)

Ferramentas para transformar o modelo de um personagem feito no Blockbench em:

| Saída | Caminho | Quem usa |
|---|---|---|
| Geometria de armadura GeckoLib | `assets/powersnj/geo/suits/<traje>.geo.json` | `SuitGeoModel` / `GeckoSuitRenderer` |
| Textura | `assets/powersnj/textures/suits/<traje>/<traje>_geo.png` | `SuitGeoModel` |
| Clipes de animação (formato Bedrock) | `assets/powersnj/animations/suits/<traje>.animation.json` | `SuitAnimationLibrary` |

O arquivo-fonte do Thragg é `art/suits/thragg/thragg.bbmodel` (modelo + textura + animações; abre direto no Blockbench).

```bash
python3 tools/suit-assets/export_suit.py art/suits/thragg/thragg.bbmodel --suit thragg   # exporta (sem dependências)
python3 tools/suit-assets/thragg_animations.py                                            # regera as animações do Thragg e exporta
python3 tools/suit-assets/lint_clips.py clips_thragg                                      # checagens (pés no chão, loops, joelhos...)
pip install numpy pillow                                                                  # só para as prévias
python3 tools/suit-assets/preview.py art/suits/thragg/thragg.bbmodel --out build/previews --gif
```

## Rig de traje (convenção obrigatória)

Coordenadas do Blockbench (pixels; o personagem olha para -Z, o lado **direito** dele é +X, pés em y = 0):

```
root            (0, 0, 0)       corpo inteiro (pose de renderização do jogador)
├─ head         (0, 24, 0)      = cabeça do jogador      → capacete
├─ body         (0, 24, 0)      = torso do jogador       → peitoral   (capa, tabardo... como filhos)
├─ right_arm    (5, 22, 0)      = braço direito          → peitoral   (right_forearm como filho)
├─ left_arm     (-5, 22, 0)     = braço esquerdo         → peitoral   (left_forearm como filho)
├─ right_leg    (1.9, 12, 0)    = perna direita          → calças     (subárvore right_shin → botas)
└─ left_leg     (-1.9, 12, 0)   = perna esquerda         → calças     (subárvore left_shin → botas)
```

* `head`, `body`, braços e pernas são **irmãos**, exatamente como as partes do `HumanoidModel` do Minecraft. Por isso o que
  se vê no Blockbench é o que aparece no jogo.
* Qualquer outro grupo é um **osso do traje** (capa, antebraço, canela, tabardo...), animado localmente.
* `rig_humanoid.py` converte um modelo humanoide comum (grupos `head`, `body`, `right_arm`... aninhados de qualquer jeito)
  para este rig: `python3 tools/suit-assets/rig_humanoid.py entrada.bbmodel saida.bbmodel --texture skin.png --name venom`.
* O exportador valida o rig, gera os ossos `armorHead/armorBody/...` que o `GeoArmorRenderer` exige, separa calças/botas,
  compensa o deslocamento de 0,1 px que o GeckoLib aplica às pernas e adiciona uma casca de 0,25 px por lado para o traje
  nunca brigar (z-fighting) com a skin quando só algumas peças estão vestidas. Faces `up`/`down` têm o UV girado como o
  Bedrock espera. Só cubos são suportados (sem meshes).

## Animações

Valores no padrão Bedrock/Blockbench (graus e pixels), iguais aos `ModelPart` do Minecraft:

| Osso | Convenção |
|---|---|
| `root` | x+ inclina para frente (pivô nos pés), y+ gira para a direita, z+ rola para a esquerda; posição y+ sobe, z+ vai para trás |
| `body` | x+ curva para frente (pivô no pescoço: as pernas precisam acompanhar o quadril) |
| `head` | x+ olha para baixo — **somado** à direção em que o jogador olha (exceto nos estados de voo rápido) |
| braços | x- levanta para frente; direito z+ / esquerdo z- abre para o lado |
| pernas | x- para frente; direita z+ / esquerda z- abre |
| `*_forearm` / `*_shin` | x- dobra o cotovelo / x+ dobra o joelho |

Interpolação: `linear` ou `catmullrom` (Catmull-Rom uniforme como no Bedrock; use o mesmo modo em todas as chaves de um
canal). Keyframes "split" (pre/post) funcionam. Molang não é suportado (o runtime só lê constantes).

### Papéis (arquivo `assets/powersnj/animation_sets/<traje>.json`)

| Papel | Tipo | Quando toca |
|---|---|---|
| `idle` / `walk` / `run` | loop | parado / andando / correndo. **walk e run seguem a fase do balanço de pernas vanilla: em t = 0 a perna direita está toda para trás** |
| `crouch` / `guard` | loop | agachado / agachado em combate (até `combat_window` s depois de bater ou apanhar); some para o vanilla ao andar agachado |
| `jump` | hold | ao pular (última pose mantida no ar); também ao cair de um degrau |
| `takeoff` | once | quando o voo começa perto do chão |
| `hover` / `fly` / `fast_flight` | loop | voo parado / voo rápido do Palladium (sprint) / modo boost ou alta velocidade. Em `fly` e `fast_flight` a inclinação do olhar e a curva (banking) são somadas em volta do centro da hitbox |
| `land` | once | ao tocar o chão depois de voar ou de uma queda longa |
| `melee.chain` | once | socos com a mão vazia, alternando (`chain_window` s para continuar a sequência) |
| `melee.after_sprint` | once | soco com a mão vazia logo depois de correr |
| `events.*` | once | `heavy_hit` (dano ≥ 8), `level_up`, `kill` (jogador ou boss) — enviados pelo servidor |
| propriedade `animation` das habilidades | once | quando a habilidade ativa (enviado pelo servidor para todos que veem o jogador) |

Durante ações, os ossos de `lower_body` continuam com a locomoção se o jogador estiver se movendo (as pernas não
"deslizam"). Braços ocupados com itens (arco, escudo, comer, golpe com item) ficam com o vanilla.

### Ataques sincronizados com o servidor

O servidor aplica o efeito da habilidade no momento do clique, então o **impacto do clipe deve acontecer até ~0,14 s**;
o peso vem da continuação e da recuperação.

## Thragg

Os clipes do Thragg são gerados por `clips_thragg/` (um módulo por grupo: locomoção, voo, combate, poderes) com poses
completas por keyframe e `ground()` para manter os pés no chão. Dá para editar tanto no Python quanto direto no
Blockbench (depois exporte com `export_suit.py`; rodar `thragg_animations.py` de novo sobrescreve as animações do
`.bbmodel`).
