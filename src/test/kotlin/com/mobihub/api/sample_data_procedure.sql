create procedure generate_sample_data()
    language plpgsql
as
$$

BEGIN
    -- Clean database
    TRUNCATE TABLE characteristicsmapping, "Comment", favourite, identityteam, identityuser, image, imagemapping, linktokens, membership, rating, trafficmodel RESTART IDENTITY CASCADE;

    -- Insert hardcoded users

    INSERT INTO identityuser (username, email, password, "isEmailVerified", "isAdmin")
    VALUES
        -- Id 1
        ('captain_crunch', 'user1@example.com', '$2a$14$zaNGFStCfGzMQ.HvvBZMSu1gyorJMp/QD5y4vzlWLvE7suf4Sh3xu',
         TRUE, FALSE),
        -- Id 2
        ('banana_joe', 'user2@example.com', '$2a$14$zaNGFStCfGzMQ.HvvBZMSu1gyorJMp/QD5y4vzlWLvE7suf4Sh3xu',
         FALSE, FALSE),
        -- Id 3
        ('admin_mcadminface', 'user3@example.com', '$2a$14$zaNGFStCfGzMQ.HvvBZMSu1gyorJMp/QD5y4vzlWLvE7suf4Sh3xu',
         TRUE, TRUE),
        -- Id 4
        ('sir_quacksalot', 'user4@example.com', '$2a$14$zaNGFStCfGzMQ.HvvBZMSu1gyorJMp/QD5y4vzlWLvE7suf4Sh3xu', TRUE,
         FALSE);

    -- Insert hardcoded traffic models

    INSERT INTO trafficmodel (name, description, "ownerUserId", "isVisibilityPublic", "dataSourceUrl",
                              "frameworkId", region, coordinates, "zipFileToken", "isZipFileUploaded")
    VALUES
        -- Id 1
        ('Stau Wars', 'Ein Modell für die epischen Staukämpfe zur Rush Hour.', 1, TRUE,
         'https://data.example.com/stauwars',
         2, 'Death Star City', '51.1657,10.4515', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 2
        ('Fast & Furious: Stau Drift', 'Modelliert riskante Spurwechsel und aggressive Fahrer.', 2, FALSE,
         'https://data.example.com/staudrift',
         3, 'Tokyo', '35.6895,139.6917', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 3
        ('Blitzer Royale', 'Ein Modell zur optimalen Platzierung von Blitzern.', 3, TRUE,
         'https://data.example.com/blitzer',
         1, 'Berlin', '52.5200,13.4050', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 4
        ('Tramtasia', 'Eine perfekte Simulation für eine Welt voller Straßenbahnen.', 4, TRUE,
         'https://data.example.com/tramtasia',
         4, 'Zürich', '47.3769,8.5417', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 5
        ('Schienenchaos', 'Ein Modell für Zugverspätungen und Weichenstörungen.', 4, TRUE,
         'https://data.example.com/schienenchaos',
         4, 'Frankfurt', '50.1109,8.6821', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 6
        ('Fahrverbot Total', 'Eine Simulation einer Stadt, die Autos verbannt hat.', 1, TRUE,
         'https://data.example.com/fahrverbot',
         2, 'Amsterdam', '52.3676,4.9041', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 7
        ('Tesla Stau-Pilot', 'Ein Modell für autonomes Stop-and-Go.', 3, FALSE, 'https://data.example.com/teslastau',
         3, 'San Francisco', '37.7749,-122.4194', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 8
        ('Radfahrer vs. Autofahrer', 'Ein Simulationsmodell für das tägliche Duell im Stadtverkehr.', 2, TRUE,
         'https://data.example.com/radfahrer',
         1, 'Kopenhagen', '55.6761,12.5683', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 9
        ('Elektro-Straßenkampf', 'Die Auswirkungen von zu wenigen Ladesäulen im Stadtgebiet.', 4, TRUE,
         'https://data.example.com/elektrochaos',
         4, 'Oslo', '59.9139,10.7522', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 10
        ('Schrittgeschwindigkeit Deluxe', 'Wie der Verkehr aussieht, wenn alle nur noch mit 5 km/h fahren.', 2,
         FALSE, 'https://data.example.com/schrittgeschwindigkeit',
         2, 'München', '48.1351,11.5820', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE),

        -- Id 11
        ('U-Bahn-Dystopie', 'Ein Modell für überfüllte U-Bahnen und spontane Ausfälle.', 2, TRUE,
         'https://data.example.com/ubahnchaos',
         3, 'London', '51.5074,-0.1278', '1edc8d50-2c84-44cd-a9f1-7202f55ab6e7', TRUE);


    -- Insert characteristics mappings
    -- no characteristics mapping for traffic model 3
    -- multiple characteristics mappings for traffic model 1 & 5
    INSERT INTO characteristicsmapping ("trafficModelId", "modelLevelId", "modelMethodId")
    VALUES (1, 2, 1),
           (1, 5, 11),
           (2, 4, 10),
           (4, 3, 1),
           (5, 1, 2),
           (5, 0, 6),
           (6, 5, 12),
           (7, 4, 9),
           (8, 2, 4),
           (9, 3, 3),
           (10, 0, 0);

    -- Insert ratings
    INSERT INTO rating ("trafficModelId", "userId", "usersRating")
    VALUES (1, 1, 5), -- "Stau Wars" bekommt eine Top-Bewertung
           (2, 2, 4), -- "Fast & Furious: Stau Drift" ist fast perfekt
           (3, 3, 2), -- "Blitzer Royale" hat Verbesserungspotenzial
           (4, 4, 3), -- "Tramtasia" ist okay, aber nicht für alle
           (5, 1, 1), -- "Fahrverbot Total" sorgt für Frust
           (6, 2, 5), -- "Tesla Stau-Pilot" ist überraschend gut
           (7, 3, 3), -- "Radfahrer vs. Autofahrer" ist kontrovers
           (8, 4, 4), -- "Elektro-Straßenkampf" ist gut umgesetzt
           (9, 1, 2), -- "Schrittgeschwindigkeit Deluxe" ist etwas zu langsam
           (10, 2, 5); -- "U-Bahn-Dystopie" ist eine realistische Simulation

    INSERT INTO "Comment" ("userId", "trafficModelId", content, "creationDate")
    VALUES (1, 2, 'Warum gibt es hier so viele Staus? Ach ja, es heißt "Stau Wars"...', '2025-03-11 08:15:00'),
           (2, 4, 'Blitzer Royale ist ja gut, aber wo bleibt die Escape-Route?', '2025-03-11 09:30:00'),
           (3, 1, 'Ich habe versucht, das Modell zu optimieren. Jetzt ist es noch schlimmer. Oops.',
            '2025-03-11 10:45:00'),
           (4, 7, 'Radfahrer vs. Autofahrer? Endlich eine Simulation für meine Albträume.', '2025-03-11 11:20:00'),
           (1, 6, 'Tesla Stau-Pilot? Ich hoffe, es kann auch Kaffee holen.', '2025-03-11 12:10:00'),
           (2, 9, '5 km/h für alle? Das klingt nach meinem Arbeitsweg.', '2025-03-11 13:05:00'),
           (3, 10, 'U-Bahn-Dystopie ist realer als mir lieb ist.', '2025-03-11 14:00:00'),
           (4, 5, 'Ich habe 100 Ladesäulen hinzugefügt und trotzdem Streit.', '2025-03-11 15:30:00');

    -- Insert favorites
    INSERT INTO favourite ("userId", "trafficModelId")
    VALUES (1, 1),
           (1, 2);

END;
$$;

alter procedure generate_sample_data() owner to admin;

