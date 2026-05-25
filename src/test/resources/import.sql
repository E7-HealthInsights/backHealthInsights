

-- Usuario fake del TestFirebaseAuthFilter (requerido para tests de integración)
INSERT INTO Users (id, name, last_name, email, role_id, status, provider_id)
VALUES ('00000000-0000-0000-0000-000000000001', 'Test', 'Admin', 'test@test.com', 1, true, 'test-firebase-uid');