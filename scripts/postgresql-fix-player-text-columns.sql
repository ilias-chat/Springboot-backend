-- Run once on Cloud SQL / PostgreSQL when players.name|team|league|position are bytea
-- (causes: ERROR: function lower(bytea) does not exist from Spring list/search queries).
--
-- If a column is already varchar/text, the USING clause still works.
-- Connect: psql "$SPRING_DATASOURCE_URL" or Cloud SQL console.

ALTER TABLE players
    ALTER COLUMN name TYPE varchar(200)
        USING CASE
            WHEN pg_typeof(name) = 'bytea'::regtype THEN convert_from(name, 'UTF8')
            ELSE name::text
        END;

ALTER TABLE players
    ALTER COLUMN team TYPE varchar(200)
        USING CASE
            WHEN pg_typeof(team) = 'bytea'::regtype THEN convert_from(team, 'UTF8')
            ELSE team::text
        END;

ALTER TABLE players
    ALTER COLUMN league TYPE varchar(200)
        USING CASE
            WHEN pg_typeof(league) = 'bytea'::regtype THEN convert_from(league, 'UTF8')
            ELSE league::text
        END;

ALTER TABLE players
    ALTER COLUMN position TYPE varchar(64)
        USING CASE
            WHEN position IS NULL THEN NULL
            WHEN pg_typeof(position) = 'bytea'::regtype THEN convert_from(position, 'UTF8')
            ELSE position::text
        END;
