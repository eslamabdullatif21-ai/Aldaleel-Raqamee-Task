MERGE INTO users (id, email, password_hash, name, role, created_at) KEY(id) VALUES
('1542aabd-3724-4ae0-9091-447a2c62f82b', 'customer@example.com', '$2a$10$kA8AuudAj5st/8./MBfiaegRRyQzNWTx2eER3QVLai890z3YdacVi', 'Demo Customer', 'CUSTOMER', CURRENT_TIMESTAMP),
('e6cd6def-7b2b-4bc4-b965-8b1376479e60', 'agent@example.com', '$2a$10$5l8m3rmKsDPlhiGFbyP2oO3olcaLwEUMY9RXokDHxU2Iu72r.v2wu', 'Demo Agent', 'AGENT', CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 'agent2@example.com', '$2a$10$5l8m3rmKsDPlhiGFbyP2oO3olcaLwEUMY9RXokDHxU2Iu72r.v2wu', 'Demo Agent 2', 'AGENT', CURRENT_TIMESTAMP);
