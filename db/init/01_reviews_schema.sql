-- 1. Reference / lookup tables
CREATE TABLE provider (
    provider_id      SMALLINT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_id      SMALLINT   NOT NULL UNIQUE,
    provider_name    TEXT       NOT NULL
);

CREATE TABLE rating_category (
    category_id   SMALLINT   GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    category_name TEXT       NOT NULL UNIQUE      -- “Cleanliness”, “Facilities”, …
);

-- 2. Core entity tables
CREATE TABLE hotel (
    hotel_id     INTEGER     GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    external_id  INTEGER     NOT NULL,
    provider_id  SMALLINT    NOT NULL REFERENCES provider (provider_id),
    hotel_name   TEXT        NOT NULL
);

ALTER TABLE hotel ADD CONSTRAINT hotel_unique_key UNIQUE (external_id, provider_id);

CREATE TABLE reviewer (
    reviewer_id  BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    display_name TEXT,                           
    provider_id  SMALLINT    NOT NULL REFERENCES provider(provider_id),
    country_id   INTEGER,
    country_name VARCHAR(255),
    flag_code    CHAR(2),                        
    is_expert    BOOLEAN     DEFAULT FALSE,
    reviews_written INTEGER  DEFAULT 0,
    CONSTRAINT reviewer_display_country_provider_unique UNIQUE (display_name, country_id, provider_id)
);

-- 3. Review-specific tables
CREATE TABLE review (
    review_id          BIGINT      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    review_external_id BIGINT      NOT NULL,
    hotel_id           INTEGER     REFERENCES hotel (hotel_id),
    provider_id        SMALLINT    REFERENCES provider (provider_id),
    reviewer_id        BIGINT      REFERENCES reviewer (reviewer_id),
    rating             NUMERIC(3,1),             
    rating_text        TEXT,                     
    rating_formatted   VARCHAR(10),                     
    review_title       TEXT,
    review_comment     TEXT,
    review_vote_positive    INTEGER,
    review_vote_negative    INTEGER,
    review_date        TIMESTAMP,
    translate_source   VARCHAR(10),
    translate_target   VARCHAR(10),
    is_response_shown  BOOLEAN      DEFAULT FALSE,
    responder_name     TEXT,
    response_text      TEXT,
    response_date_text TEXT,
    response_date_fmt  TEXT,
    check_in_month_yr  VARCHAR(31)
);

-- 4. Stay-specific details (split out from reviewerInfo)
CREATE TABLE stay_info (
    review_id          BIGINT      PRIMARY KEY REFERENCES review (review_id),
    room_type_id       INTEGER,
    room_type_name     TEXT,
    review_group_id    INTEGER,
    review_group_name  VARCHAR(31),      -- “Solo traveler”
    length_of_stay     SMALLINT
);

-- 5. Aggregated per-hotel metrics supplied by provider
CREATE TABLE provider_hotel_summary (
    hotel_id       INTEGER    REFERENCES hotel (hotel_id),
    provider_id    SMALLINT   REFERENCES provider (provider_id),
    overall_score  NUMERIC(3,1),
    review_count   INTEGER,
    PRIMARY KEY (hotel_id, provider_id)
);

CREATE TABLE provider_hotel_grade (
    hotel_id       INTEGER    REFERENCES hotel (hotel_id),
    provider_id    SMALLINT   REFERENCES provider (provider_id),
    category_id    SMALLINT   REFERENCES rating_category (category_id),
    grade_value    NUMERIC(3,1),
    PRIMARY KEY (hotel_id, provider_id, category_id)
);
