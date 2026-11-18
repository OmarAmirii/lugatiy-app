# Architectuur en implementatieplan

## Architectuur

React/TypeScript → Vite-proxy → Spring Security en REST → services/repositories → PostgreSQL. Flyway beheert het schema. Authenticatie gebruikt een server-side sessie met HttpOnly-cookie; mutaties vereisen een CSRF-token. Oorsprongen voor CORS staan expliciet in de configuratie. Productiebestanden horen buiten de database en buiten de publieke webroot, met autorisatie vóór levering. PDF-bestanden worden buiten de database opgeslagen en achter sessieautorisatie geleverd. Video’s staan buiten de database en worden achter sessieautorisatie geleverd. Productieopslag en productiebeheer zijn nog niet geïmplementeerd.

## ERD: beoogd model

```mermaid
erDiagram
 USERS ||--o{ USER_ROLES : heeft
 USERS ||--o{ QUIZ_ATTEMPTS : maakt
 USERS ||--o{ READING_PROGRESS : leest
 USERS ||--o{ WATCH_PROGRESS : bekijkt
 WORD_CATEGORIES ||--o{ WORDS : bevat
 WORDS ||--o{ WORD_TRANSLATIONS : vertaalt
 WORDS ||--o{ WORD_EXAMPLES : illustreert
 WORDS ||--o{ FLASHCARD_ITEMS : gebruikt
 FLASHCARD_SETS ||--o{ FLASHCARD_ITEMS : bevat
 VERB_ROOTS ||--o{ VERBS : vormt
 VERB_PATTERNS ||--o{ VERBS : beschrijft
 VERBS ||--o{ VERB_CONJUGATIONS : heeft
 VERBS ||--o{ VERB_EXCEPTIONS : heeft
 QUIZ_LEVELS ||--o{ QUIZZES : bevat
 QUIZZES ||--o{ QUIZ_QUESTIONS : bevat
 QUIZ_QUESTIONS ||--o{ QUIZ_ANSWERS : heeft
 QUIZZES ||--o{ QUIZ_ATTEMPTS : registreert
 BOOKS ||--o{ READING_PROGRESS : heeft
 VIDEO_SERIES ||--o{ VIDEOS : bevat
 VIDEOS ||--o{ WATCH_PROGRESS : heeft
 MEDIA_ASSETS ||--o{ BOOKS : bestand
 MEDIA_ASSETS ||--o{ VIDEOS : bestand
```

Alleen het in de migraties aanwezige schema is daadwerkelijk geïmplementeerd. Relaties tussen ouders/docenten en leerlingen, publicatie en overige content vereisen verdere migraties.

## API

| Endpoint | Doel | Status |
| --- | --- | --- |
| GET /api/v1/auth/csrf | CSRF-cookie en token ophalen | basis |
| POST /api/v1/auth/login | sessie starten | basis |
| POST /api/v1/auth/logout | sessie beëindigen | basis |
| GET /api/v1/auth/me | huidige gebruiker | basis |
| GET /api/v1/words?q= | woorden zoeken | basis |
| POST /api/v1/words | woord toevoegen, ADMIN | basis |
| /api/v1/quizzes, /levels, /progress | meerkeuzequiz, niveaus en pogingen | eerste versie |
| /api/v1/verbs, /flashcards | taal en woordkaarten | gepland |
| /api/v1/books, /admin/books | PDF-metadata, afgeschermde inhoud en beheer | eerste versie |
| /api/v1/videos, /series, /admin/videos, /admin/series | video, series en kijkvoortgang | eerste versie |
| /api/v1/admin | content en gebruikers | gepland |

## React-structuur

`App` bevat de applicatieschil en routes; `ArabicText` centraliseert taalrichting en typografie; `Dictionary` verzorgt zoeken en woordweergave. Verdere modules krijgen eigen featuremappen, services en formulieren zodra hun backend bestaat.

## Fasering

1. Fundament: migraties, beveiligde sessies, CORS, React-schil en verticale woordenboekintegratie. **Basis aangelegd; integratietest ontbreekt; eerste beheerder wordt optioneel via omgevingsvariabelen aangemaakt.**
2. Arabisch: gecontroleerde lexicale inhoud, categorieën en een expliciete morfologische regelengine met uitzonderingen.
3. Leren: eerste meerkeuzequiz en opgeslagen resultaten aanwezig; kaartontwerp, PDF-export, volledig niveaubeheer en overige vraagtypen ontbreken.
4. Media: veilige uploads, afgeschermde PDF-viewer, video's, series en voortgang.
5. Beheer: gevalideerde contentworkflows, rollen, relaties en rapportages.
6. Afwerking: toegankelijkheid, beveiliging, herstelbare back-ups, end-to-endtests en productie-inrichting.

Elke fase vereist migraties en integratietests. Er worden geen vervoegingen gegenereerd zonder gecontroleerde grammaticale gegevens. Het aangeleverde logo staat in de navigatie.
