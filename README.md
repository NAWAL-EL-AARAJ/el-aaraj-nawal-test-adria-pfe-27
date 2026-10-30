# Test ADRIA PFE 2027 : plateforme bancaire micro-services

Épreuve technique de sélection ENSET Mohammedia × ADRIA Business & Technology : architecture micro-services, IA et développement full-stack.

La plateforme gère des **comptes bancaires** et des **transactions** (paiements, virements). Elle publie un **événement Kafka** à chaque transaction et propose un **assistant IA**, accessible par API REST, depuis le client Angular ou sur Telegram, qui agit sur les comptes via MCP.

**Rapport complet : [docs/rapport.pdf](docs/rapport.pdf)**. Les captures sont dans [captures/](captures/).

## Technologies

Java 21 · Maven multi-modules · Spring Boot 4.1.2 · Spring Cloud 2025.1.4 (Gateway, Eureka, Config, OpenFeign) · Resilience4J · Spring for GraphQL · Spring AI 2.0.1 (LLM Groq, MCP) · Apache Kafka · Angular 22 + Angular Material · Docker Compose

## Architecture

![Architecture](docs/images/architecture.png)

## Modules et ports

| Module | Port | Rôle |
|---|---|---|
| discovery-service | 8761 | Annuaire Eureka |
| config-service | 8888 | Configuration centralisée (`config-repo/`) |
| gateway-service | 8080 | Point d'entrée unique, CORS pour le front |
| account-service | 8081 | Comptes : REST, GraphQL, MCP |
| transaction-service | 8082 | Transactions : REST, GraphQL, MCP, Feign et Resilience4J, producteur Kafka |
| notification-service | 8083 | Consommateur Kafka |
| ai-agent-service | 8084 | Agent Spring AI (`/api/chat`) et bot Telegram |
| Kafka / Kafka UI | 9092 / 8090 | Messagerie (`docker-compose-infra.yml`) |
| frontend | 4200 | Client web Angular |

Chaque service métier est organisé en `domain/`, `repository/`, `service/`, `web/rest/`, `web/graphql/` et `mcp/`. REST, GraphQL et MCP appellent la même couche service.

## Lancement

Prérequis : Java 21, Node.js, Docker Desktop.

```bash
# 1. Infrastructure Kafka
docker compose -f docker-compose-infra.yml up -d

# 2. Services (dans cet ordre, un terminal chacun)
./mvnw -pl discovery-service spring-boot:run
./mvnw -pl config-service spring-boot:run
./mvnw -pl account-service spring-boot:run
./mvnw -pl transaction-service spring-boot:run
./mvnw -pl notification-service spring-boot:run
./mvnw -pl ai-agent-service spring-boot:run
./mvnw -pl gateway-service spring-boot:run

# 3. Client web
cd frontend
npm install
npx ng serve        # http://localhost:4200
```

## Variables d'environnement

À définir sur la machine (modèle : [.env.example](.env.example)), jamais dans un fichier commité :

| Variable | Rôle |
|---|---|
| `GROQ_API_KEY` | clé du LLM (API compatible OpenAI) |
| `LLM_MODEL` | optionnel : modèle utilisé (`openai/gpt-oss-120b` par défaut) |
| `TELEGRAM_BOT_TOKEN` | token du bot Telegram ; sans lui, le bot est désactivé |
| `TELEGRAM_BOT_USERNAME` | nom du bot |

## Endpoints principaux (via la gateway)

| Méthode | URL | Description |
|---|---|---|
| GET / POST | `/api/accounts` | liste / création de comptes |
| PUT | `/api/accounts/{id}/block` · `/activate` | bloquer / activer un compte |
| GET / POST | `/api/transactions` | liste / nouvelle transaction |
| POST | `/api/transactions/transfer` | virement entre deux comptes |
| POST | `/api/chat` | assistant IA `{conversationId, message}` |
| POST | `/graphql/accounts` · `/graphql/transactions` | GraphQL |

Les scénarios de test sont décrits dans [docs/tests.md](docs/tests.md).

## Auteur

**Nawal EL AARAJ**, cycle ingénieur S5, ENSET Mohammedia, 2026/2027
