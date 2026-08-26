--
-- PostgreSQL database dump
--

-- Dumped from database version 16.9
-- Dumped by pg_dump version 16.9

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.tickets DROP CONSTRAINT IF EXISTS tickets_customer_id_fkey;
ALTER TABLE IF EXISTS ONLY public.tickets DROP CONSTRAINT IF EXISTS tickets_assigned_agent_id_fkey;
ALTER TABLE IF EXISTS ONLY public.status_history DROP CONSTRAINT IF EXISTS status_history_ticket_id_fkey;
ALTER TABLE IF EXISTS ONLY public.status_history DROP CONSTRAINT IF EXISTS status_history_changed_by_fkey;
ALTER TABLE IF EXISTS ONLY public.comments DROP CONSTRAINT IF EXISTS comments_ticket_id_fkey;
ALTER TABLE IF EXISTS ONLY public.comments DROP CONSTRAINT IF EXISTS comments_author_id_fkey;
DROP INDEX IF EXISTS public.idx_ticket_status;
DROP INDEX IF EXISTS public.idx_ticket_customer;
DROP INDEX IF EXISTS public.idx_ticket_agent_status;
DROP INDEX IF EXISTS public.idx_history_ticket_changed;
DROP INDEX IF EXISTS public.idx_comment_ticket_created;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS users_pkey;
ALTER TABLE IF EXISTS ONLY public.users DROP CONSTRAINT IF EXISTS uq_users_email;
ALTER TABLE IF EXISTS ONLY public.tickets DROP CONSTRAINT IF EXISTS tickets_pkey;
ALTER TABLE IF EXISTS ONLY public.status_history DROP CONSTRAINT IF EXISTS status_history_pkey;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
ALTER TABLE IF EXISTS ONLY public.comments DROP CONSTRAINT IF EXISTS comments_pkey;
DROP TABLE IF EXISTS public.users;
DROP TABLE IF EXISTS public.tickets;
DROP TABLE IF EXISTS public.status_history;
DROP TABLE IF EXISTS public.flyway_schema_history;
DROP TABLE IF EXISTS public.comments;
SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: comments; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.comments (
    id uuid NOT NULL,
    ticket_id uuid NOT NULL,
    author_id uuid NOT NULL,
    body text NOT NULL,
    created_at timestamp with time zone NOT NULL
);


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: status_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.status_history (
    id uuid NOT NULL,
    ticket_id uuid NOT NULL,
    event_type character varying(30) NOT NULL,
    from_status character varying(30),
    to_status character varying(30),
    from_agent_id uuid,
    to_agent_id uuid,
    changed_by uuid NOT NULL,
    changed_at timestamp with time zone NOT NULL,
    note character varying(1000),
    CONSTRAINT status_history_event_type_check CHECK (((event_type)::text = ANY ((ARRAY['STATUS_CHANGE'::character varying, 'ASSIGNMENT'::character varying])::text[])))
);


--
-- Name: tickets; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tickets (
    id uuid NOT NULL,
    title character varying(200) NOT NULL,
    description text NOT NULL,
    category character varying(30) NOT NULL,
    priority character varying(20) NOT NULL,
    status character varying(30) NOT NULL,
    customer_id uuid NOT NULL,
    assigned_agent_id uuid,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    CONSTRAINT tickets_category_check CHECK (((category)::text = ANY ((ARRAY['TECHNICAL'::character varying, 'BILLING'::character varying, 'ACCOUNT'::character varying, 'GENERAL'::character varying])::text[]))),
    CONSTRAINT tickets_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'URGENT'::character varying])::text[]))),
    CONSTRAINT tickets_status_check CHECK (((status)::text = ANY ((ARRAY['OPEN'::character varying, 'IN_PROGRESS'::character varying, 'RESOLVED'::character varying, 'CLOSED'::character varying, 'REOPENED'::character varying])::text[])))
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.users (
    id uuid NOT NULL,
    email character varying(320) NOT NULL,
    password_hash character varying(100) NOT NULL,
    name character varying(120) NOT NULL,
    role character varying(20) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    CONSTRAINT users_role_check CHECK (((role)::text = ANY ((ARRAY['CUSTOMER'::character varying, 'AGENT'::character varying])::text[])))
);


--
-- Data for Name: comments; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.comments (id, ticket_id, author_id, body, created_at) FROM stdin;
73473c6b-8e29-4612-9ba8-8a81697c3aa0	20eee9e2-7bb2-430c-8080-2d3574ad4c5f	1542aabd-3724-4ae0-9091-447a2c62f82b	I can reproduce this in two browsers.	2026-08-25 22:43:34.958966+03
efdc55bf-765c-4d78-8cd0-eca6595c48be	20eee9e2-7bb2-430c-8080-2d3574ad4c5f	e6cd6def-7b2b-4bc4-b965-8b1376479e60	Investigating the authentication logs now.	2026-08-25 22:43:34.975965+03
20d3c4b3-6efa-47d2-994e-84263d2b7baf	c698c692-c5b7-498b-8961-74977d953503	1542aabd-3724-4ae0-9091-447a2c62f82b	The duplicate line is item 4 on the invoice.	2026-08-25 22:43:34.987966+03
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init schema	SQL	V1__init_schema.sql	-809351647	app	2026-08-25 22:42:58.011781	46	t
\.


--
-- Data for Name: status_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.status_history (id, ticket_id, event_type, from_status, to_status, from_agent_id, to_agent_id, changed_by, changed_at, note) FROM stdin;
31d7f97d-f220-463a-8a09-9fb545309855	20eee9e2-7bb2-430c-8080-2d3574ad4c5f	STATUS_CHANGE	\N	OPEN	\N	\N	1542aabd-3724-4ae0-9091-447a2c62f82b	2026-08-25 22:43:34.835965+03	Ticket created
95e2d34f-b1bf-46aa-9f81-22fff2d2e5bf	c698c692-c5b7-498b-8961-74977d953503	STATUS_CHANGE	\N	OPEN	\N	\N	1542aabd-3724-4ae0-9091-447a2c62f82b	2026-08-25 22:43:34.868977+03	Ticket created
63b85eab-3512-497e-bbbd-8fcaa05cd6d4	20eee9e2-7bb2-430c-8080-2d3574ad4c5f	ASSIGNMENT	\N	\N	\N	e6cd6def-7b2b-4bc4-b965-8b1376479e60	e6cd6def-7b2b-4bc4-b965-8b1376479e60	2026-08-25 22:43:34.916965+03	Ticket assigned
938a4f31-3416-47e4-8b31-ee8666b5d77f	c698c692-c5b7-498b-8961-74977d953503	ASSIGNMENT	\N	\N	\N	e6cd6def-7b2b-4bc4-b965-8b1376479e60	e6cd6def-7b2b-4bc4-b965-8b1376479e60	2026-08-25 22:43:34.942966+03	Ticket assigned
9075b76e-24f3-44c0-b71d-885128734288	20eee9e2-7bb2-430c-8080-2d3574ad4c5f	STATUS_CHANGE	OPEN	IN_PROGRESS	\N	\N	e6cd6def-7b2b-4bc4-b965-8b1376479e60	2026-08-25 22:43:35.004967+03	Agent started investigation
2e6be22e-1540-4a7c-85f1-0f0bcc26ed8e	c698c692-c5b7-498b-8961-74977d953503	STATUS_CHANGE	OPEN	IN_PROGRESS	\N	\N	e6cd6def-7b2b-4bc4-b965-8b1376479e60	2026-08-25 22:43:35.018965+03	Invoice review started
017ed1de-18dd-4d17-8b19-09123a7d5204	c698c692-c5b7-498b-8961-74977d953503	STATUS_CHANGE	IN_PROGRESS	RESOLVED	\N	\N	e6cd6def-7b2b-4bc4-b965-8b1376479e60	2026-08-25 22:43:35.030965+03	Duplicate charge confirmed and credit issued
\.


--
-- Data for Name: tickets; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.tickets (id, title, description, category, priority, status, customer_id, assigned_agent_id, created_at, updated_at, version) FROM stdin;
20eee9e2-7bb2-430c-8080-2d3574ad4c5f	Cannot sign in after password reset	The reset link succeeded, but the new password is rejected on the login screen.	ACCOUNT	HIGH	IN_PROGRESS	1542aabd-3724-4ae0-9091-447a2c62f82b	e6cd6def-7b2b-4bc4-b965-8b1376479e60	2026-08-25 22:43:34.834965+03	2026-08-25 22:43:35.004967+03	2
c698c692-c5b7-498b-8961-74977d953503	Invoice total is incorrect	The August invoice includes a duplicate service charge.	BILLING	URGENT	RESOLVED	1542aabd-3724-4ae0-9091-447a2c62f82b	e6cd6def-7b2b-4bc4-b965-8b1376479e60	2026-08-25 22:43:34.868977+03	2026-08-25 22:43:35.030965+03	3
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.users (id, email, password_hash, name, role, created_at) FROM stdin;
1542aabd-3724-4ae0-9091-447a2c62f82b	customer@example.com	$2a$10$kA8AuudAj5st/8./MBfiaegRRyQzNWTx2eER3QVLai890z3YdacVi	Demo Customer	CUSTOMER	2026-08-25 22:43:34.319057+03
e6cd6def-7b2b-4bc4-b965-8b1376479e60	agent@example.com	$2a$10$5l8m3rmKsDPlhiGFbyP2oO3olcaLwEUMY9RXokDHxU2Iu72r.v2wu	Demo Agent	AGENT	2026-08-25 22:43:34.428598+03
\.


--
-- Name: comments comments_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: status_history status_history_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_pkey PRIMARY KEY (id);


--
-- Name: tickets tickets_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tickets
    ADD CONSTRAINT tickets_pkey PRIMARY KEY (id);


--
-- Name: users uq_users_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uq_users_email UNIQUE (email);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_comment_ticket_created; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_comment_ticket_created ON public.comments USING btree (ticket_id, created_at);


--
-- Name: idx_history_ticket_changed; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_history_ticket_changed ON public.status_history USING btree (ticket_id, changed_at);


--
-- Name: idx_ticket_agent_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ticket_agent_status ON public.tickets USING btree (assigned_agent_id, status);


--
-- Name: idx_ticket_customer; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ticket_customer ON public.tickets USING btree (customer_id);


--
-- Name: idx_ticket_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ticket_status ON public.tickets USING btree (status);


--
-- Name: comments comments_author_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_author_id_fkey FOREIGN KEY (author_id) REFERENCES public.users(id);


--
-- Name: comments comments_ticket_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.comments
    ADD CONSTRAINT comments_ticket_id_fkey FOREIGN KEY (ticket_id) REFERENCES public.tickets(id);


--
-- Name: status_history status_history_changed_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_changed_by_fkey FOREIGN KEY (changed_by) REFERENCES public.users(id);


--
-- Name: status_history status_history_ticket_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.status_history
    ADD CONSTRAINT status_history_ticket_id_fkey FOREIGN KEY (ticket_id) REFERENCES public.tickets(id);


--
-- Name: tickets tickets_assigned_agent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tickets
    ADD CONSTRAINT tickets_assigned_agent_id_fkey FOREIGN KEY (assigned_agent_id) REFERENCES public.users(id);


--
-- Name: tickets tickets_customer_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tickets
    ADD CONSTRAINT tickets_customer_id_fkey FOREIGN KEY (customer_id) REFERENCES public.users(id);


--
-- PostgreSQL database dump complete
--

