# ADR 0004 — Integração via Deeplink com a Cielo Smart (substitui a Intent customizada inicial)

## Status
Aceito — substitui a abordagem original descrita nos comentários iniciais de
`CieloSmartContract.kt` (removido).

## Contexto
A primeira versão da integração com a Cielo Smart foi implementada como uma
`Intent` explícita disparada para um pacote `br.com.cielo.smart`, com nomes
de `action`/`extras` inferidos por analogia com outras integrações de POS
Android, sem confirmação contra a documentação oficial (que não estava
acessível no momento da primeira implementação).

Ao validar com a documentação real da Cielo (Portal de Desenvolvedores) e
com o repositório oficial de exemplo
(`DeveloperCielo/LIO-SDK-Sample-Integracao-Local`), ficou claro que o modelo
de integração é diferente:

- A Cielo Smart não expõe um app de terceiros escutando actions/extras
  livres; ela resolve uma **URI scheme própria** (`lio://payment`).
- O payload não vai em extras de Intent — vai em um **JSON serializado e
  codificado em Base64**, embutido como query param da própria URI.
- A resposta não volta por `onActivityResult`; volta como uma **nova Intent
  de deeplink** (`order://response`) que o app declara no Manifest e recebe
  via `onNewIntent`.
- O SDK nativo embarcado (usado pela antiga Cielo LIO) foi descontinuado
  para a Cielo Smart em favor desse modelo de Deeplink.

## Decisão
Reescrever a camada `data/remote/cielo` para o modelo de Deeplink:

- `CieloDeeplinkContract` substitui `CieloSmartContract` (esquemas/hosts em
  vez de actions/extras).
- `CieloDeeplinkRequest`/`CieloDeeplinkOutcome` (dados) e
  `CieloDeeplinkParser` (serialização/parsing) substituem os antigos
  `CieloSmartContract` (dados+constantes misturados) e `CieloPaymentContract`
  (`ActivityResultContract`, que não se aplica a esse modelo).
- `CieloPaymentBridge` passa a guardar uma `WeakReference<Activity>` e
  disparar `startActivity` diretamente, em vez de um
  `ActivityResultLauncher` registrado antecipadamente — porque o retorno não
  é um result code, é uma nova Intent capturada em `onNewIntent`.
- `MainActivity` ganha `android:launchMode="singleTask"` e um segundo
  `intent-filter` para o esquema `order://response`, além de anexar/desanexar
  o bridge em `onResume`/`onPause` (não só em `onCreate`/`onDestroy`, para
  cobrir corretamente o ciclo de vida quando o app volta de segundo plano
  após o usuário concluir o pagamento na Cielo Smart).

A interface `PaymentGateway` (em `domain`) não mudou — a troca de modelo
ficou inteiramente contida em `data`, confirmando que a inversão de
dependência entre `domain` e `data` cumpriu seu papel: uma mudança de
protocolo de integração externa não vazou para regra de negócio nem para
testes de use case.

## Consequências
- Testes de `domain` (`StartPaymentUseCaseTest` etc.) não precisaram de
  nenhuma alteração — validação de que o isolamento por interface funcionou
  na prática, não só na intenção.
- Foi necessário validar em campo (com token real e emulador oficial) que o
  emulador da Cielo Smart só funciona em Android 7 ou 10 (API 24/29); em
  API 33+ o próprio app da Cielo falha com
  `SecurityException: RECEIVER_EXPORTED/RECEIVER_NOT_EXPORTED` — uma
  limitação do lado deles, documentada para não ser confundida com bug do
  nosso app em avaliações futuras.
- Os campos de resposta (`brand`, `externalId`) vêm com valores de teste
  literais (`mock_brand`, `mock_externalId`) no ambiente sandbox da Cielo —
  comportamento do simulador deles, não do nosso parsing.
