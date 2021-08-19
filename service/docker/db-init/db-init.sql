ALTER USER postgres PASSWORD 'password1';
CREATE SCHEMA IF NOT EXISTS postgres AUTHORIZATION postgres;
GRANT ALL ON SCHEMA postgres TO postgres;