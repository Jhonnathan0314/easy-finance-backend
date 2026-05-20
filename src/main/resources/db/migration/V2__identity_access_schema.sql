CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'))
);

CREATE TABLE global_roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    CONSTRAINT uq_global_roles_name UNIQUE (name),
    CONSTRAINT chk_global_roles_name CHECK (name IN ('SUPER_ADMIN', 'USER'))
);

CREATE TABLE user_global_roles (
    user_id BIGINT NOT NULL,
    global_role_id BIGINT NOT NULL,
    CONSTRAINT pk_user_global_roles PRIMARY KEY (user_id, global_role_id),
    CONSTRAINT fk_user_global_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_global_roles_global_role FOREIGN KEY (global_role_id) REFERENCES global_roles (id)
);

CREATE TABLE participants (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    display_name VARCHAR(150) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    CONSTRAINT uq_participants_user_id UNIQUE (user_id),
    CONSTRAINT fk_participants_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_participants_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_users_status ON users (status);
CREATE INDEX idx_participants_status ON participants (status);
CREATE INDEX idx_user_global_roles_role ON user_global_roles (global_role_id);

INSERT INTO global_roles (name, description)
VALUES
    ('SUPER_ADMIN', 'Technical platform administrator.'),
    ('USER', 'Default authenticated user.')
ON CONFLICT (name) DO NOTHING;
