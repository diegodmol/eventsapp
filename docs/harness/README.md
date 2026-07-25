# Harness de IA — como o Claude foi usado neste projeto

Este documento registra como a IA foi usada na construção do app, o prompt
que guiou o trabalho, as restrições aplicadas e os principais pontos de
decisão ao longo do processo — conforme pedido no enunciado.

## Ferramenta e formato de trabalho

Usei o Claude (Anthropic), em uma sessão de chat com acesso a um ambiente de
execução (terminal, criação/edição de arquivo). O trabalho foi conduzido
arquivo por arquivo, seguindo a ordem real de dependência entre os módulos
(core-common → domain → data → core-ui → features → app), para poder validar
cada contrato antes de implementar quem depende dele. Depois da entrega
inicial, várias sessões de ajuste foram feitas em cima de problemas reais de
build e de integração com a Cielo Smart — parte considerável do valor do
processo de IA aqui não foi só gerar código, mas diagnosticar erros de
Gradle/toolchain/JDK e, principalmente, corrigir uma suposição errada sobre
como a integração com a Cielo funciona (ver seção 5).

## O pedido original

Em resumo, pedi um app Android em Kotlin usando a arquitetura oficial e boas
práticas, com integração ao ecossistema Cielo Smart. As funcionalidades
esperadas eram: listagem de eventos, seleção de quantidade de ingressos,
pagamento via Cielo e geração opcional de QR Code do ingresso. Os fluxos
obrigatórios eram visualizar eventos, selecionar quantidade, iniciar e
concluir o pagamento, registrar o resultado da compra (aprovada/negada/
cancelada) e exibir um comprovante.

Além disso, deixei claro que "boas práticas" não era um pedido genérico —
incluía pontos específicos que eu já sabia que costumam ser negligenciados
em projetos Android feitos às pressas:

- Arquitetura em camadas (Clean Architecture), não só MVVM de fachada.
- Evitar acoplamento entre módulos — cada camada só deveria conhecer
  contratos (interfaces), nunca implementação concreta de outra camada.
- Cuidado com vazamento de memória e ciclo de vida de Composables —
  nada de listeners/launchers presos além do tempo de vida da tela, e
  atenção especial ao ciclo de vida da Activity na integração com a Cielo
  (que envolve sair do app e voltar).
- Evitar recomposição desnecessária no Compose, por causa de performance —
  estado bem escopado, sem recriar objetos a cada recomposição.
- Um padrão de estado consistente (Unidirectional Data Flow) em todas as
  telas, sem exceção.

Nos requisitos não-funcionais pedi: tratamento explícito de erros de
integração e pagamento, evitar duplicidade de cobrança em reenvio de ação,
código organizado e de fácil manutenção, testes automatizados para os
cenários críticos, e documentação de como a IA foi usada. Deixei explícito
que a construção de um backend de apoio não seria avaliada. Como restrição
técnica, pedi um projeto executável com README claro e a documentação do
harness (specs, decisões arquiteturais, prompts, restrições, resultados).

## Restrições que apliquei durante a geração

1. Sem backend real. A fonte de eventos é simulada localmente
   (`EventRemoteDataSource`) e documentada como ponto de troca futuro — não
   fazia sentido gastar esforço nisso já que não seria avaliado.
2. `domain` tem que ser Kotlin puro, sem nenhum import de Android. Isso
   obriga a lógica crítica (idempotência de pagamento, validação de
   quantidade, vínculo entre QR e compra aprovada) a viver em use cases
   testáveis com JUnit puro, sem precisar de instrumentação nem emulador.
3. Nenhuma função de borda (rede, banco, integração externa) pode lançar
   exception silenciosa — tudo passa por `AppResult<T>`, e a UI é obrigada a
   tratar os casos de sucesso e falha via `when` exaustivo.
4. Idempotência de pagamento não pode depender de uma única camada. Pedi
   explicitamente pra IA pensar nos cenários de reenvio de ação (duplo tap,
   rotação de tela, timeout seguido de retry, processo morto e recriado)
   antes de escrever qualquer código, e distribuir a proteção em três
   pontos independentes: UI (ViewModel), use case e banco local.
5. Separação clara de módulos por responsabilidade — cada módulo de feature
   só pode depender de `domain`/`core-common`/`core-ui`, nunca de `data`
   diretamente (isso foi corrigido no meio do processo: `feature-payment`
   tinha ganhado uma dependência de `:data` sem necessidade, e foi removida
   assim que percebi o vazamento de camada).

## Decisões arquiteturais (ADRs)

Ver `docs/adr/` para o detalhe de cada uma:

| ADR | Decisão |
|---|---|
| 0001 | Idempotência de pagamento em três camadas (UI, use case, banco) |
| 0002 | Valores monetários sempre em centavos (`Long`), nunca `Double` |
| 0003 | QR Code sem assinatura criptográfica no MVP (limitação documentada) |
| 0004 | Migração da integração de Intent customizada (suposição inicial errada) para Deeplink oficial da Cielo Smart |

Fora das ADRs formais, algumas escolhas estruturais que vieram direto do
pedido de "arquitetura oficial":

- Multi-módulo Gradle (`domain`, `data`, `core-common`, `core-ui` e um
  módulo por feature) em vez de um único módulo `app` monolítico — reduz
  acoplamento e acelera build incremental.
- MVVM com UDF: cada tela expõe um único `StateFlow<UiState>` imutável; a
  UI nunca muda estado diretamente, só invoca funções do ViewModel.
- Hilt para injeção de dependência, com um módulo dedicado
  (`PaymentModule`) que centraliza a troca entre o gateway real da Cielo e
  o gateway fake de demonstração — um único ponto de decisão.
- Room como persistência local, já que backend estava fora do escopo. A
  query de deduplicação por `idempotencyKey` no `OrderDao` é a peça central
  por trás da ADR 0001.

## O ponto mais delicado: a integração com a Cielo Smart

Essa foi a parte que mais exigiu ida e volta, e vale registrar como
processo porque não saiu certa de primeira.

Na primeira versão, sem acesso à documentação oficial da Cielo no momento da
geração, montei uma integração baseada em `Intent` explícita com um pacote e
extras inventados por analogia com outras integrações de POS Android. Isso
era estruturalmente razoável (a interface `PaymentGateway` isolava bem a
implementação do resto do app), mas estava **factualmente errado** sobre
como a Cielo Smart realmente funciona.

Depois de eu criar conta no Portal de Desenvolvedores da Cielo e trazer o
link do repositório oficial de exemplo, ficou claro que o modelo real é
via Deeplink: o app monta um JSON, codifica em Base64, dispara uma URI
`lio://payment?...` e recebe a resposta de volta como uma nova Intent em
`order://response`. Isso é bem diferente de uma Intent com extras livres.

A parte boa: como a interface `PaymentGateway` já isolava a integração
externa desde o início, a correção ficou inteiramente contida no módulo
`data` — nenhum teste de `domain`, nenhum ViewModel e nenhuma tela precisou
mudar. Isso é a prova prática de que a inversão de dependência valeu a pena,
não só ficou bonita no diagrama.

Depois disso ainda apareceram problemas de ambiente que não tinham a ver com
o código: incompatibilidade de JVM toolchain entre módulos (Java 17 vs 21),
uma versão do `kotlinx.serialization` incompatível com o Kotlin do projeto,
e por fim um bug real do lado da própria Cielo — o app deles crasha em
Android 13+ porque o `registerReceiver` interno não foi atualizado para a
exigência de `RECEIVER_EXPORTED`/`RECEIVER_NOT_EXPORTED` do Android 13. Isso
só foi confirmado testando em um emulador com Android 10 (API 29), que é a
versão que a documentação da Cielo recomenda para testes — e nesse ambiente
o pagamento efetivamente funcionou, com QR Code gerado e vinculado à compra
aprovada.

## O que ficou como limitação conhecida

- Campos de resposta como `brand` e `externalId` vêm com valores de teste
  (`mock_brand`, `mock_externalId`) no ambiente sandbox da Cielo — isso é
  do simulador deles, não do parsing do app, e não deve ser mascarado no
  código (perderia rastreabilidade real de erro).
- QR Code não é assinado criptograficamente — aceitável sem backend, não
  para produção real (ver ADR 0003).
- Testes instrumentados (`androidTest`) não foram priorizados em favor de
  testes unitários (JUnit/MockK/Turbine), que cobrem os cenários críticos
  de forma mais rápida e determinística.
