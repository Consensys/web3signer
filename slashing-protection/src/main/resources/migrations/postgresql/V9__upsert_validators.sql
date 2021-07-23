CREATE OR REPLACE FUNCTION upsert_validators(_vals BYTEA[])
  RETURNS TABLE (v_id integer, v_public_key BYTEA) AS
$$
BEGIN
  RETURN QUERY
    WITH
      all_rows AS (
        SELECT *
        FROM UNNEST(_vals) WITH ORDINALITY AS t(public_key, ordinality)
      ),
      dist_rows AS (
        SELECT DISTINCT public_key
        FROM all_rows
      ),
      new_rows AS (
        SELECT d.public_key
        FROM dist_rows d
             LEFT JOIN validators f USING (public_key)
        WHERE f.public_key IS NULL
      ),
      ins AS (
        INSERT INTO validators (public_key)
          SELECT public_key
          FROM new_rows
          ORDER BY public_key
          ON CONFLICT DO NOTHING
          RETURNING public_key, id
      ),
      sel AS (
        SELECT public_key, id
        FROM ins
        UNION ALL
        SELECT f.public_key, f.id
        FROM dist_rows
             JOIN validators f USING (public_key)
      ),
      ups AS (
        INSERT INTO validators AS f (public_key)
          SELECT d.public_key
          FROM dist_rows d
               LEFT JOIN sel s USING (public_key)
          WHERE s.public_key IS NULL
          ORDER BY public_key
          ON CONFLICT ON CONSTRAINT validators_public_key_key DO UPDATE
            SET public_key = f.public_key
          RETURNING public_key, id
      ),
      fin AS (
        SELECT public_key, id
        FROM sel
        UNION ALL
        TABLE ups
      )
    SELECT f.id, f.public_key
    FROM all_rows a
         JOIN fin f USING (public_key)
    ORDER BY a.ordinality;
END
$$ LANGUAGE plpgsql;

UPDATE database_version SET version = 9 WHERE id = 1;