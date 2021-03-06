-- Patch from v2823 -> v2824
-- Change: Added REST-API token columns to accounts
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_ACCOUNTS ADD COLUMN REST_TOKEN CHAR(64) NULL;
ALTER TABLE FXS_ACCOUNTS ADD COLUMN REST_EXPIRES BIGINT NULL;

CREATE INDEX IDX_ACCOUNTS_REST_TOKEN ON FXS_ACCOUNTS(REST_TOKEN);
CREATE UNIQUE INDEX UK_ACCOUNTS_REST_TOKEN ON FXS_ACCOUNTS(REST_TOKEN);