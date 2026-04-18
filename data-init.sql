-- =============================================================================
-- Script de inicialização — Sistema de Vendas
-- Execute UMA VEZ após a primeira inicialização da aplicação.
-- =============================================================================

-- Cria o usuário admin inicial.
-- A senha 'admin' já codificada com BCrypt (strength=10).
-- IMPORTANTE: troque este hash por uma senha segura em produção.
INSERT INTO usuario (cpf, senha, enabled)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    true
);

-- Vincula a role ROLE_ADMIN ao usuário recém-criado
INSERT INTO roles (role, usuario_id)
VALUES (
    'ROLE_ADMIN',
    (SELECT id FROM usuario WHERE cpf = 'admin')
);

-- =============================================================================
-- Exemplos de outros usuários (opcional)
-- Senha 'vendedor123' codificada com BCrypt:
-- $2a$10$7EqJtq98hPqEX7fNZaFWoO1KYTjaGX5rMNkFVxOr7V9K3uC1.pN9e
-- =============================================================================
-- INSERT INTO usuario (cpf, senha, enabled)
-- VALUES ('vendedor01', '$2a$10$7EqJtq98hPqEX7fNZaFWoO1KYTjaGX5rMNkFVxOr7V9K3uC1.pN9e', true);
--
-- INSERT INTO roles (role, usuario_id)
-- VALUES ('ROLE_VENDEDOR', (SELECT id FROM usuario WHERE cpf = 'vendedor01'));
