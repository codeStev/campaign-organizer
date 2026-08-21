--
-- PostgreSQL database dump
--

\restrict 8VzHj72nscfrrCcNdZfsevfWSDNraJboCAx1E7A4Mfg35JyfsqVDN1rsgWyLJ7A

-- Dumped from database version 16.14
-- Dumped by pg_dump version 16.14

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

--
-- Name: pg_trgm; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;


--
-- Name: EXTENSION pg_trgm; Type: COMMENT; Schema: -; Owner: 
--

COMMENT ON EXTENSION pg_trgm IS 'text similarity measurement and index searching based on trigrams';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: arc_beats; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.arc_beats (
    id uuid NOT NULL,
    arc_id uuid NOT NULL,
    session_id uuid,
    title character varying(200) NOT NULL,
    body text,
    done boolean DEFAULT false NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.arc_beats OWNER TO app;

--
-- Name: arcs; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.arcs (
    id uuid NOT NULL,
    campaign_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    description text,
    status character varying(20) DEFAULT 'PLANNED'::character varying NOT NULL,
    "position" integer DEFAULT 0 NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.arcs OWNER TO app;

--
-- Name: article_revisions; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.article_revisions (
    id uuid NOT NULL,
    article_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    slug character varying(220) NOT NULL,
    template character varying(50) NOT NULL,
    body text,
    created_at timestamp with time zone NOT NULL
);


ALTER TABLE public.article_revisions OWNER TO app;

--
-- Name: articles; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.articles (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    category_id uuid,
    title character varying(200) NOT NULL,
    slug character varying(220) NOT NULL,
    template character varying(50) DEFAULT 'GENERIC'::character varying NOT NULL,
    body text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    search_vector tsvector GENERATED ALWAYS AS ((setweight(to_tsvector('english'::regconfig, (COALESCE(title, ''::character varying))::text), 'A'::"char") || setweight(to_tsvector('english'::regconfig, regexp_replace(COALESCE(body, ''::text), '<[^>]+>'::text, ' '::text, 'g'::text)), 'B'::"char"))) STORED
);


ALTER TABLE public.articles OWNER TO app;

--
-- Name: beat_articles; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.beat_articles (
    beat_id uuid NOT NULL,
    article_id uuid NOT NULL
);


ALTER TABLE public.beat_articles OWNER TO app;

--
-- Name: beat_statblocks; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.beat_statblocks (
    beat_id uuid NOT NULL,
    statblock_id uuid NOT NULL
);


ALTER TABLE public.beat_statblocks OWNER TO app;

--
-- Name: calendar_months; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.calendar_months (
    id uuid NOT NULL,
    calendar_id uuid NOT NULL,
    "position" integer NOT NULL,
    name character varying(100) NOT NULL,
    days integer NOT NULL
);


ALTER TABLE public.calendar_months OWNER TO app;

--
-- Name: calendars; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.calendars (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    days_per_week integer,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.calendars OWNER TO app;

--
-- Name: campaigns; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.campaigns (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    notes text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.campaigns OWNER TO app;

--
-- Name: categories; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.categories (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    parent_id uuid,
    name character varying(200) NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.categories OWNER TO app;

--
-- Name: character_sheets; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.character_sheets (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    template_id uuid NOT NULL,
    article_id uuid,
    name character varying(200) NOT NULL,
    field_values jsonb DEFAULT '{}'::jsonb NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    campaign_id uuid
);


ALTER TABLE public.character_sheets OWNER TO app;

--
-- Name: field_templates; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.field_templates (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    system character varying(100),
    sections jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    kind character varying(20) DEFAULT 'CHARACTER'::character varying NOT NULL
);


ALTER TABLE public.field_templates OWNER TO app;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: app
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


ALTER TABLE public.flyway_schema_history OWNER TO app;

--
-- Name: map_pins; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.map_pins (
    id uuid NOT NULL,
    map_id uuid NOT NULL,
    article_id uuid,
    label character varying(200),
    layer character varying(100),
    x double precision NOT NULL,
    y double precision NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.map_pins OWNER TO app;

--
-- Name: maps; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.maps (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    media_id uuid,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.maps OWNER TO app;

--
-- Name: media; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.media (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    filename character varying(300) NOT NULL,
    content_type character varying(100) NOT NULL,
    size_bytes bigint NOT NULL,
    storage_key character varying(100) NOT NULL,
    created_at timestamp with time zone NOT NULL
);


ALTER TABLE public.media OWNER TO app;

--
-- Name: relationships; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.relationships (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    from_article_id uuid NOT NULL,
    to_article_id uuid NOT NULL,
    label character varying(100),
    directed boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.relationships OWNER TO app;

--
-- Name: sessions; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.sessions (
    id uuid NOT NULL,
    campaign_id uuid NOT NULL,
    title character varying(200) NOT NULL,
    session_number integer,
    date date,
    summary text,
    notes text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.sessions OWNER TO app;

--
-- Name: statblocks; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.statblocks (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    article_id uuid,
    name character varying(200) NOT NULL,
    stats jsonb DEFAULT '{}'::jsonb NOT NULL,
    notes text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    campaign_id uuid,
    template_id uuid
);


ALTER TABLE public.statblocks OWNER TO app;

--
-- Name: timeline_events; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.timeline_events (
    id uuid NOT NULL,
    timeline_id uuid NOT NULL,
    article_id uuid,
    title character varying(200) NOT NULL,
    description text,
    year integer NOT NULL,
    month integer,
    day integer,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.timeline_events OWNER TO app;

--
-- Name: timelines; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.timelines (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    calendar_id uuid
);


ALTER TABLE public.timelines OWNER TO app;

--
-- Name: whiteboards; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.whiteboards (
    id uuid NOT NULL,
    world_id uuid NOT NULL,
    name character varying(200) NOT NULL,
    nodes jsonb DEFAULT '[]'::jsonb NOT NULL,
    edges jsonb DEFAULT '[]'::jsonb NOT NULL,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL
);


ALTER TABLE public.whiteboards OWNER TO app;

--
-- Name: worlds; Type: TABLE; Schema: public; Owner: app
--

CREATE TABLE public.worlds (
    id uuid NOT NULL,
    name character varying(200) NOT NULL,
    description text,
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL,
    layer_styles jsonb DEFAULT '{}'::jsonb NOT NULL
);


ALTER TABLE public.worlds OWNER TO app;

--
-- Data for Name: arc_beats; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.arc_beats (id, arc_id, session_id, title, body, done, "position", created_at, updated_at) FROM stdin;
e97e486f-5126-4656-88cc-68bcbddec6d4	9aa0f61b-8497-4055-9153-bf54aaaea8e1	\N	L9 Sand Archives	\N	f	0	2026-08-15 23:12:47.751749+00	2026-08-15 23:12:47.751749+00
750c7bc5-3e29-4ea9-a804-e5f5b2ca7768	9aa0f61b-8497-4055-9153-bf54aaaea8e1	\N	L10 Ruins of Uxlum	\N	f	0	2026-08-15 23:12:58.583041+00	2026-08-15 23:12:58.583041+00
2b34f173-e2ac-4d87-a3c3-f4bc3e5107ed	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	Jungle Tube L1	\N	t	0	2026-08-15 23:11:39.746978+00	2026-08-16 00:13:09.529227+00
a3024792-db03-47c8-b6ce-2966e577fef6	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	L2 Grotto Tube	\N	t	0	2026-08-15 23:11:49.003009+00	2026-08-16 00:13:12.634286+00
53c89f47-01d5-451f-ae1a-8d65bd5506cb	583597d3-969f-4b7a-be40-1cf5823cb560	d761a660-f54a-4b41-a2bd-8bb64805f0c2	Redcoat Ambush	\N	t	0	2026-08-15 23:15:03.760554+00	2026-08-16 00:07:56.392875+00
1762e905-f319-471c-806c-8b4bb6ce55a9	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	L3 The Talus Maw	\N	t	0	2026-08-15 23:11:59.069174+00	2026-08-16 00:13:16.835194+00
6aaabca5-bb4d-4690-a64b-6918d7c7761a	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	L4 Echo Caves	\N	t	0	2026-08-15 23:12:04.469316+00	2026-08-16 00:13:20.097417+00
c3307dac-f444-443a-b5f4-41e80d479c16	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	L5 Glowing Pool	\N	t	0	2026-08-15 23:12:09.606159+00	2026-08-16 00:13:23.682005+00
599ac00b-9d3d-42c6-a245-2c42a3473ff0	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	L6 Mud & Iron Pits	\N	t	0	2026-08-15 23:12:22.634542+00	2026-08-16 00:13:27.14758+00
9ffde6d3-dff2-4749-afec-89898ac8f9df	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	L7 Temple of Water	\N	t	0	2026-08-15 23:12:29.999287+00	2026-08-16 00:13:30.605414+00
14b1a874-b680-416d-bcc5-45c74ba0ad3a	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	L8 Obsidian Plains	\N	t	0	2026-08-15 23:12:40.056355+00	2026-08-16 00:13:35.788638+00
cff17be3-3015-443c-a99d-2caff072cf2d	9f84d9a9-b49e-4167-9725-389918b6c32b	d761a660-f54a-4b41-a2bd-8bb64805f0c2	Finding the Wreck of the Silent Angel	\N	t	0	2026-08-15 23:07:30.908548+00	2026-08-16 00:09:31.519999+00
bd9f398a-3db3-442e-98c6-1335b2f860d2	9f84d9a9-b49e-4167-9725-389918b6c32b	d761a660-f54a-4b41-a2bd-8bb64805f0c2	Finding the redcoat boat	\N	t	0	2026-08-15 23:07:48.459701+00	2026-08-16 00:09:31.915615+00
10ffbb7c-2878-466f-84ae-890da095b7bc	9f84d9a9-b49e-4167-9725-389918b6c32b	d761a660-f54a-4b41-a2bd-8bb64805f0c2	Finding the zombie traps	\N	t	0	2026-08-15 23:08:02.785303+00	2026-08-16 00:09:32.365448+00
d0c741ed-c2d4-4e70-b4cc-c0612955a2b4	9f84d9a9-b49e-4167-9725-389918b6c32b	d761a660-f54a-4b41-a2bd-8bb64805f0c2	Discovering Scrub Island	\N	t	0	2026-08-15 23:08:11.508486+00	2026-08-16 00:09:32.830966+00
d20711ae-2594-45f2-a449-7f7963c4df13	9af69b00-3775-414e-a47b-9bef6f21770c	d761a660-f54a-4b41-a2bd-8bb64805f0c2	Handelsschiff der West India Trading Company	\N	t	0	2026-08-16 00:09:11.012293+00	2026-08-16 00:09:34.219267+00
f0739ab8-683e-464b-95bd-01eabe3eb83d	c7078b73-163d-49ba-af93-05ebe556a594	92ebde51-9311-4367-8bdf-2d97c43e2e04	Discovering St. Martin	\N	t	0	2026-08-15 23:09:17.85726+00	2026-08-16 00:12:58.82013+00
2d4c0271-03ac-46f4-af21-79d1d3fb96b9	c7078b73-163d-49ba-af93-05ebe556a594	92ebde51-9311-4367-8bdf-2d97c43e2e04	Get the other half of the treasure map	\N	t	0	2026-08-15 23:09:39.24101+00	2026-08-16 00:13:02.314666+00
b19a1dbb-69df-4872-908a-e6708b3d4fb3	9aa0f61b-8497-4055-9153-bf54aaaea8e1	92ebde51-9311-4367-8bdf-2d97c43e2e04	Discovering Pelican Island	\N	t	0	2026-08-15 23:10:44.951658+00	2026-08-16 00:13:06.162413+00
ddf7ab3a-265a-4a36-87a4-17fddcd4e136	583597d3-969f-4b7a-be40-1cf5823cb560	d761a660-f54a-4b41-a2bd-8bb64805f0c2	The Betrayal	\N	t	0	2026-08-15 23:14:54.100097+00	2026-08-19 21:11:16.658798+00
b85676a8-f0a8-4a61-907a-292c38f89cd6	583597d3-969f-4b7a-be40-1cf5823cb560	d761a660-f54a-4b41-a2bd-8bb64805f0c2	Backstory	The PCs are a gang of novice pirates that crew\nthe sloop Wharf Rat 41; one of them is captain.\nRecently, they took a job from the Spanish\nInquisition to escort conquistador Beltran de\nOrdaz and the nun Mother Iris through British-\ncontrolled waters to Eel Island 18 in hopes\nof finding the wreck of the brigantine Silent\nAngel.\nThe PCs were told the objective is to recover\na lost treasure haul. Secretly, the NPCs seek\nthe Tongue of Thoth12, an artifact with\nsignificant religious implications.\nEach PC was paid 100s (100 silver) up front\n(which the players can add to their character\nsheets). Ordaz has agreed to split profits from\nany treasure or salvage recovered 50/50 with the\ncrew. He will honor his word, but he knows there\nisn't much to be found beyond a partial map.\nAfter a few days at sea, the Wharf Rat arrives at\nEel Island in the Lesser Antilles. The crew head\nashore in the ship’s boat (or one of the players'\ndinghies, if they started with one) to search\nfor the wreck. They leave the ship’s bosun Mr.\nOxley 41 behind to watch the dinghy as they\nhead into the jungle. Mr. Oxley will soon betray\nthe PCs and maroon them.	t	0	2026-08-15 23:14:50.206534+00	2026-08-19 21:12:02.173971+00
\.


--
-- Data for Name: arcs; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.arcs (id, campaign_id, title, description, status, "position", created_at, updated_at) FROM stdin;
583597d3-969f-4b7a-be40-1cf5823cb560	2731a511-0553-4e5f-8ee2-115b5aa9be8e	Intro	\N	COMPLETED	0	2026-08-15 23:05:22.743577+00	2026-08-16 00:04:04.989814+00
9f84d9a9-b49e-4167-9725-389918b6c32b	2731a511-0553-4e5f-8ee2-115b5aa9be8e	Part 1	\N	COMPLETED	0	2026-08-15 23:05:27.297442+00	2026-08-16 00:04:06.193118+00
9af69b00-3775-414e-a47b-9bef6f21770c	2731a511-0553-4e5f-8ee2-115b5aa9be8e	Part 2	\N	COMPLETED	0	2026-08-15 23:05:31.83404+00	2026-08-16 00:04:07.812676+00
c7078b73-163d-49ba-af93-05ebe556a594	2731a511-0553-4e5f-8ee2-115b5aa9be8e	Part 3	\N	COMPLETED	0	2026-08-15 23:05:35.028898+00	2026-08-16 00:04:09.462119+00
9aa0f61b-8497-4055-9153-bf54aaaea8e1	2731a511-0553-4e5f-8ee2-115b5aa9be8e	Part 4	\N	ACTIVE	0	2026-08-15 23:05:39.07213+00	2026-08-16 00:04:28.582444+00
\.


--
-- Data for Name: article_revisions; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.article_revisions (id, article_id, title, slug, template, body, created_at) FROM stdin;
54f2cf51-50b2-498e-a4a8-0979e7568a99	eed7f3da-9d23-449c-a540-a306e782cb81	The Rpublic of Pirates	the-rpublic-of-pirates	ORGANIZATION	## Purpose\n\nthe republic of pirates wants freedom,\n\ndebauchery, and riches, in that order.\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:46:02.901798+00
10d90231-9035-4f55-8a99-d3bc84727eac	845c540b-f2e1-41ca-9036-5e643d71f1f8	The British	the-british	GENERIC		2026-08-15 23:46:35.915633+00
ed18f6c3-88a7-4a4c-88e8-456def5e847a	f047b884-c3fe-48d7-9d58-3d68f700843f	The French	the-french	GENERIC		2026-08-15 23:46:39.515944+00
7e13ee18-8c57-4ca6-b87c-f7b796fd3e7c	12bb0408-aee4-421f-a8cd-60e54244757c	The Spanish	the-spanish	GENERIC		2026-08-15 23:46:42.98089+00
421f2c5c-440a-4273-9c39-23c11c5a640d	6cb6565c-f9cd-4c6f-b7b4-ae75e03e1456	Expedition for the Tongue of Toth	expedition-for-the-tongue-of-toth	EVENT	## Summary\n\n## Causes\n\n## Consequences\n\n## Participants	2026-08-15 23:57:08.865005+00
57395236-020c-4c6f-8b3f-f8cf6cb3b04d	193e32da-1d79-4a37-acd6-577c9d2b6340	Survivor from the Silent Angel appears	survivor-from-the-silent-angel-appears	EVENT	After almost a year of terror and preparation, the survivor risked the jungle and escaped the island on a raft. He was found drifting by a patrolling ship and taken to Maracaibo.	2026-08-15 23:58:18.648629+00
edb39dec-d88b-4600-90fc-5b6b5b850114	5a5fa002-c261-4dea-b75e-3bbd2de6ca04	Founding of the Republic of Pirates	founding-of-the-republic-of-pirates	EVENT	Nassau is sacked. The “Republic of Pirates" is established, the region's first true democracy.	2026-08-16 00:00:52.509887+00
8cd36ad4-c1b8-4316-9a4c-37a4102774db	5a5fa002-c261-4dea-b75e-3bbd2de6ca04	Founding of the Republic of Pirates	founding-of-the-republic-of-pirates	EVENT	Nassau is sacked. The “[[Republic of Pirates]]" is established, the region's first true democracy.	2026-08-16 00:01:12.446056+00
308a48d1-2883-43cb-8474-2245c7937cdf	e2c4edb4-774b-4eb8-8a8d-bfb960e6b3a2	Beltran de Ordaz	beltran-de-ordaz	CHARACTER	## Appearance\n\nLate 50s, handsome, strong but trim, fine clothing, fancy but battle-worn armor.\n\n## Personality\n\nCocky. Smart. Snide. Bossy. Rochefort in The\n\nThree Musketeers. Alejandro Gillick in Sicario.\n\n## History\n\n## Goals & Motivations\n\nWants to discover the truth of the lone survivor's\n\ntale12 . To find clues to the location of the Tongue\n\nof Thoth 12 on board the Silent Angel. To not be\n\nmade a fool of, especially by pirates. To make others\n\ndo the dirty work.\n\n## Relationships\n\n*   Leads the [[Second Expedition for the Tongue of Toth]]\n    \n\n![](/api/media/56835966-fb10-4a26-a52d-b68de15ca194/content)	2026-08-16 00:18:21.08395+00
68c20a67-2915-460f-bdc7-7fbe859fd2bc	a0ce13bb-dcbd-41d7-b1d0-b2a3c08c878d	Mother Iris	mother-iris	CHARACTER	## Appearance\n\nLong, plain robes. Old, but much older than she looks. She seems to float when she walks.\n\n## Personality\n\nStern. Refined. All-knowing. Cruel. A Bene Gesserit from Dune. Maleficent from Sleeping Beauty. If she obtains the Tongue, evil Galadriel from Lord of the Rings (she can handle it) or Irina Spalko from Indiana Jones and the Crystal Skull (she can't handle it).\n\n## History\n\n## Goals & Motivations\n\nWants To find and study the Tongue of Thoth To bend the artifact's power to her own will. To learn from the eldritch minds of the Great Old Ones. To gain reputation with The Wretched cult leaders.\n\n## Relationships	2026-08-16 00:21:08.137666+00
c0006039-74bc-4a2c-a23e-465835cefde5	25fadea5-3966-49a8-805c-7493c90062c7	Tongue of Toth	tongue-of-toth	ITEM	## Description\n\n## Properties\n\n## History\n\n[[Discovery of the Tongue of Toth]]\n\n[[Expedition for the Tongue of Toth]]\n\n[[Second Expedition for the Tongue of Toth]]\n\n## Current Owner\n\n![](/api/media/3c8fa27a-6f23-4436-87db-dfdb5ba309a1/content)	2026-08-16 00:40:12.983191+00
30dc72f3-5b2c-40d5-a4bd-680658f5c1e8	a0ce13bb-dcbd-41d7-b1d0-b2a3c08c878d	Mother Iris	mother-iris	CHARACTER	## Appearance\n\nLong, plain robes. Old, but much older than she looks. She seems to float when she walks.\n\n## Personality\n\nStern. Refined. All-knowing. Cruel. A Bene Gesserit from Dune. Maleficent from Sleeping Beauty. If she obtains the Tongue, evil Galadriel from Lord of the Rings (she can handle it) or Irina Spalko from Indiana Jones and the Crystal Skull (she can't handle it).\n\n## Goals & Motivations\n\nWants To find and study the Tongue of Thoth To bend the artifact's power to her own will. To learn from the eldritch minds of the Great Old Ones. To gain reputation with The Wretched cult leaders.\n\n![](/api/media/bb6a0114-3593-43e1-9e9b-fbef56262d9b/content)	2026-08-16 00:29:49.10041+00
\.


--
-- Data for Name: articles; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.articles (id, world_id, category_id, title, slug, template, body, created_at, updated_at) FROM stdin;
eed7f3da-9d23-449c-a540-a306e782cb81	32958839-3404-4eab-8c44-dc9de184602a	\N	The Republic of Pirates	the-rpublic-of-pirates	ORGANIZATION	## Purpose\n\nthe republic of pirates wants freedom,\n\ndebauchery, and riches, in that order.\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:45:59.033589+00	2026-08-15 23:46:02.90715+00
845c540b-f2e1-41ca-9036-5e643d71f1f8	32958839-3404-4eab-8c44-dc9de184602a	\N	The British	the-british	ORGANIZATION	## Purpose\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:46:12.994996+00	2026-08-15 23:46:35.91966+00
f047b884-c3fe-48d7-9d58-3d68f700843f	32958839-3404-4eab-8c44-dc9de184602a	\N	The French	the-french	ORGANIZATION	## Purpose\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:46:23.240644+00	2026-08-15 23:46:39.520648+00
12bb0408-aee4-421f-a8cd-60e54244757c	32958839-3404-4eab-8c44-dc9de184602a	\N	The Spanish	the-spanish	ORGANIZATION	## Purpose\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:46:30.320769+00	2026-08-15 23:46:42.984761+00
2d44ac89-1b92-42d3-8ddd-4211da0d5a58	32958839-3404-4eab-8c44-dc9de184602a	\N	The West India Company	the-west-india-company	ORGANIZATION	## Purpose\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:46:56.039388+00	2026-08-15 23:46:56.039388+00
ee8a72e3-19b4-4a9d-8119-7c4706d1f8b2	32958839-3404-4eab-8c44-dc9de184602a	\N	The Inquisition	the-inquisition	ORGANIZATION	## Purpose\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:47:06.053219+00	2026-08-15 23:47:06.053219+00
6c50ca56-a0fd-4ec2-a09c-1e0e869571f0	32958839-3404-4eab-8c44-dc9de184602a	\N	The Wretched	the-wretched	ORGANIZATION	## Purpose\n\nSea cultists that aim to summon Great Old Ones and hasten Armageddon.\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:47:37.919935+00	2026-08-15 23:47:37.919935+00
c91178b2-365d-4007-a4d4-d9dfe2d1cf6e	32958839-3404-4eab-8c44-dc9de184602a	\N	The Scourge	the-scourge	ORGANIZATION	## Purpose\n\nThe in-world term for the undead. More of a force than a faction. They want the brains or bones of anyone living.\n\n## Structure\n\n## History\n\n## Notable Members	2026-08-15 23:48:03.188278+00	2026-08-15 23:48:03.188278+00
139cf3fc-3d25-41fd-a618-aacb0e7c4a4d	32958839-3404-4eab-8c44-dc9de184602a	\N	Discovery of the Tongue of Toth	discovery-of-the-tongue-of-toth	EVENT	A strange pendant found on an expedition was delivered to Inquisition scholars at The Citadel in Maracaibo.\n\nAfter months of fruitless study, one priest, seemingly unprompted, placed the pendant in his mouth. It possessed him, and through his voice, the pendant commanded him to travel to an area near the island of St. Martin.\n\nWhen the priest regained cognizance, he recounted visions of a nightmarish city inhabited by monstrous deities. The religious implications could not be ignored.	2026-08-15 23:56:20.766933+00	2026-08-15 23:56:20.766933+00
6cb6565c-f9cd-4c6f-b7b4-ae75e03e1456	32958839-3404-4eab-8c44-dc9de184602a	\N	Expedition for the Tongue of Toth	expedition-for-the-tongue-of-toth	EVENT	An expedition was mounted, and the brigantine Silent Angel set sail. The pendant led them to uninhabited Pelican Island. As they explored lava tubes under the island’s dormant volcano, the utterances of the priest became increasingly disturbing. The voice knew things it could not know; it horrified the crew. They discovered the ruins of an ancient civilization where a horrific monster— capable of disintegrating its victims into red sand—hunted them one by one. The crew panicked. Some deserted, others disappeared. Many begged for the expedition to be called off, yet they persisted.\n\nThe priest went mad. He ripped out his own tongue and replaced it with the pendant. The voice led them to an underground sea of lava. As it spoke, something churned in the lava, reigniting the dormant volcano. The tormented crew mutinied and murdered the priest, burying him in a shallow grave. The survivors fled in the Silent Angel, but the volcano rumbled, and a tsunami propelled the Silent Angel onto nearby Eel Island, where it now rests, suspended above the ground in the jungle canopy. All aboard but one were killed. The lone survivor buried the dead, but the next night they He took arose shelter fromin the the grave hanging and hunted shipwreck, him. and set alarms and traps to ward off the undead.	2026-08-15 23:56:47.392619+00	2026-08-15 23:57:08.869342+00
193e32da-1d79-4a37-acd6-577c9d2b6340	32958839-3404-4eab-8c44-dc9de184602a	\N	Survivor from the Crew of the Silent Angel appears	survivor-from-the-silent-angel-appears	EVENT	After almost a year of terror and preparation, the survivor risked the jungle and escaped the island on a raft. He was found drifting by a patrolling ship and taken to Maracaibo.	2026-08-15 23:58:04.533209+00	2026-08-15 23:58:18.652794+00
e5f2f740-f279-45fd-a80b-f0a7c04981cf	32958839-3404-4eab-8c44-dc9de184602a	\N	Second Expedition for the Tongue of Toth	second-expedition-for-the-tongue-of-toth	EVENT	The Inquisition, wary of expending more resources in English-controlled waters, dispatched conquistador Beltran de Ordaz and Mother Iris to hire a team and investigate the truth of the survivor's strange tale.	2026-08-15 23:58:58.168892+00	2026-08-15 23:58:58.168892+00
e7ee06e1-b95a-4ec1-857d-23230404a2c1	32958839-3404-4eab-8c44-dc9de184602a	\N	Destruction of Port Royal	destruction-of-port-royal	EVENT	Port Royal is destroyed in an earthquake. Thousands are killed, more are homeless.	2026-08-15 23:59:53.296672+00	2026-08-15 23:59:53.296672+00
a0ce13bb-dcbd-41d7-b1d0-b2a3c08c878d	32958839-3404-4eab-8c44-dc9de184602a	\N	Mother Iris	mother-iris	CHARACTER	## Appearance\n\nLong, plain robes. Old, but much older than she looks. She seems to float when she walks.\n\n## Personality\n\nStern. Refined. All-knowing. Cruel. A Bene Gesserit from Dune. Maleficent from Sleeping Beauty. If she obtains the Tongue, evil Galadriel from Lord of the Rings (she can handle it) or Irina Spalko from Indiana Jones and the Crystal Skull (she can't handle it).\n\n## Goals & Motivations\n\nWants To find and study the Tongue of Thoth To bend the artifact's power to her own will. To learn from the eldritch minds of the Great Old Ones. To gain reputation with The Wretched cult leaders.\n\n*   Accompanies the [[Second Expedition for the Tongue of Toth]]\n    \n\n![](/api/media/bb6a0114-3593-43e1-9e9b-fbef56262d9b/content)	2026-08-16 00:20:11.901828+00	2026-08-16 00:29:49.105069+00
5a5fa002-c261-4dea-b75e-3bbd2de6ca04	32958839-3404-4eab-8c44-dc9de184602a	\N	Founding of the Republic of Pirates	founding-of-the-republic-of-pirates	EVENT	Nassau is sacked. "[[The Republic of Pirates]]" is established, the region's first true democracy.	2026-08-16 00:00:40.488372+00	2026-08-16 00:01:12.44991+00
e2c4edb4-774b-4eb8-8a8d-bfb960e6b3a2	32958839-3404-4eab-8c44-dc9de184602a	\N	Beltran de Ordaz	beltran-de-ordaz	CHARACTER	## Appearance\n\nLate 50s, handsome, strong but trim, fine clothing, fancy but battle-worn armor.\n\n## Personality\n\nCocky. Smart. Snide. Bossy. Rochefort in The\n\nThree Musketeers. Alejandro Gillick in Sicario.\n\n## Goals & Motivations\n\nWants to discover the truth of the lone survivor's\n\ntale12 . To find clues to the location of the Tongue\n\nof Thoth 12 on board the Silent Angel. To not be\n\nmade a fool of, especially by pirates. To make others\n\ndo the dirty work.\n\n## Relationships\n\n*   Leads the [[Second Expedition for the Tongue of Toth]]\n    \n\n![](/api/media/56835966-fb10-4a26-a52d-b68de15ca194/content)	2026-08-16 00:18:01.205496+00	2026-08-16 00:18:21.088203+00
25fadea5-3966-49a8-805c-7493c90062c7	32958839-3404-4eab-8c44-dc9de184602a	\N	Tongue of Toth	tongue-of-toth	ITEM	## Description\n\n## Properties\n\nidea: Any PC that puts it in their mouth must make a spirit test or roll on the ASH experience table using a d10 instead of a d20.\n\nSee Now What? 56\n\nfor more ideas for\n\nwhat to do after they\n\nfind the Tongue.\n\nOther Ideas: Trapped in the Tropics Page 56\n\n## History\n\n[[Discovery of the Tongue of Toth]]\n\n[[Expedition for the Tongue of Toth]]\n\n[[Second Expedition for the Tongue of Toth]]\n\n## Current Owner\n\n![](/api/media/3c8fa27a-6f23-4436-87db-dfdb5ba309a1/content)	2026-08-16 00:37:02.739784+00	2026-08-16 00:40:12.98726+00
\.


--
-- Data for Name: beat_articles; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.beat_articles (beat_id, article_id) FROM stdin;
b85676a8-f0a8-4a61-907a-292c38f89cd6	139cf3fc-3d25-41fd-a618-aacb0e7c4a4d
b85676a8-f0a8-4a61-907a-292c38f89cd6	6cb6565c-f9cd-4c6f-b7b4-ae75e03e1456
b85676a8-f0a8-4a61-907a-292c38f89cd6	e5f2f740-f279-45fd-a80b-f0a7c04981cf
\.


--
-- Data for Name: beat_statblocks; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.beat_statblocks (beat_id, statblock_id) FROM stdin;
\.


--
-- Data for Name: calendar_months; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.calendar_months (id, calendar_id, "position", name, days) FROM stdin;
2809967e-911b-4f27-b8d8-6d78c798136c	02ca4483-5cae-425f-a1b0-f501579581f3	0	Januar	30
2e7cb359-f6a9-4e15-af00-38d74a3c0951	02ca4483-5cae-425f-a1b0-f501579581f3	1	Februar	30
f5b81e60-2f15-4dc6-9073-136f88cba929	02ca4483-5cae-425f-a1b0-f501579581f3	2	März	30
88997d77-e499-452b-93ea-d7ea2ee6c4c8	02ca4483-5cae-425f-a1b0-f501579581f3	3	April	30
2f30998a-71fc-4b6a-bdd2-7b1a93447833	02ca4483-5cae-425f-a1b0-f501579581f3	4	Mai	30
69a3c015-a5d5-4f75-bb4e-589a79dd2d8c	02ca4483-5cae-425f-a1b0-f501579581f3	5	Juni	30
a2d1c630-5378-48fc-99e6-46207255a803	02ca4483-5cae-425f-a1b0-f501579581f3	6	Juli	30
f829b89a-d44f-42a2-bf89-f5e2a016b5b5	02ca4483-5cae-425f-a1b0-f501579581f3	7	August	30
413c6dee-3f31-4d02-b135-27215553c5b9	02ca4483-5cae-425f-a1b0-f501579581f3	8	September	30
67b753e2-2c7d-46f9-b9f9-b999e7640e7c	02ca4483-5cae-425f-a1b0-f501579581f3	9	Oktober	30
bd09fe0b-216a-4757-86a0-f445b922722f	02ca4483-5cae-425f-a1b0-f501579581f3	10	November	30
c77711c5-f1cb-4bd8-bb7a-b42c635be0c2	02ca4483-5cae-425f-a1b0-f501579581f3	11	Dezember	30
\.


--
-- Data for Name: calendars; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.calendars (id, world_id, name, days_per_week, created_at, updated_at) FROM stdin;
02ca4483-5cae-425f-a1b0-f501579581f3	32958839-3404-4eab-8c44-dc9de184602a	Gregorianischer Kalender (Simplifiziert)	7	2026-08-15 23:54:21.069638+00	2026-08-15 23:54:21.069638+00
\.


--
-- Data for Name: campaigns; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.campaigns (id, world_id, name, description, notes, created_at, updated_at) FROM stdin;
2731a511-0553-4e5f-8ee2-115b5aa9be8e	32958839-3404-4eab-8c44-dc9de184602a	Trapped in the Tropics	\N	The PCs have been hired by the Spanish\nInquisition to investigate a shipwreck in the\njungles of Eel Island. They are told it’s filled\nwith treasure, but the only thing of real value on\nboard is part of a map that leads to nearby Pelican\nIsland, where a newly awakened volcano will\nsoon erupt. In secret, the Inquisition is searching\nfor the Tongue of Thoth, a powerful artifact.	2026-08-15 23:04:11.076835+00	2026-08-15 23:05:16.56914+00
\.


--
-- Data for Name: categories; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.categories (id, world_id, parent_id, name, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: character_sheets; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.character_sheets (id, world_id, template_id, article_id, name, field_values, created_at, updated_at, campaign_id) FROM stdin;
\.


--
-- Data for Name: field_templates; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.field_templates (id, world_id, name, system, sections, created_at, updated_at, kind) FROM stdin;
c6a76c81-d800-4c97-8c2e-2933e3e7f880	32958839-3404-4eab-8c44-dc9de184602a	NPC	pirate borg	[{"title": "Section", "fields": []}]	2026-08-20 20:48:58.681315+00	2026-08-20 20:48:58.681315+00	STATBLOCK
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	init	SQL	V1__init.sql	-47521405	app	2026-08-15 22:55:21.003838	25	t
2	2	articles and categories	SQL	V2__articles_and_categories.sql	-1708021352	app	2026-08-15 22:55:21.043943	60	t
3	3	media	SQL	V3__media.sql	1179485368	app	2026-08-15 22:55:21.11512	22	t
4	4	article search vector	SQL	V4__article_search_vector.sql	-539910368	app	2026-08-15 22:55:21.145171	37	t
5	5	maps	SQL	V5__maps.sql	-2129626768	app	2026-08-15 22:55:21.190563	44	t
6	6	timelines	SQL	V6__timelines.sql	1435143834	app	2026-08-15 22:55:21.247324	51	t
7	7	calendars	SQL	V7__calendars.sql	-635160679	app	2026-08-15 22:55:21.30559	29	t
8	8	relationships	SQL	V8__relationships.sql	380838782	app	2026-08-15 22:55:21.341407	29	t
9	9	fuzzy search	SQL	V9__fuzzy_search.sql	-1691422676	app	2026-08-15 22:55:21.376931	11	t
10	10	campaigns	SQL	V10__campaigns.sql	561089030	app	2026-08-15 22:55:21.393833	44	t
11	11	arcs	SQL	V11__arcs.sql	988265359	app	2026-08-15 22:55:21.444855	44	t
12	12	sheet templates	SQL	V12__sheet_templates.sql	1371081029	app	2026-08-15 22:55:21.495776	22	t
13	13	character sheets	SQL	V13__character_sheets.sql	-1121061263	app	2026-08-15 22:55:21.523727	30	t
14	14	statblocks	SQL	V14__statblocks.sql	1501228339	app	2026-08-15 22:55:21.560592	22	t
15	15	article revisions	SQL	V15__article_revisions.sql	1229829302	app	2026-08-15 22:55:21.588606	21	t
16	16	whiteboards	SQL	V16__whiteboards.sql	1203299310	app	2026-08-15 22:55:21.615969	21	t
17	17	character sheet campaign	SQL	V17__character_sheet_campaign.sql	1913229840	app	2026-08-15 22:55:21.643185	8	t
18	18	statblock campaign	SQL	V18__statblock_campaign.sql	-1683304129	app	2026-08-15 22:55:21.657377	8	t
19	19	beat articles	SQL	V19__beat_articles.sql	1897279552	app	2026-08-15 22:55:21.670994	17	t
20	20	beat statblocks	SQL	V20__beat_statblocks.sql	-506008439	app	2026-08-15 22:55:21.695361	15	t
21	21	world layer styles	SQL	V21__world_layer_styles.sql	1206476399	app	2026-08-15 22:55:21.716001	1	t
22	22	field template kind	SQL	V22__field_template_kind.sql	-822459319	app	2026-08-20 18:10:43.239276	19	t
23	23	statblock template	SQL	V23__statblock_template.sql	-2037102175	app	2026-08-20 18:10:43.278347	10	t
\.


--
-- Data for Name: map_pins; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.map_pins (id, map_id, article_id, label, layer, x, y, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: maps; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.maps (id, world_id, name, media_id, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: media; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.media (id, world_id, filename, content_type, size_bytes, storage_key, created_at) FROM stdin;
ebc5958d-349e-4bdf-b51a-65221b17e0fb	32958839-3404-4eab-8c44-dc9de184602a	Beltran.png	image/png	278517	6412a4a1-28de-466b-b36e-b664db02045a	2026-08-16 00:16:58.417717+00
67c42328-4d4b-4aef-b10b-4cfa9f24ed4d	32958839-3404-4eab-8c44-dc9de184602a	content.png	image/png	278517	685530a3-55f2-4331-89ce-48d392ddb5c6	2026-08-16 00:17:01.535289+00
df9ef01f-b732-45cc-9e8e-550a55688b3d	32958839-3404-4eab-8c44-dc9de184602a	content.png	image/png	278517	78692c48-b117-4531-808f-4c8d4d623e16	2026-08-16 00:17:06.317254+00
56835966-fb10-4a26-a52d-b68de15ca194	32958839-3404-4eab-8c44-dc9de184602a	Beltran.png	image/png	278517	e053a5ed-17d5-47eb-a8df-790677ab6e3b	2026-08-16 00:17:19.552156+00
bb6a0114-3593-43e1-9e9b-fbef56262d9b	32958839-3404-4eab-8c44-dc9de184602a	Mother_Iris.png	image/png	135014	20f9e285-faab-43f6-93df-824dd26527c0	2026-08-16 00:20:57.729698+00
3c8fa27a-6f23-4436-87db-dfdb5ba309a1	32958839-3404-4eab-8c44-dc9de184602a	Tongue_of_toth.png	image/png	412151	5050ff31-ad72-478a-8b01-42b2ec0107d1	2026-08-16 00:35:59.190943+00
\.


--
-- Data for Name: relationships; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.relationships (id, world_id, from_article_id, to_article_id, label, directed, created_at, updated_at) FROM stdin;
dc933169-728d-4d7c-8c20-21634c59280c	32958839-3404-4eab-8c44-dc9de184602a	12bb0408-aee4-421f-a8cd-60e54244757c	f047b884-c3fe-48d7-9d58-3d68f700843f	at war with	f	2026-08-16 00:02:51.227735+00	2026-08-16 00:02:51.227735+00
bc924a1f-85a6-4007-9840-3450220613e6	32958839-3404-4eab-8c44-dc9de184602a	12bb0408-aee4-421f-a8cd-60e54244757c	845c540b-f2e1-41ca-9036-5e643d71f1f8	at war with	f	2026-08-16 00:02:58.156086+00	2026-08-16 00:02:58.156086+00
e87a5bda-3b48-4673-9e50-29705bf6510d	32958839-3404-4eab-8c44-dc9de184602a	f047b884-c3fe-48d7-9d58-3d68f700843f	845c540b-f2e1-41ca-9036-5e643d71f1f8	at war with	f	2026-08-16 00:03:05.250894+00	2026-08-16 00:03:05.250894+00
\.


--
-- Data for Name: sessions; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.sessions (id, campaign_id, title, session_number, date, summary, notes, created_at, updated_at) FROM stdin;
d761a660-f54a-4b41-a2bd-8bb64805f0c2	2731a511-0553-4e5f-8ee2-115b5aa9be8e	Intro to Trapped in the Tropics	1	2026-03-13	\N	\N	2026-08-16 00:06:54.240234+00	2026-08-16 00:06:54.240234+00
92ebde51-9311-4367-8bdf-2d97c43e2e04	2731a511-0553-4e5f-8ee2-115b5aa9be8e	Auf nach St. Martin	2	2026-05-30	\N	\N	2026-08-16 00:11:58.252103+00	2026-08-16 00:11:58.252103+00
\.


--
-- Data for Name: statblocks; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.statblocks (id, world_id, article_id, name, stats, notes, created_at, updated_at, campaign_id, template_id) FROM stdin;
fb35c835-c870-4541-85a7-42527f67c88b	32958839-3404-4eab-8c44-dc9de184602a	\N	Mother Iris	{"HP": 30, "Armor": "No Armor", "Morale": 11}	### Attacks\n- Cross and Rosary d2 (melee) or d12 against undead\n(ranged, only works in her hands)\n### Abilities\n- Divine Wrath One target loses d8 hp (ignore armor).\n- Angelic Heal Everyone within 10' heals d6 hp.	2026-08-16 00:24:03.801018+00	2026-08-16 00:28:13.233117+00	2731a511-0553-4e5f-8ee2-115b5aa9be8e	\N
21cc9d86-2b5e-4f86-909b-6cd5f60eba45	32958839-3404-4eab-8c44-dc9de184602a	\N	Beltran de Ordaz	{"HP": 20, "Armor": "Conquistador Plate: -d6", "Morale": 9}	### Attacks\nFlintlock 3d4 (2d4 for anyone else who wields it)\nd Finely Craf ted Rapier d8\n### Abilities\nSuperior Tactician Once per combat, he can let one\nally reroll an attack, defense, or damage roll.	2026-08-16 00:27:59.829586+00	2026-08-20 18:12:16.207433+00	2731a511-0553-4e5f-8ee2-115b5aa9be8e	\N
\.


--
-- Data for Name: timeline_events; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.timeline_events (id, timeline_id, article_id, title, description, year, month, day, created_at, updated_at) FROM stdin;
9fbb224d-63c6-4c6b-bf44-24d171f7f670	c50e0b89-1f07-4ec9-96ab-67a879f10459	\N	Discovery of Ash	\N	1693	\N	\N	2026-08-15 23:50:30.125799+00	2026-08-15 23:50:30.125799+00
af37d5d6-1dce-4fc9-b9c1-a1a68c0305cb	c50e0b89-1f07-4ec9-96ab-67a879f10459	\N	First Rise of the Undead	\N	1694	\N	\N	2026-08-15 23:51:29.295663+00	2026-08-15 23:51:29.295663+00
b0a182b8-19bb-4da8-b1b5-7ab3d53748a8	c50e0b89-1f07-4ec9-96ab-67a879f10459	\N	Black Market around ASH	\N	1697	\N	\N	2026-08-15 23:52:17.079866+00	2026-08-15 23:52:33.753748+00
c23499c9-9583-4dc6-a6e6-61e4a3bcd0d5	0916c6ba-7460-4b0e-ad2f-e6654802d4d7	6cb6565c-f9cd-4c6f-b7b4-ae75e03e1456	Expedition for the Tongue of Toth	\N	1694	\N	\N	2026-08-15 23:41:47.590939+00	2026-08-15 23:57:23.297332+00
049fc6ee-b20b-47cf-b4e3-054a60b7f064	0916c6ba-7460-4b0e-ad2f-e6654802d4d7	139cf3fc-3d25-41fd-a618-aacb0e7c4a4d	Discovery of the Tongue of Toth	\N	1693	\N	\N	2026-08-15 23:32:31.293693+00	2026-08-15 23:56:30.113999+00
8e71cfa8-5eb8-4aea-9d55-38e7bf5ac580	0916c6ba-7460-4b0e-ad2f-e6654802d4d7	193e32da-1d79-4a37-acd6-577c9d2b6340	Survivor from the Crew of the Silent Angel appears	\N	1695	\N	\N	2026-08-15 23:42:46.034911+00	2026-08-15 23:58:28.362434+00
1a93101c-26f7-4e69-88ab-0189d2e23ceb	0916c6ba-7460-4b0e-ad2f-e6654802d4d7	e5f2f740-f279-45fd-a80b-f0a7c04981cf	Second Expedition for the Tongue of Toth	\N	1698	5	\N	2026-08-15 23:44:10.043427+00	2026-08-15 23:59:17.97733+00
d974c1f3-820b-46ce-95ef-882ef9170f75	c50e0b89-1f07-4ec9-96ab-67a879f10459	e7ee06e1-b95a-4ec1-857d-23230404a2c1	Destruction of Port Royal	\N	1692	\N	\N	2026-08-15 23:49:47.47183+00	2026-08-16 00:00:01.997246+00
8a65a396-dd5b-4a99-8872-ca778e667b01	c50e0b89-1f07-4ec9-96ab-67a879f10459	5a5fa002-c261-4dea-b75e-3bbd2de6ca04	 Founding of The Republic of Pirates	Nassau is sacked. The “Republic of Pirates" is established, the region's first true democracy.	1694	\N	\N	2026-08-15 23:50:55.558792+00	2026-08-16 00:01:29.615502+00
\.


--
-- Data for Name: timelines; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.timelines (id, world_id, name, description, created_at, updated_at, calendar_id) FROM stdin;
0916c6ba-7460-4b0e-ad2f-e6654802d4d7	32958839-3404-4eab-8c44-dc9de184602a	The tongue of toth	\N	2026-08-15 23:16:18.309423+00	2026-08-15 23:54:26.92411+00	02ca4483-5cae-425f-a1b0-f501579581f3
c50e0b89-1f07-4ec9-96ab-67a879f10459	32958839-3404-4eab-8c44-dc9de184602a	General History	\N	2026-08-15 23:49:26.017258+00	2026-08-15 23:54:31.555804+00	02ca4483-5cae-425f-a1b0-f501579581f3
\.


--
-- Data for Name: whiteboards; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.whiteboards (id, world_id, name, nodes, edges, created_at, updated_at) FROM stdin;
f6b699e1-ca44-4316-a825-d323e6ac9751	32958839-3404-4eab-8c44-dc9de184602a	aaa	[]	[]	2026-08-20 21:55:39.435213+00	2026-08-20 21:55:39.435213+00
\.


--
-- Data for Name: worlds; Type: TABLE DATA; Schema: public; Owner: app
--

COPY public.worlds (id, name, description, created_at, updated_at, layer_styles) FROM stdin;
32958839-3404-4eab-8c44-dc9de184602a	Dark Caribbean	The Dark Caribbean is a grim, supernatural alternate history of the 17th-century Caribbean where the 1692 Port Royal earthquake tore open a rift in the ocean, unleashing undead hordes, eldritch horrors, and black magic upon a world actively spiraling toward Armageddon.	2026-08-15 23:03:56.733681+00	2026-08-15 23:03:56.733681+00	{}
\.


--
-- Name: arc_beats arc_beats_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.arc_beats
    ADD CONSTRAINT arc_beats_pkey PRIMARY KEY (id);


--
-- Name: arcs arcs_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.arcs
    ADD CONSTRAINT arcs_pkey PRIMARY KEY (id);


--
-- Name: article_revisions article_revisions_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.article_revisions
    ADD CONSTRAINT article_revisions_pkey PRIMARY KEY (id);


--
-- Name: articles articles_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_pkey PRIMARY KEY (id);


--
-- Name: calendar_months calendar_months_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.calendar_months
    ADD CONSTRAINT calendar_months_pkey PRIMARY KEY (id);


--
-- Name: calendars calendars_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.calendars
    ADD CONSTRAINT calendars_pkey PRIMARY KEY (id);


--
-- Name: campaigns campaigns_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_pkey PRIMARY KEY (id);


--
-- Name: categories categories_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);


--
-- Name: character_sheets character_sheets_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.character_sheets
    ADD CONSTRAINT character_sheets_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: map_pins map_pins_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.map_pins
    ADD CONSTRAINT map_pins_pkey PRIMARY KEY (id);


--
-- Name: maps maps_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.maps
    ADD CONSTRAINT maps_pkey PRIMARY KEY (id);


--
-- Name: media media_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT media_pkey PRIMARY KEY (id);


--
-- Name: relationships relationships_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT relationships_pkey PRIMARY KEY (id);


--
-- Name: sessions sessions_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.sessions
    ADD CONSTRAINT sessions_pkey PRIMARY KEY (id);


--
-- Name: field_templates sheet_templates_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.field_templates
    ADD CONSTRAINT sheet_templates_pkey PRIMARY KEY (id);


--
-- Name: statblocks statblocks_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.statblocks
    ADD CONSTRAINT statblocks_pkey PRIMARY KEY (id);


--
-- Name: timeline_events timeline_events_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.timeline_events
    ADD CONSTRAINT timeline_events_pkey PRIMARY KEY (id);


--
-- Name: timelines timelines_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.timelines
    ADD CONSTRAINT timelines_pkey PRIMARY KEY (id);


--
-- Name: articles uq_articles_world_slug; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT uq_articles_world_slug UNIQUE (world_id, slug);


--
-- Name: whiteboards whiteboards_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.whiteboards
    ADD CONSTRAINT whiteboards_pkey PRIMARY KEY (id);


--
-- Name: worlds worlds_pkey; Type: CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.worlds
    ADD CONSTRAINT worlds_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_arcs_campaign_order; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_arcs_campaign_order ON public.arcs USING btree (campaign_id, "position");


--
-- Name: idx_article_revisions_article; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_article_revisions_article ON public.article_revisions USING btree (article_id, created_at DESC);


--
-- Name: idx_articles_category; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_articles_category ON public.articles USING btree (category_id);


--
-- Name: idx_articles_search; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_articles_search ON public.articles USING gin (search_vector);


--
-- Name: idx_articles_title_trgm; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_articles_title_trgm ON public.articles USING gin (title public.gin_trgm_ops);


--
-- Name: idx_articles_world_created; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_articles_world_created ON public.articles USING btree (world_id, created_at DESC);


--
-- Name: idx_beat_articles_article; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_beat_articles_article ON public.beat_articles USING btree (article_id);


--
-- Name: idx_beat_articles_beat; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_beat_articles_beat ON public.beat_articles USING btree (beat_id);


--
-- Name: idx_beat_statblocks_beat; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_beat_statblocks_beat ON public.beat_statblocks USING btree (beat_id);


--
-- Name: idx_beat_statblocks_statblock; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_beat_statblocks_statblock ON public.beat_statblocks USING btree (statblock_id);


--
-- Name: idx_beats_arc_order; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_beats_arc_order ON public.arc_beats USING btree (arc_id, "position");


--
-- Name: idx_calendar_months_cal; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_calendar_months_cal ON public.calendar_months USING btree (calendar_id, "position");


--
-- Name: idx_calendars_world_created; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_calendars_world_created ON public.calendars USING btree (world_id, created_at DESC);


--
-- Name: idx_campaigns_world_created; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_campaigns_world_created ON public.campaigns USING btree (world_id, created_at DESC);


--
-- Name: idx_categories_parent; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_categories_parent ON public.categories USING btree (parent_id);


--
-- Name: idx_categories_world; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_categories_world ON public.categories USING btree (world_id);


--
-- Name: idx_character_sheets_campaign; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_character_sheets_campaign ON public.character_sheets USING btree (world_id, campaign_id);


--
-- Name: idx_character_sheets_template; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_character_sheets_template ON public.character_sheets USING btree (template_id);


--
-- Name: idx_character_sheets_world; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_character_sheets_world ON public.character_sheets USING btree (world_id, created_at DESC);


--
-- Name: idx_events_timeline_order; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_events_timeline_order ON public.timeline_events USING btree (timeline_id, year, month, day);


--
-- Name: idx_field_templates_kind; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_field_templates_kind ON public.field_templates USING btree (world_id, kind, created_at DESC);


--
-- Name: idx_field_templates_world; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_field_templates_world ON public.field_templates USING btree (world_id, created_at DESC);


--
-- Name: idx_map_pins_map; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_map_pins_map ON public.map_pins USING btree (map_id);


--
-- Name: idx_maps_world_created; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_maps_world_created ON public.maps USING btree (world_id, created_at DESC);


--
-- Name: idx_media_world_created; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_media_world_created ON public.media USING btree (world_id, created_at DESC);


--
-- Name: idx_relationships_from; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_relationships_from ON public.relationships USING btree (from_article_id);


--
-- Name: idx_relationships_to; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_relationships_to ON public.relationships USING btree (to_article_id);


--
-- Name: idx_relationships_world; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_relationships_world ON public.relationships USING btree (world_id);


--
-- Name: idx_sessions_campaign_order; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_sessions_campaign_order ON public.sessions USING btree (campaign_id, session_number, date);


--
-- Name: idx_statblocks_campaign; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_statblocks_campaign ON public.statblocks USING btree (world_id, campaign_id);


--
-- Name: idx_statblocks_template; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_statblocks_template ON public.statblocks USING btree (template_id);


--
-- Name: idx_statblocks_world; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_statblocks_world ON public.statblocks USING btree (world_id, created_at DESC);


--
-- Name: idx_timelines_world_created; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_timelines_world_created ON public.timelines USING btree (world_id, created_at DESC);


--
-- Name: idx_whiteboards_world; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_whiteboards_world ON public.whiteboards USING btree (world_id, created_at DESC);


--
-- Name: idx_worlds_created_at; Type: INDEX; Schema: public; Owner: app
--

CREATE INDEX idx_worlds_created_at ON public.worlds USING btree (created_at DESC);


--
-- Name: arc_beats arc_beats_arc_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.arc_beats
    ADD CONSTRAINT arc_beats_arc_id_fkey FOREIGN KEY (arc_id) REFERENCES public.arcs(id) ON DELETE CASCADE;


--
-- Name: arc_beats arc_beats_session_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.arc_beats
    ADD CONSTRAINT arc_beats_session_id_fkey FOREIGN KEY (session_id) REFERENCES public.sessions(id) ON DELETE SET NULL;


--
-- Name: arcs arcs_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.arcs
    ADD CONSTRAINT arcs_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: article_revisions article_revisions_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.article_revisions
    ADD CONSTRAINT article_revisions_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;


--
-- Name: articles articles_category_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_category_id_fkey FOREIGN KEY (category_id) REFERENCES public.categories(id) ON DELETE SET NULL;


--
-- Name: articles articles_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.articles
    ADD CONSTRAINT articles_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: beat_articles beat_articles_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.beat_articles
    ADD CONSTRAINT beat_articles_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE CASCADE;


--
-- Name: beat_articles beat_articles_beat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.beat_articles
    ADD CONSTRAINT beat_articles_beat_id_fkey FOREIGN KEY (beat_id) REFERENCES public.arc_beats(id) ON DELETE CASCADE;


--
-- Name: beat_statblocks beat_statblocks_beat_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.beat_statblocks
    ADD CONSTRAINT beat_statblocks_beat_id_fkey FOREIGN KEY (beat_id) REFERENCES public.arc_beats(id) ON DELETE CASCADE;


--
-- Name: beat_statblocks beat_statblocks_statblock_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.beat_statblocks
    ADD CONSTRAINT beat_statblocks_statblock_id_fkey FOREIGN KEY (statblock_id) REFERENCES public.statblocks(id) ON DELETE CASCADE;


--
-- Name: calendar_months calendar_months_calendar_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.calendar_months
    ADD CONSTRAINT calendar_months_calendar_id_fkey FOREIGN KEY (calendar_id) REFERENCES public.calendars(id) ON DELETE CASCADE;


--
-- Name: calendars calendars_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.calendars
    ADD CONSTRAINT calendars_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: campaigns campaigns_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.campaigns
    ADD CONSTRAINT campaigns_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: categories categories_parent_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.categories(id) ON DELETE CASCADE;


--
-- Name: categories categories_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.categories
    ADD CONSTRAINT categories_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: character_sheets character_sheets_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.character_sheets
    ADD CONSTRAINT character_sheets_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE SET NULL;


--
-- Name: character_sheets character_sheets_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.character_sheets
    ADD CONSTRAINT character_sheets_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE SET NULL;


--
-- Name: character_sheets character_sheets_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.character_sheets
    ADD CONSTRAINT character_sheets_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.field_templates(id) ON DELETE CASCADE;


--
-- Name: character_sheets character_sheets_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.character_sheets
    ADD CONSTRAINT character_sheets_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: map_pins map_pins_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.map_pins
    ADD CONSTRAINT map_pins_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE SET NULL;


--
-- Name: map_pins map_pins_map_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.map_pins
    ADD CONSTRAINT map_pins_map_id_fkey FOREIGN KEY (map_id) REFERENCES public.maps(id) ON DELETE CASCADE;


--
-- Name: maps maps_media_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.maps
    ADD CONSTRAINT maps_media_id_fkey FOREIGN KEY (media_id) REFERENCES public.media(id) ON DELETE SET NULL;


--
-- Name: maps maps_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.maps
    ADD CONSTRAINT maps_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: media media_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.media
    ADD CONSTRAINT media_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: relationships relationships_from_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT relationships_from_article_id_fkey FOREIGN KEY (from_article_id) REFERENCES public.articles(id) ON DELETE CASCADE;


--
-- Name: relationships relationships_to_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT relationships_to_article_id_fkey FOREIGN KEY (to_article_id) REFERENCES public.articles(id) ON DELETE CASCADE;


--
-- Name: relationships relationships_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.relationships
    ADD CONSTRAINT relationships_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: sessions sessions_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.sessions
    ADD CONSTRAINT sessions_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE CASCADE;


--
-- Name: field_templates sheet_templates_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.field_templates
    ADD CONSTRAINT sheet_templates_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: statblocks statblocks_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.statblocks
    ADD CONSTRAINT statblocks_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE SET NULL;


--
-- Name: statblocks statblocks_campaign_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.statblocks
    ADD CONSTRAINT statblocks_campaign_id_fkey FOREIGN KEY (campaign_id) REFERENCES public.campaigns(id) ON DELETE SET NULL;


--
-- Name: statblocks statblocks_template_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.statblocks
    ADD CONSTRAINT statblocks_template_id_fkey FOREIGN KEY (template_id) REFERENCES public.field_templates(id) ON DELETE SET NULL;


--
-- Name: statblocks statblocks_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.statblocks
    ADD CONSTRAINT statblocks_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: timeline_events timeline_events_article_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.timeline_events
    ADD CONSTRAINT timeline_events_article_id_fkey FOREIGN KEY (article_id) REFERENCES public.articles(id) ON DELETE SET NULL;


--
-- Name: timeline_events timeline_events_timeline_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.timeline_events
    ADD CONSTRAINT timeline_events_timeline_id_fkey FOREIGN KEY (timeline_id) REFERENCES public.timelines(id) ON DELETE CASCADE;


--
-- Name: timelines timelines_calendar_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.timelines
    ADD CONSTRAINT timelines_calendar_id_fkey FOREIGN KEY (calendar_id) REFERENCES public.calendars(id) ON DELETE SET NULL;


--
-- Name: timelines timelines_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.timelines
    ADD CONSTRAINT timelines_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- Name: whiteboards whiteboards_world_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: app
--

ALTER TABLE ONLY public.whiteboards
    ADD CONSTRAINT whiteboards_world_id_fkey FOREIGN KEY (world_id) REFERENCES public.worlds(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict 8VzHj72nscfrrCcNdZfsevfWSDNraJboCAx1E7A4Mfg35JyfsqVDN1rsgWyLJ7A

