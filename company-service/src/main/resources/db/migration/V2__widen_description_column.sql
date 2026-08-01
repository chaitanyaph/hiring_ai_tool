-- V1 created companies.description as TEXT, but the entity's @Lob mapping
-- (correctly) expects LONGTEXT -- Hibernate's schema-validation was failing
-- every startup with "found text, but expecting longtext". Widening here
-- rather than editing V1, since V1 is already applied on any environment
-- that has booted this service before.
ALTER TABLE companies MODIFY COLUMN description LONGTEXT;
