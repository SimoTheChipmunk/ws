CREATE TABLE location (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name       VARCHAR(120)     NOT NULL,
    latitude   DOUBLE PRECISION NOT NULL CHECK (latitude  BETWEEN  -90 AND  90),
    longitude  DOUBLE PRECISION NOT NULL CHECK (longitude BETWEEN -180 AND 180),
    timezone   VARCHAR(64)      NOT NULL DEFAULT 'UTC',
    created_at TIMESTAMPTZ      NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_location_name ON location (lower(name));

CREATE TABLE measurement (
    location_id   BIGINT           NOT NULL REFERENCES location (id) ON DELETE CASCADE,
    observed_at   TIMESTAMPTZ      NOT NULL,
    temperature_c DOUBLE PRECISION,
    humidity_pct  DOUBLE PRECISION CHECK (humidity_pct BETWEEN 0 AND 100),
    wind_kmh      DOUBLE PRECISION CHECK (wind_kmh >= 0),
    source        VARCHAR(32)      NOT NULL,
    ingested_at   TIMESTAMPTZ      NOT NULL DEFAULT now(),
    PRIMARY KEY (location_id, observed_at)
);
