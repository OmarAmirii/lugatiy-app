# Lugatiy

Een basis voor een Arabisch leerplatform. Dit project is **nog niet gereed voor productie**: alleen de eerste woordenboek- en authenticatiebasis is gebouwd; zie [architectuur en fasering](docs/architectuur.md) voor de actuele omvang. Een eerste woordenbeheer, eenmalige beheerderinitialisatie en een eenvoudige meerkeuzequiz met opgeslagen resultaten en aanmaak van niveaus zijn toegevoegd. Een eerste afgeschermde PDF- en videobibliotheek en accountbeheer zijn toegevoegd; andere onderdelen blijven onvolledig.

## Fase 1 lokaal starten

Benodigd: Java 21, Node.js 20 of nieuwer, npm en Docker Desktop met Compose. Een aparte Maven-installatie is niet nodig: de meegeleverde Maven-wrapper downloadt Maven bij de eerste start. Voor de eerste installatie is internet nodig.

1. Start Docker Desktop en wacht tot Docker gereed is.
2. Open een terminal in de projectmap en voer uit: `cp .env.example .env`.
3. Open `.env` in een teksteditor. Kies een eigen `POSTGRES_PASSWORD`, vul `BOOTSTRAP_ADMIN_EMAIL` in en kies een `BOOTSTRAP_ADMIN_PASSWORD` van minstens 12 tekens. Dit wordt het eerste beheerdersaccount. Gebruik aanhalingstekens voor waarden met spaties of shelltekens, bijvoorbeeld `BOOTSTRAP_ADMIN_PASSWORD='een-veilig-wachtwoord'`.
4. Start alles met `./scripts/start.sh`. De eerste start kan enkele minuten duren terwijl afhankelijkheden worden opgehaald. Wacht totdat Spring Boot meldt dat de applicatie is gestart.
5. Open [http://localhost:5173](http://localhost:5173), log in en open **Beheer**. Voeg een woord met klinkertekens toe en zoek het daarna in het woordenboek. Onder **Oefenen** kun je de voorbeeldquiz maken; het resultaat verschijnt bij **Voortgang**.
6. Stop frontend en backend met `Ctrl+C`. De database blijft draaien om gegevens te bewaren; stoppen kan met `docker compose stop`. Start de app later opnieuw met `./scripts/start.sh`.

Verwijder na de eerste succesvolle start de twee `BOOTSTRAP_ADMIN_`-waarden uit `.env`. Het bestaande account blijft in de database staan. Wijzig het wachtwoord zo nodig via **Account**. Verander het databasewachtwoord niet zomaar in `.env` nadat het databasevolume is aangemaakt: het wachtwoord in PostgreSQL verandert niet automatisch mee.

Voor de browsercontrole kan in `frontend` `npm run test:e2e` worden uitgevoerd nadat Playwright Chromium is geïnstalleerd met `npx playwright install chromium`. Geef `E2E_ADMIN_EMAIL` en `E2E_ADMIN_PASSWORD` mee via omgevingsvariabelen. Gebruik hiervoor uitsluitend een aparte testdatabase: de tests maken gegevens aan. De API-documentatie staat op http://localhost:8080/swagger-ui/index.html.

De PostgreSQL-volume bevat blijvende data. Verwijder dit volume niet zonder een gecontroleerde back-up. Voor ingebruikname moeten versleutelde, externe en regelmatig geteste back-ups van zowel de database als toekomstige uploads worden ingericht. Herstel vereist de repository, omgevingsconfiguratie, een databasebackup en eventuele mediabestanden; oefen het terugzetten voordat gegevens worden toevertrouwd aan het systeem. Er is nog geen automatische back-up ingericht.

Er is geen privéremote ingesteld. Maak een afzonderlijke Git-repository voor deze map en koppel zelf een privéremote; controleer vóór de eerste commit dat `.env`, uploads en andere geheimen uitgesloten zijn.

## Eerste beheerder en woordenbeheer

Vul vóór de eerste backendstart `BOOTSTRAP_ADMIN_EMAIL` en een uniek `BOOTSTRAP_ADMIN_PASSWORD` van minimaal 12 tekens in `.env` in. Bij het opstarten wordt uitsluitend een beheerder aangemaakt als er nog geen beheerder en geen account met dit e-mailadres bestaat. Verwijder beide waarden daarna uit `.env` en de omgeving. Log in via `/inloggen` en open `/beheer` om woorden aan te maken, te wijzigen en te verwijderen. Deze eerste beheerpagina toont maximaal 24 woorden; paginering en volledig contentbeheer ontbreken nog. Controleer Arabische spelling en klinkertekens redactioneel voordat woorden worden gepubliceerd.

## Quizzen

Onder `/quizzen` staat een voorbeeldquiz met meerkeuzevragen. Ingelogde gebruikers kunnen antwoorden insturen, uitleg zien en hun pogingen bekijken onder `/voortgang`. Een beheerder kan onder `/beheer` een niveau toevoegen. Quizbeheer, andere vraagtypen en directe feedback per vraag ontbreken nog. De voorbeeldinhoud staat in de Flyway-migratie.

## Woordkaarten, favorieten en werkwoorden

Ingelogde gebruikers kunnen woorden bewaren onder `/favorieten`. Onder `/woordkaarten` zijn kaarten te selecteren en via het afdrukvenster van de browser als PDF te bewaren. Papierformaten A3–A6 worden aangevraagd via de printinstellingen; controleer het formaat in het afdrukvenster. Illustraties, opgeslagen sets, directe PDF-download en de overige ontwerpopties ontbreken. Het scherm toont momenteel alleen de eerste 24 woorden.

Onder `/werkwoorden` staan voorbeeldvormen voor كَتَبَ, inclusief 14 vormen in de verleden en tegenwoordige tijd. Deze voorbeeldgegevens wachten op onafhankelijke redactionele controle; er is nog geen regelengine of algemeen werkwoordbeheer.

## PDF-bibliotheek

Een beheerder kan via `/beheer` PDF-bestanden van maximaal 25 MB als concept uploaden en publiceren. PDFBox controleert of het bestand kan worden geopend. Ingelogde gebruikers kunnen gepubliceerde boeken onder `/bibliotheek` lezen; de laatst bezochte pagina wordt per account opgeslagen. Concepten en het beheer zijn afgeschermd. Het bestand wordt buiten PostgreSQL onder `STORAGE_PATH/books` opgeslagen; neem deze map samen met de database op in de back-up. `STORAGE_PATH` is relatief aan de werkmap van de backend als geen absoluut pad wordt opgegeven.

Een browser moet de PDF ontvangen om deze te kunnen weergeven: autorisatie voorkomt geen screenshots of extractie door een geautoriseerde lezer. Filters op niveau, omslagen, favorieten, collecties en volledig boekenbeheer ontbreken nog. De viewer is nog niet via een browser getest.

## Videobibliotheek en accounts

Beheerders kunnen onder `/beheer` MP4-bestanden van maximaal 500 MB uploaden, aan series koppelen en afzonderlijk publiceren. Zowel de aflevering als de serie moet gepubliceerd zijn voor bezoekers. Onder `/videos` kunnen ingelogde gebruikers video’s bekijken; kijkposities worden per account opgeslagen en bytebereiken worden ondersteund. Bestanden staan in `STORAGE_PATH/videos`, buiten PostgreSQL. Neem deze map op in de back-up. De uploadcontrole controleert de hoofdstructuur van de MP4-container, maar controleert niet of codecs en alle mediadata geldig of veilig zijn. Ondertiteling, transcodering, HLS, miniaturen en volledig seriebeheer ontbreken. Het afspelen is nog niet met een echte video in een browser getest.

Beheerders kunnen accounts voor de vier rollen aanmaken en uitschakelen. Gebruikers kunnen onder `/account` hun wachtwoord wijzigen en uitloggen. Uitschakelen en wachtwoordwijzigingen trekken bestaande sessies in. Relaties tussen leerlingen en ouders/docenten en hun toegangsregels zijn nog niet geïmplementeerd.
