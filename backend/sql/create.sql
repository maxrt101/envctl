
CREATE DATABASE controller;

\c controller

BEGIN;

CREATE TABLE IF NOT EXISTS controller (
    id serial       PRIMARY KEY,
    name text       NOT NULL
);

CREATE TABLE IF NOT EXISTS env_data (
    id serial       PRIMARY KEY,
    controllerId    int REFERENCES controller(id),
    timepoint       timestamp,
    temperature     float,
    humidity        float
);

INSERT INTO controller(name) VALUES ('default');
