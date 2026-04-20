CREATE TABLE Role (
                     id TINYINT AUTO_INCREMENT PRIMARY KEY,
                     name VARCHAR(200)
);


CREATE TABLE Users (
                         id VARCHAR(36) PRIMARY KEY,
                         name VARCHAR(50),
                         last_name VARCHAR(100),
                         email VARCHAR(100) UNIQUE,
                         rol_id tinyint,
                         status BOOLEAN DEFAULT true,
                         provider_ide VARCHAR(255),

                         FOREIGN KEY (rol_id) REFERENCES Role(id)
);


INSERT INTO Role VALUES (1, 'ADMIN');
INSERT INTO Role VALUES (2, 'DIRECTOR_GENERAL');
INSERT INTO Role VALUES (3, 'DIRECTOR_FINANZAS');
INSERT INTO Role VALUES (4, 'DIRECTOR_MERCADOTECNIA');

INSERT INTO Users (id, name, last_name, email, role_id, status, provider_id) VALUES ('08631269-3f4c-4299-a1e7-23f5684e1091', 'Santiago', 'Niño', 'santiago.nino@example.com', 1, true, 'i8AULkutUNTy9xIUyp2lpHczMHi2');