CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    name VARCHAR(120) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('CUSTOMER', 'AGENT')),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE tickets (
    id UUID PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(30) NOT NULL CHECK (category IN ('TECHNICAL', 'BILLING', 'ACCOUNT', 'GENERAL')),
    priority VARCHAR(20) NOT NULL CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    status VARCHAR(30) NOT NULL CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'REOPENED')),
    customer_id UUID NOT NULL REFERENCES users(id),
    assigned_agent_id UUID REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets(id),
    author_id UUID NOT NULL REFERENCES users(id),
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE status_history (
    id UUID PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES tickets(id),
    event_type VARCHAR(30) NOT NULL CHECK (event_type IN ('STATUS_CHANGE', 'ASSIGNMENT')),
    from_status VARCHAR(30),
    to_status VARCHAR(30),
    from_agent_id UUID,
    to_agent_id UUID,
    changed_by UUID NOT NULL REFERENCES users(id),
    changed_at TIMESTAMPTZ NOT NULL,
    note VARCHAR(1000)
);

CREATE INDEX idx_ticket_customer ON tickets(customer_id);
CREATE INDEX idx_ticket_status ON tickets(status);
CREATE INDEX idx_ticket_agent_status ON tickets(assigned_agent_id, status);
CREATE INDEX idx_comment_ticket_created ON comments(ticket_id, created_at);
CREATE INDEX idx_history_ticket_changed ON status_history(ticket_id, changed_at);
