-- Create the new user
CREATE USER backend_service WITH PASSWORD '[password]';

-- Grant all privileges on the mobiHub database to the new user
GRANT ALL PRIVILEGES ON DATABASE "mobiHub" TO backend_service;

-- Grant all privileges on all tables in the public schema to the new user
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO backend_service;

-- Grant all privileges on all sequences in the public schema to the new user
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO backend_service;

-- Grant all privileges on all functions in the public schema to the new user
GRANT ALL PRIVILEGES ON ALL FUNCTIONS IN SCHEMA public TO backend_service;

-- Grant the CREATE privilege on the public schema to the new user
GRANT CREATE ON SCHEMA public TO backend_service;

-- Grant the USAGE privilege on the public schema to the new user
GRANT USAGE ON SCHEMA public TO backend_service;
