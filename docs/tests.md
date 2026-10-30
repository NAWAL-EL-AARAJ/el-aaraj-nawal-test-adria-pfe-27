# Tests manuels : transaction-service, Feign et Resilience4J

## Prérequis

Démarrer dans l'ordre :

1. `discovery-service` (8761)
2. `config-service` (8888)
3. `account-service` (8081)
4. `transaction-service` (8082)

Au démarrage, account-service crée 3 comptes et affiche leurs ids dans les logs :

```
Compte EUR actif  (5000 EUR) : <EUR>
Compte MAD actif  (3000 MAD) : <MAD>
Compte EUR BLOCKED (1000 EUR) : <BLOCKED>
```

La base H2 est en mémoire : **les ids changent à chaque démarrage**. Les définir dans le terminal (Git Bash) :

```bash
EUR=<id du compte EUR actif>
MAD=<id du compte MAD actif>
BLOCKED=<id du compte EUR bloqué>
A=http://localhost:8081/api/accounts
T=http://localhost:8082/api/transactions
```

> Les mêmes appels passent par la gateway en remplaçant `8081` / `8082` par `8080`.

---

## 1. Transaction valide

```bash
curl -s $A/$EUR
curl -s -X POST $T -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$EUR\",\"amount\":250,\"currency\":\"EUR\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}"
curl -s $A/$EUR
```

**Attendu :** `201 Created`, une transaction `type: DEBIT`, puis le solde du compte EUR passe de **5000.0 à 4750.0** côté account-service.

## 2. Montant supérieur au maximum (1000)

```bash
curl -s -X POST $T -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$MAD\",\"amount\":1500,\"currency\":\"MAD\",\"merchantCategory\":\"GROCERY\",\"country\":\"MA\"}"
```

**Attendu :** `422` - `Le montant 1500 dépasse le maximum autorisé (1000)`.

Le maximum vient de `config-repo/application.properties` (`bank.transaction.max-amount`). Après l'avoir modifié, le recharger sans redémarrer :

```bash
curl -s -X POST http://localhost:8082/actuator/refresh
```

**Attendu :** `["bank.transaction.max-amount"]`.

## 3. Montant négatif

```bash
curl -s -X POST $T -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$EUR\",\"amount\":-50,\"currency\":\"EUR\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}"
```

**Attendu :** `422` - `Le montant doit être strictement positif`.

## 4. Compte bloqué

```bash
curl -s -X POST $T -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$BLOCKED\",\"amount\":10,\"currency\":\"EUR\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}"
```

**Attendu :** `422` - `Le compte <BLOCKED> n'est pas actif (statut : BLOCKED)`.

## 5. Mauvaise devise

```bash
curl -s -X POST $T -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$EUR\",\"amount\":10,\"currency\":\"MAD\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}"
```

**Attendu :** `422` - `Devise MAD différente de celle du compte <EUR> (EUR)`.

## 6. Solde insuffisant

Le compte MAD a 3000 MAD : trois paiements de 1000 vident le compte, le quatrième est refusé.

```bash
for i in 1 2 3 4; do
  curl -s -X POST $T -H "Content-Type: application/json" \
    -d "{\"accountId\":\"$MAD\",\"amount\":1000,\"currency\":\"MAD\",\"merchantCategory\":\"GROCERY\",\"country\":\"MA\"}"
  echo
done
```

**Attendu :** trois `201`, puis `422` - `Solde insuffisant sur le compte <MAD> (solde : 0.0, montant demandé : 1000)`.

## 7. Compte inexistant

```bash
curl -s -X POST $T -H "Content-Type: application/json" \
  -d '{"accountId":"00000000-0000-0000-0000-000000000000","amount":10,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}'
```

**Attendu :** `404` - `Compte introuvable : 00000000-...`. Une erreur métier n'est **pas** comptée comme un échec par le circuit breaker.

## 8. Données invalides

```bash
curl -s -X POST $T -H "Content-Type: application/json" -d '{"amount":10}'
```

**Attendu :** `400` - `Données invalides`, avec le détail des champs manquants (`accountId`, `currency`, `merchantCategory`, `country`).

## 9. Virement entre deux comptes EUR

Un seul compte EUR est actif au démarrage : en créer un second.

```bash
EUR2=$(curl -s -X POST $A -H "Content-Type: application/json" \
  -d '{"customerReference":"CUST-004","currency":"EUR","balance":200}' | sed 's/.*"id":"\([^"]*\)".*/\1/')

curl -s -X POST $T/transfer -H "Content-Type: application/json" \
  -d "{\"fromAccountId\":\"$EUR\",\"toAccountId\":\"$EUR2\",\"amount\":300,\"currency\":\"EUR\"}"

curl -s $A/$EUR
curl -s $A/$EUR2
curl -s $T/account/$EUR
```

**Attendu :**
- `201` avec deux transactions `TRANSFER` : un `DEBIT` sur `EUR` et un `CREDIT` sur `EUR2` ;
- le compte source perd 300 (4750.0 → 4450.0) et le compte destination gagne 300 (200.0 → 500.0) ;
- l'historique du compte `EUR` liste le virement puis le paiement de l'étape 1.

## 10. Fallback et circuit breaker (account-service arrêté)

Configuration (`config-repo/transaction-service.properties`) :
- fenêtre glissante de **10** appels, calcul à partir de **5** appels ;
- circuit ouvert à **50 %** d'échecs, pendant **10 s** ;
- **3** tentatives (retry) sur la lecture du compte uniquement.

Arrêter account-service, puis lancer quelques transactions :

```bash
for i in 1 2 3 4 5; do
  curl -s -w "  [HTTP %{http_code} en %{time_total}s]\n" -X POST $T -H "Content-Type: application/json" \
    -d "{\"accountId\":\"$EUR\",\"amount\":10,\"currency\":\"EUR\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}"
  curl -s http://localhost:8082/actuator/circuitbreakers; echo
done
curl -s http://localhost:8082/actuator/health
```

**Attendu :**
- chaque appel renvoie `503` - `Service compte indisponible, réessayez plus tard` ;
- appels 1 et 2 : quelques secondes (3 tentatives chacun), le circuit passe de `CLOSED` à **`OPEN`** ;
- appels suivants : réponse immédiate (~10 ms), sans appeler account-service (`notPermittedCalls` augmente) ;
- `/actuator/health` : composant `circuitBreakers` à `CIRCUIT_OPEN` avec l'état détaillé (le service reste joignable : HTTP 200).

Redémarrer account-service, attendre ~30 s (enregistrement dans Eureka), récupérer le nouvel id `EUR` dans ses logs, puis relancer une transaction :

```bash
curl -s -X POST $T -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$EUR\",\"amount\":10,\"currency\":\"EUR\",\"merchantCategory\":\"GROCERY\",\"country\":\"FR\"}"
curl -s http://localhost:8082/actuator/circuitbreakerevents
```

**Attendu :** `201`, et les transitions `CLOSED → OPEN`, `OPEN → HALF_OPEN` (après 10 s), puis `HALF_OPEN → CLOSED` ; `/actuator/health` repasse à `UP`.

## Endpoints utiles

| URL | Rôle |
|---|---|
| `GET http://localhost:8082/actuator/health` | santé du service et état des circuit breakers |
| `GET http://localhost:8082/actuator/circuitbreakers` | état et métriques du circuit `accountService` |
| `GET http://localhost:8082/actuator/circuitbreakerevents` | historique des appels et des transitions |
| `GET http://localhost:8082/actuator/retries` | configuration du retry |
| `POST http://localhost:8082/actuator/refresh` | recharge la configuration depuis config-service |

---

## 11. Kafka : événement TransactionCreated

Démarrer Kafka et Kafka UI (Docker Desktop doit être lancé) :

```bash
docker compose -f docker-compose-infra.yml up -d
```

Puis démarrer `notification-service` (8083) en plus des services précédents. Au démarrage, son log affiche :

```
notification-group: partitions assigned: [bank.transactions.created-0, bank.transactions.created-1, bank.transactions.created-2]
```

Créer une transaction :

```bash
curl -s -X POST $T -H "Content-Type: application/json" \
  -d "{\"accountId\":\"$EUR\",\"amount\":75,\"currency\":\"EUR\",\"merchantCategory\":\"RESTAURANT\",\"country\":\"FR\"}"
```

**Attendu :**
- `201 Created` ;
- log de **transaction-service** :
  `Événement <eventId> publié dans bank.transactions.created (clé=<EUR>, partition=0, offset=0)` ;
- log de **notification-service** :
  `Événement reçu : TransactionCreatedEvent[...] | clé=<EUR> | partition=0 | offset=0`
  `Notification : transaction <transactionId> de 75 EUR sur le compte <EUR>`.

Message JSON dans le topic (clé = `accountId`) :

```bash
MSYS_NO_PATHCONV=1 docker exec kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic bank.transactions.created \
  --from-beginning --property print.key=true --timeout-ms 5000
```

```json
{"eventId":"...","eventType":"TransactionCreated","transactionId":"...","accountId":"<EUR>",
 "amount":75,"currency":"EUR","occurredAt":"2026-10-30T11:11:08.282885800Z"}
```

L'événement est envoyé **après le commit** : une transaction refusée (règle métier, 4xx) ne produit aucun message.
Les messages sont aussi visibles dans Kafka UI : http://localhost:8090 → Topics → `bank.transactions.created`.

---

## 12. Agent IA : API /api/chat

Prérequis : `discovery-service`, `config-service`, `account-service`, `transaction-service`, puis `ai-agent-service` (8084).
Variables d'environnement (voir `.env.example`, ne jamais les écrire dans un fichier) :

| Variable | Rôle |
|---|---|
| `GROQ_API_KEY` | clé du LLM (API compatible OpenAI, modèle `openai/gpt-oss-120b` par défaut, modifiable avec `LLM_MODEL`) |
| `TELEGRAM_BOT_TOKEN` | token du bot donné par @BotFather ; **sans lui, le bot n'est pas démarré** |
| `TELEGRAM_BOT_USERNAME` | nom du bot (vérifié au démarrage) |

```bash
C=http://localhost:8084/api/chat
curl -s -X POST $C -H "Content-Type: application/json" \
  -d '{"conversationId":"test-1","message":"Liste les comptes"}'
curl -s -X POST $C -H "Content-Type: application/json" \
  -d '{"conversationId":"test-1","message":"Quel est le solde du premier ?"}'
```

**Attendu :** `200` avec `{"conversationId":"test-1","answer":"..."}` ; la deuxième réponse s'appuie sur la première (mémoire par `conversationId`).

Message vide : `{"conversationId":"test-1","message":""}` → `400` - `Données invalides`.

## 13. Bot Telegram (long polling)

Le bot utilise le **long polling** (`getUpdates`) : aucun webhook ni URL publique, il fonctionne en local.
Au démarrage, il supprime un éventuel webhook puis le log de **ai-agent-service** affiche :

```
Bot Telegram connecté : @<TELEGRAM_BOT_USERNAME> (id <id>), mode long polling
```

Chaque message texte passe par le même `AgentService` que `/api/chat`, avec le **chatId Telegram comme conversationId** :
chaque utilisateur a sa propre mémoire de conversation.

Ouvrir `https://t.me/<TELEGRAM_BOT_USERNAME>` dans Telegram, puis :

| # | Message envoyé | Attendu |
|---|---|---|
| 13.1 | `/start` | message d'accueil listant ce que sait faire l'assistant (comptes, solde, création, blocage, paiement, historique, virement) et des exemples |
| 13.2 | `Liste les comptes` | les 3 comptes créés au démarrage (référence, devise, solde, statut) ; log `Message Telegram reçu (chat <chatId>)` |
| 13.3 | `Quel est le solde du compte de CUST-001 ?` | solde exact du compte, identique à `curl $A/<id>` |
| 13.4 | `Et ses transactions ?` | historique du **même** compte : la mémoire garde le contexte du message précédent |
| 13.5 | `Fais un paiement de 20 EUR au restaurant en France sur ce compte` | transaction créée ; le solde baisse de 20 (vérifier avec `curl $A/$EUR`) |
| 13.6 | `Fais un paiement de 5000 EUR sur ce compte` | refus expliqué (montant supérieur au maximum de 1000) |
| 13.7 | une photo ou un autocollant | `Je ne comprends que les messages texte...` |
| 13.8 | depuis un **autre** compte Telegram : `Et ses transactions ?` | l'agent ne connaît pas le contexte de l'autre utilisateur (mémoire séparée par chatId) |
| 13.9 | `Liste toutes les transactions de tous les comptes, avec tous les détails` (après plusieurs paiements) ou une demande de texte très long | si la réponse dépasse 4000 caractères, elle arrive en **plusieurs messages** successifs |

### Cas d'erreur

| # | Situation | Attendu |
|---|---|---|
| 13.10 | arrêter `transaction-service`, puis `Montre les transactions du compte de CUST-001` | le bot répond quand même : l'outil est en erreur, l'agent explique que le service est indisponible, ou le bot répond `Désolé, je ne peux pas répondre pour le moment : l'assistant est momentanément indisponible (modèle IA ou service bancaire injoignable)...` |
| 13.11 | démarrer ai-agent-service avec une `GROQ_API_KEY` invalide, puis envoyer `Bonjour` | `Désolé, je ne peux pas répondre pour le moment : ...` au lieu d'un silence ; log `Échec de l'agent (conversation <chatId>)` |
| 13.12 | démarrer ai-agent-service **sans** `TELEGRAM_BOT_TOKEN` | le service démarre normalement, `/api/chat` fonctionne, aucun log `Bot Telegram` (bot désactivé) |
| 13.13 | couper Internet quelques secondes | log `Erreur de long polling Telegram, nouvel essai dans 5 s` (token masqué `***`), puis reprise automatique |

Le découpage des longues réponses est aussi couvert par le test unitaire `MessageSplitterTest` :

```bash
./mvnw -pl ai-agent-service test
```
