# EventTickets — App Android de Venda de Ingressos com Cielo Smart

App Android nativo em Kotlin para listagem de eventos, seleção de
quantidade de ingressos, pagamento integrado ao ecossistema **Cielo Smart**
e emissão de ingressos com QR Code, construído seguindo a arquitetura oficial
recomendada pelo Google (MVVM + Clean Architecture + Unidirectional Data
Flow, multi-módulo, 100% Jetpack Compose).

## Índice
- [Arquitetura](#arquitetura)
- [Módulos](#módulos)
- [Como executar](#como-executar)
- [Integração com a Cielo Smart](#integração-com-a-cielo-smart)
- [Testes](#testes)
- [Documentação do harness de IA](#documentação-do-harness-de-ia)

## Arquitetura

```
app (Single Activity, NavHost)
 ├── feature-events    (Fluxo 1 - listagem de eventos)
 ├── feature-checkout  (Fluxo 2 - seleção de quantidade)
 ├── feature-payment   (Fluxo 3/4 - pagamento Cielo + registro do resultado)
 ├── feature-receipt   (Fluxo 5 - comprovante + QR Code)
 ├── data              (Room, integração Cielo Smart, repositórios)
 ├── domain            (Kotlin puro: models, use cases, contratos)
 ├── core-ui           (tema Material 3, componentes compartilhados)
 └── core-common       (AppResult/AppError - tratamento explícito de erro)
```

- **MVVM**: cada tela tem um `ViewModel` (Hilt) expondo um único
  `StateFlow<UiState>` imutável; a UI (Compose) apenas observa e dispara
  intents de usuário.
- **Clean Architecture**: `domain` não depende de Android nem de nenhuma
  outra camada — é testável com JUnit puro. `data` implementa os contratos
  definidos em `domain`. `app` conecta tudo via Hilt.
- **Result explícito**: toda operação que atravessa uma borda de
  integração (rede, Cielo, banco) retorna `AppResult<T>` (sealed class),
  nunca lança exception silenciosa — ver `core-common/AppResult.kt`.

## Módulos

| Módulo | Responsabilidade |
|---|---|
| `:domain` | Models, interfaces de repositório, use cases (regra de negócio pura) |
| `:core-common` | `AppResult`/`AppError` |
| `:data` | Room (Event/Order/Ticket), integração Cielo Smart (Intent), repositórios |
| `:core-ui` | Tema Material 3, componentes Compose compartilhados |
| `:feature-events` | Tela de listagem de eventos |
| `:feature-checkout` | Tela de seleção de quantidade / criação do pedido |
| `:feature-payment` | Tela de pagamento (Cielo Smart) |
| `:feature-receipt` | Tela de comprovante + QR Code do ingresso |
| `:app` | `Application`, `MainActivity`, grafo de navegação |

## Como executar

### Pré-requisitos
- Android Studio Koala (2024.1.1) ou mais recente.
- JDK 17.
- Um dispositivo/emulador Android com API 24+.
- (Opcional, para testar a integração real) **Emulador Cielo Smart**,
  disponível em:
  https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo

### Passos
1. Clone/abra este diretório no Android Studio (`File > Open`, selecione a
   raiz `EventTickets/`).
2. Se o wrapper do Gradle (`gradlew`/`gradlew.bat`) não estiver presente por
   qualquer motivo, gere-o localmente com:
   ```bash
   gradle wrapper --gradle-version 8.9
   ```
   (requer o Gradle instalado globalmente apenas para esta etapa única).
3. Deixe o Android Studio sincronizar o projeto (`Sync Project with Gradle
   Files`).
4. Rode a configuração `app` no emulador/dispositivo de sua escolha:
   ```bash
   ./gradlew :app:installDebug
   ```
   ou use o botão "Run" do Android Studio.
5. O app abre diretamente na listagem de eventos (dados de demonstração
   pré-carregados, sem necessidade de backend).

### Executando com o emulador Cielo Smart (opcional)
Por padrão, o app usa um **gateway de pagamento simulado**
(`FakeCieloPaymentGateway`) para permitir demonstração completa do fluxo de
compra sem depender do terminal físico ou do emulador Cielo instalado no
ambiente de avaliação. Para usar a integração real via Deeplink:

1. Crie uma conta no [Portal de Desenvolvedores da Cielo](https://desenvolvedores.cielo.com.br/api-portal/)
   e obtenha seu `Client ID` e `Access Token`.
2. Coloque essas credenciais em `local.properties` (arquivo não versionado):
   ```properties
   CIELO_CLIENT_ID=seu_client_id_aqui
   CIELO_ACCESS_TOKEN=seu_access_token_aqui
   ```
3. Baixe o emulador/app Cielo Smart pelo Portal de Desenvolvedores e instale
   no mesmo dispositivo/AVD do app.

   ⚠️ **Atenção à versão do Android**: o emulador Cielo Smart só é compatível
   com **Android 7 ou Android 10 (API 24 ou API 29)**. Em versões mais
   recentes (Android 13+/API 33+), o app da Cielo trava com
   `SecurityException: One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED
   should be specified...` — isso é uma limitação do lado do app da Cielo
   (código não atualizado para a exigência do Android 13+ em
   `registerReceiver`), não do nosso app. Use um AVD com API 29 para testar.
4. Em `data/src/main/java/com/eventtickets/data/di/PaymentModule.kt`, altere:
   ```kotlin
   private const val USE_REAL_CIELO_GATEWAY = false
   ```
   para `true`.
5. Recompile e reinstale o app.

### Simulando diferentes desfechos no gateway fake
O `FakeCieloPaymentGateway` distribui resultados de forma determinística
pelo valor total da compra (em centavos), para facilitar testes manuais de
todos os desfechos exigidos pelo enunciado:
- Valor múltiplo de **777** centavos → pagamento **negado** (simulado).
- Valor múltiplo de **1313** centavos → **erro de integração** (simulado).
- Demais valores → pagamento **aprovado**.

## Integração com a Cielo Smart

A integração é feita via **Deeplink**, modelo recomendado pela Cielo para a
Cielo Smart (o SDK nativo embarcado usado pela antiga Cielo LIO foi
descontinuado nesse modelo de terminal).

### Como funciona
1. O app monta um JSON de pedido (`CieloDeeplinkPaymentRequest`), codifica em
   Base64 e monta uma URI `lio://payment?request=<base64>&urlCallback=order://response`.
2. Essa URI é disparada via `Intent(ACTION_VIEW, uri)` (`CieloPaymentBridge`),
   a partir da Activity ativa no momento do pagamento.
3. O app da Cielo Smart processa o pagamento e devolve o resultado ao nosso
   app via deeplink `order://response?response=<base64>`, capturado em
   `MainActivity.onNewIntent`.
4. `CieloDeeplinkParser` decodifica a resposta e diferencia sucesso (objeto
   com `payments[]`) de erro/cancelamento (objeto `{code, reason}`).

### Estrutura de arquivos (`data/remote/cielo`)
- `CieloDeeplinkContract.kt` — esquemas/hosts da URI (`lio://payment`, `order://response`)
- `CieloDeeplinkRequest.kt` — modelos de requisição (`CieloDeeplinkItem`, `CieloDeeplinkPaymentRequest`)
- `CieloDeeplinkOutcome.kt` — modelos de resposta (`CieloDeeplinkOutcome.Success/Failure/IntegrationError`)
- `CieloDeeplinkParser.kt` — serialização da requisição e parsing da resposta
- `CieloPaymentBridge.kt` — ponte entre a Activity (que dispara a Intent) e a
  função `suspend` usada pelos use cases
- `CieloSmartPaymentGateway.kt` — implementação de `PaymentGateway` usando o
  fluxo acima
- `FakeCieloPaymentGateway.kt` — gateway simulado para demo sem o app Cielo
  Smart instalado

A troca entre real e fake é feita em um único ponto:
`data/src/main/java/com/eventtickets/data/di/PaymentModule.kt`.

### Comportamento observado no ambiente sandbox
Em testes no sandbox/emulador da Cielo, campos como `brand` e `externalId`
retornam valores de teste literais (`"mock_brand"`, `"mock_externalId"`) em
vez de dados realistas — isso é esperado do ambiente de simulação da Cielo,
não um problema de parsing do app. Em um terminal físico real, esses campos
vêm preenchidos com a bandeira real do cartão e o ID de transação real.

Ver [ADR 0004](docs/adr/0004-integracao-deeplink-cielo-smart.md) para o
histórico completo dessa decisão (por que o modelo inicial via Intent
customizada foi substituído pelo Deeplink oficial).

## Testes

Testes automatizados cobrem os cenários críticos exigidos:

```bash
./gradlew test               # todos os testes unitários (todos os módulos)
./gradlew :domain:test        # regras de negócio (use cases)
./gradlew :data:test          # repositórios / deduplicação
./gradlew :feature-payment:test  # proteção de reenvio na UI
```

Cenários cobertos (ver arquivos `*Test.kt` correspondentes):
- **Anti-duplicidade de cobrança**: pedido `PROCESSING`/`APPROVED`/`DENIED`/
  `CANCELED` não aceita nova tentativa de pagamento; duplo-tap na UI só
  dispara uma chamada ao gateway.
- **Validação de quantidade**: quantidade abaixo do mínimo, acima do limite
  por pedido, ou acima do estoque disponível é rejeitada antes de qualquer
  chamada de rede.
- **Vínculo QR ↔ compra concluída**: `GenerateTicketsUseCase` só gera
  ingressos para pedidos `APPROVED`; qualquer outro status retorna falha.
- **Idempotência de emissão de ingressos**: reabrir a tela de comprovante não
  gera QR Codes duplicados/diferentes para o mesmo pedido.
- **Registro de todos os desfechos de pagamento**: aprovado, negado, erro de
  integração e retry após erro.

## Documentação do harness de IA

Toda a documentação exigida sobre o uso de IA no processo de construção
deste projeto — especificações, decisões arquiteturais (ADRs), prompts,
restrições impostas e resultados que orientaram a solução — está em:

- [`docs/harness/README.md`](docs/harness/README.md) — processo, prompts e
  restrições.
- [`docs/harness/specs.md`](docs/harness/specs.md) — especificação funcional
  detalhada e critérios de aceite.
- [`docs/adr/`](docs/adr/) — decisões arquiteturais individuais.
