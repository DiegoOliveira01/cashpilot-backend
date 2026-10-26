# 💰 Cash Pilot — Backend

> API REST para gerenciamento de finanças pessoais, construída com Java 21 e Spring Boot 4, com um **bot do Telegram** para lançar receitas e despesas por comandos ou texto livre e IA.

[![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0-green?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-blue?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Telegram](https://img.shields.io/badge/Telegram-Bot_API-229ED9?style=flat-square&logo=telegram)](https://core.telegram.org/bots/api)
[![Railway](https://img.shields.io/badge/Deploy-Railway-purple?style=flat-square&logo=railway)](https://cashpilot-backend-production.up.railway.app)

---

## 🔗 Links

| | |
|---|---|
| 🌐 **Frontend (Vercel)** | [cashpilot-frontend.vercel.app](https://cashpilot-frontend.vercel.app) |
| 📁 **Repositório Frontend** | [github.com/DiegoOliveira01/cashpilot-frontend](https://github.com/DiegoOliveira01/cashpilot-frontend) |
| 🤖 **Bot do Telegram** | [@CopilotoCashBot](https://t.me/CopilotoCashBot) (é preciso ter conta no sistema e conectá-la pelo dashboard) |
| 📄 **Swagger / Documentação** | [cashpilot-backend-production.up.railway.app/swagger-ui/index.html](https://cashpilot-backend-production.up.railway.app/swagger-ui/index.html) |
| 💼 **LinkedIn** | [linkedin.com/in/diego-oliveira-da-fonte](https://www.linkedin.com/in/diego-oliveira-da-fonte-2395b0223/) |
| 🗂️ **Portfólio** | [diegooliveira01.github.io](https://diegooliveira01.github.io) |

---

## 📸 Interface do Sistema

> O frontend consome esta API e está disponível em [cashpilot-frontend.vercel.app](https://cashpilot-frontend.vercel.app)


| Login | Dashboard |
|---|---|
| ![Login](https://github.com/DiegoOliveira01/cashpilot-backend/blob/503dbaf598858e330d6433700cc90159cd8b5476/Readme_img_login.png) | ![Dashboard](https://github.com/DiegoOliveira01/cashpilot-backend/blob/503dbaf598858e330d6433700cc90159cd8b5476/Readme_img_dashboard.png) |

---

## 🚀 Funcionalidades

- ✅ Cadastro e autenticação de usuários com **JWT**
- ✅ CRUD completo de transações financeiras
- ✅ Classificação por tipo: **Receita (INCOME)** e **Despesa (EXPENSE)**
- ✅ Resumo financeiro: total de receitas, despesas e saldo
- ✅ Senhas criptografadas com **BCrypt**
- ✅ Documentação interativa com **Swagger UI**
- ✅ Proteção de endpoints com **Spring Security**
- ✅ **Bot do Telegram** multiusuário: lançar receitas e despesas, ver saldo do mês, listar e desfazer lançamentos
- ✅ **Texto livre** ("gastei 40 no mercado ontem") com parser próprio por regras
- ✅ **Interpretação por IA opcional** (Gemini, camada gratuita) com consentimento por usuário e limites diários

---

## 🤖 Bot do Telegram

Além da interface web, é possível lançar transações pelo Telegram. Cada usuário conecta a própria conta pelo dashboard (botão **Conectar Telegram**, com link e QR Code) e passa a usar o bot.

### Comandos

| Comando | O que faz |
|---|---|
| `/gasto 35,90 mercado` | Registra uma despesa (a descrição é opcional) |
| `/receita 1500 salário` | Registra uma receita |
| `/saldo` | Resumo do mês atual (receitas, despesas e saldo) |
| `/ultimos` | Últimos 5 lançamentos |
| `/desfazer` | Remove o último lançamento criado |
| `/ia` | Explica a interpretação por IA e liga/desliga (`/ia on`, `/ia off`) |
| `/ajuda` | Lista os comandos |

Também é possível escrever livremente:

```
gastei 40 no mercado
recebi 1.500 de salário ontem
paguei 120 de luz dia 15
```

Sem data, o lançamento vai para hoje (fuso `America/Sao_Paulo`). Toda resposta mostra o que foi entendido e lembra do `/desfazer`.

### Como o texto livre é interpretado (em camadas)

```
Mensagem (não começa com "/")
   │
   ├─ 1️⃣ Parser por regras (TelegramFreeTextParser)
   │     local, sem custo e previsível. Se não tiver certeza, não chuta.
   │
   └─ 2️⃣ Gemini (opcional)
         só se a IA estiver habilitada no ambiente E o usuário aceitou (/ia on),
         respeitando os limites diários → resposta validada (AiExtraction)
```

- O parser entende verbos (*gastei, paguei, comprei, recebi, ganhei...*), valores (`R$ 40`, `40 reais`, `35,90`, `1.500,50`), datas (*hoje, ontem, anteontem, dia 15, 15/10*) e limpa a descrição.
- A resposta do modelo **nunca é confiável**: tipo, valor e data são validados antes de virar lançamento (valor positivo e plausível, data não futura e com no máximo 1 ano).

> ⚠️ **Aviso de privacidade (projeto de portfólio):** a IA usa a **camada gratuita** do Gemini, em que o Google pode usar o texto enviado para melhorar seus produtos e ele pode ser revisado por pessoas. Por isso a IA é **opt-in por usuário** (`/ia on`), só o texto da mensagem é enviado (sem nome, e-mail ou histórico) e não se deve digitar dados sensíveis. Sem o consentimento, nada sai do servidor.

### Fluxo de vinculação (chat ↔ usuário)

```
Dashboard (JWT) ── POST /telegram/link-code ──▶ código de uso único (10 min) + link t.me/bot?start=CODIGO
Usuário abre o link e toca em "Iniciar" ──▶ /start CODIGO ──▶ webhook ──▶ chat_id vinculado ao usuário
Dashboard consulta GET /telegram/status ──▶ botão vira "Telegram conectado ✓"
```

### Fluxo de uma mensagem

```
Telegram ── POST /telegram/webhook ──▶ TelegramController
                (header X-Telegram-Bot-Api-Secret-Token validado)
                          │
                          ▼
                 TelegramBotService ──▶ comando ou texto livre
                          │
                          ▼
        TransactionService.createForUser(...) ──▶ PostgreSQL
                          │
                          ▼
              TelegramClient.sendMessage(...) ──▶ resposta no chat
```

### Decisões técnicas

- **Webhook protegido por secret token**, comparado em tempo constante (`MessageDigest.isEqual`); a rota é pública no Spring Security, mas só aceita quem tem o secret.
- **Código de vinculação de uso único**, aleatório (`SecureRandom`) e com validade de 10 minutos.
- **Idempotência por `update_id`:** o Telegram reenvia updates quando a resposta demora (por exemplo, com o serviço acordando na hospedagem gratuita), e isso evita lançamentos duplicados ou um `/desfazer` apagando dois itens.
- **Sempre responde 200 ao Telegram**, mesmo com erro interno, para não gerar loop de reenvios. O erro vai para o log, sem conteúdo do usuário.
- **Fuso fixo `America/Sao_Paulo`:** o servidor roda em UTC, e sem isso lançamentos feitos à noite cairiam no dia seguinte.
- **`TransactionService.createForUser(user, dto)`:** o webhook não tem JWT, então o usuário vem do vínculo do chat, e as regras de negócio continuam no mesmo service usado pela API REST.
- **Só chats privados** são atendidos; grupos e canais são ignorados.
- **Controle de uso da IA:** consentimento por usuário, limite diário por usuário e global (abaixo da cota gratuita), timeouts curtos, chave de desligamento (`LLM_ENABLED`) e chave de API enviada em header, nunca em URL nem em log.

### Por que Telegram e não WhatsApp

O WhatsApp exige número dedicado e conta Meta Business (API oficial, com cobrança por mensagem em alguns cenários) ou clientes não oficiais com risco real de banimento. O Telegram tem API oficial gratuita, bot com comandos e webhook simples, ideal para um projeto de portfólio. A camada de serviço é a mesma, então trocar o canal de entrada seria só trocar o adaptador.

---

## 🛠️ Tecnologias

| Tecnologia | Uso |
|---|---|
| Java 21 | Linguagem principal |
| Spring Boot 4 | Framework base |
| Spring Security | Autenticação e autorização |
| JWT (jjwt 0.12.6) | Tokens de autenticação |
| Spring Data JPA | Acesso ao banco de dados |
| PostgreSQL | Banco de dados relacional |
| Hibernate | ORM |
| Lombok | Redução de boilerplate |
| Springdoc OpenAPI | Documentação Swagger |
| Spring `RestClient` | Chamadas à Telegram Bot API e à API do Gemini |
| Telegram Bot API | Bot de lançamentos (webhook) |
| Google Gemini API | Interpretação de texto livre (opcional, camada gratuita) |
| JUnit 5 | Testes unitários |
| Railway | Deploy em nuvem |

---

## 🔐 Fluxo de Autenticação JWT

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│   Frontend  │         │   Backend   │         │   Database  │
└──────┬──────┘         └──────┬──────┘         └──────┬──────┘
       │                       │                       │
       │  POST /auth/login     │                       │
       │  { email, password }  │                       │
       │──────────────────────>│                       │
       │                       │  SELECT user          │
       │                       │  WHERE email = ?      │
       │                       │──────────────────────>│
       │                       │<──────────────────────│
       │                       │  BCrypt.verify()      │
       │                       │  generateToken()      │
       │  { token: "eyJ..." }  │                       │
       │<──────────────────────│                       │
       │                       │                       │
       │  GET /transactions    │                       │
       │  Authorization: Bearer│                       │
       │──────────────────────>│                       │
       │                       │  JwtFilter validates  │
       │                       │  token on every req   │
       │  [ transactions ]     │                       │
       │<──────────────────────│                       │
```

---

## 📁 Estrutura do Projeto

```
src/main/java/com/diego/cashpilot/
│
├── controller/                 # Endpoints REST
│   ├── AuthController          # POST /auth/register, /auth/login
│   ├── TransactionController
│   ├── UserController
│   └── TelegramController      # link-code, status e webhook
│
├── service/                    # Regras de negócio
│   ├── UserService
│   ├── TransactionService
│   ├── TelegramBotService      # roteia comandos e texto livre
│   ├── TelegramCommandParser   # /gasto e /receita
│   ├── TelegramFreeTextParser  # frases livres por regras
│   ├── TelegramLinkService     # vínculo chat ↔ usuário e consentimento de IA
│   ├── TelegramClient          # chamadas à Bot API
│   ├── GeminiClient            # extração por IA (opcional)
│   ├── AiExtraction            # validação da resposta do modelo
│   └── AiUsageLimiter          # limites diários de uso da IA
│
├── repository/                 # Acesso ao banco (JPA)
│   ├── UserRepository
│   ├── TransactionRepository
│   └── TelegramLinkRepository
│
├── model/                      # Entidades JPA
│   ├── User
│   ├── Transaction
│   ├── TelegramLink
│   └── enums/
│       ├── Role
│       └── TransactionType
│
├── dto/                        # Objetos de transferência
│   ├── UserRequestDTO / UserResponseDTO
│   ├── LoginRequestDTO / AuthResponseDTO
│   ├── TransactionRequestDTO / TransactionResponseDTO / TransactionSummaryDTO
│   └── TelegramUpdate
│
├── security/                   # JWT e filtros
│   ├── JwtService
│   ├── JwtFilter
│   └── CustomUserDetailsService
│
└── config/
    └── SecurityConfig          # segurança e CORS

src/test/java/com/diego/cashpilot/service/
├── TelegramFreeTextParserTest
├── AiExtractionTest
└── AiUsageLimiterTest
```

---

## 📡 Endpoints da API

### Autenticação — público

| Método | Rota | Descrição |
|---|---|---|
| `POST` | `/auth/register` | Cadastrar novo usuário |
| `POST` | `/auth/login` | Autenticar e receber token JWT |

### Transações — requer token JWT

| Método | Rota | Descrição |
|---|---|---|
| `GET` | `/transactions` | Listar todas as transações do usuário |
| `GET` | `/transactions/{id}` | Buscar uma transação |
| `POST` | `/transactions` | Criar nova transação |
| `PUT` | `/transactions/{id}` | Atualizar transação |
| `DELETE` | `/transactions/{id}` | Deletar transação |
| `GET` | `/transactions/summary` | Resumo financeiro (receitas, despesas, saldo) |

### Telegram

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| `POST` | `/telegram/link-code` | JWT | Gera o código de vínculo (10 min) e o link do bot |
| `GET` | `/telegram/status` | JWT | Informa se a conta já está conectada ao Telegram |
| `POST` | `/telegram/webhook` | Header `X-Telegram-Bot-Api-Secret-Token` | Recebe os updates do Telegram |

### Exemplos de requisição

**Registro:**
```json
POST /auth/register
{
  "name": "Diego Oliveira",
  "email": "diego@email.com",
  "password": "123456"
}
```

**Login:**
```json
POST /auth/login
{
  "email": "diego@email.com",
  "password": "123456"
}
```

**Criar transação** (com Bearer token no header):
```json
POST /transactions
{
  "description": "Salário",
  "amount": 3000.00,
  "type": "INCOME",
  "date": "2026-04-01"
}
```

---

## ⚙️ Como rodar localmente

### Pré-requisitos

- Java 21+
- Maven
- PostgreSQL (ou Docker)

### Passo a passo

**1. Clone o repositório**
```bash
git clone https://github.com/DiegoOliveira01/cashpilot-backend.git
cd cashpilot-backend
```

**2. Suba um PostgreSQL**

Exemplo com Docker:
```bash
docker run --name cashpilot-db -e POSTGRES_PASSWORD=dev -e POSTGRES_DB=cashpilot -p 5432:5432 -d postgres
```

**3. Defina as variáveis de ambiente**

O `application.properties` lê tudo de variáveis de ambiente (nenhum segredo vai para o repositório):

| Variável | Obrigatória | Descrição |
|---|---|---|
| `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | Sim | Conexão com o PostgreSQL |
| `JWT_SECRET` | Sim | Chave de assinatura dos tokens (longa e aleatória) |
| `TELEGRAM_BOT_TOKEN` | Sim | Token do bot, criado no @BotFather |
| `TELEGRAM_BOT_USERNAME` | Sim | Username do bot, sem o `@` |
| `TELEGRAM_WEBHOOK_SECRET` | Sim | Texto aleatório (letras, números, `_` e `-`), o mesmo usado no `setWebhook` |
| `LLM_ENABLED` | Não | `true` liga a interpretação por IA (padrão: `false`) |
| `GEMINI_API_KEY` | Se IA ligada | Chave do Google AI Studio |
| `GEMINI_MODEL` | Se IA ligada | ID do modelo (ex.: um Flash-Lite com cota gratuita) |
| `LLM_DAILY_LIMIT_PER_USER` / `LLM_DAILY_LIMIT_GLOBAL` | Não | Limites diários de chamadas à IA (padrão: 20 e 200) |

Para rodar só a API, sem o bot, as três variáveis `TELEGRAM_*` aceitam qualquer valor. No IntelliJ, use *Run > Edit Configurations > Environment variables*.

**4. Rode a aplicação**
```bash
./mvnw spring-boot:run
```

A API estará disponível em `http://localhost:8080`

**5. Acesse o Swagger**
```
http://localhost:8080/swagger-ui/index.html
```

### Testando o bot de verdade

O Telegram só entrega webhooks para uma URL pública com HTTPS. Para testar o bot de ponta a ponta, use um bot de desenvolvimento (cada bot tem um único webhook) e um túnel para a porta 8080 (por exemplo, Cloudflare Tunnel), ou faça o deploy. A lógica do parser e da validação da IA é coberta por testes unitários e não precisa de Telegram.

---

## 🧪 Testes

```bash
./mvnw test
```

- `TelegramFreeTextParserTest`: frases que devem ser entendidas e frases ambíguas que devem ser recusadas (valores com milhar e vírgula, datas relativas, dia inexistente, data futura etc.).
- `AiExtractionTest`: a resposta do modelo é validada (tipo, valor, data e descrição) antes de virar lançamento.
- `AiUsageLimiterTest`: limites por usuário e globais, com virada de dia.

---

## 🌐 Deploy

A API está hospedada no **Railway** com banco de dados PostgreSQL dedicado.

**URL base:** `https://cashpilot-backend-production.up.railway.app`

**Swagger em produção:** [cashpilot-backend-production.up.railway.app/swagger-ui/index.html](https://cashpilot-backend-production.up.railway.app/swagger-ui/index.html)

As variáveis da tabela acima ficam em *Variables* do serviço no Railway (o serviço precisa reiniciar para lê-las). Depois do deploy, o webhook é registrado uma única vez:

```
https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://<DOMINIO-DO-BACKEND>/telegram/webhook&secret_token=<WEBHOOK_SECRET>
```

> Como o plano gratuito hiberna o serviço por inatividade, a primeira mensagem depois de um tempo parado pode demorar alguns segundos.

---

## 👨‍💻 Autor

**Diego Oliveira**

[![LinkedIn](https://img.shields.io/badge/LinkedIn-Diego_Oliveira-blue?style=flat-square&logo=linkedin)](https://www.linkedin.com/in/diego-oliveira-da-fonte-2395b0223/)
[![Portfolio](https://img.shields.io/badge/Portfólio-diegooliveira01.github.io-gray?style=flat-square&logo=github)](https://diegooliveira01.github.io)
