CREATE DATABASE RPG_telegrambot

CREATE SCHEMA public;
CREATE SCHEMA fixed;

drop table if exists public.party cascade;
drop table if exists public.equipment cascade;
drop table if exists public.inventory_cell cascade;
drop table if exists public.ingame_item cascade;
drop table if exists fixed.solo_activity_reward cascade;
drop table if exists fixed.activity_result_message cascade;
drop table if exists fixed.solo_activity cascade;
drop table if exists fixed.base_item cascade;
drop table if exists fixed.item_types cascade;
drop table if exists public.applied_skill cascade;
drop table if exists public.usr cascade;
drop table if exists fixed.skill_bonus cascade;
drop table if exists fixed.skill cascade;
drop table if exists fixed.class cascade;

CREATE TABLE fixed.class (
    id serial PRIMARY KEY,
    name text,
    description text,
    required_level bigint,
    base_class bigint references fixed.class(id)
);

CREATE TABLE fixed.skill (
    id serial PRIMARY KEY,
    name text,
    description text,
    class_id bigint references fixed.class(id),
    passive_points_required bigint
);

CREATE TABLE fixed.skill_bonus (
    skill_id bigint PRIMARY KEY references fixed.skill(id),
    damage_bonus bigint,
    armor_bonus bigint,
    health_bonus bigint,
    mana_bonus bigint
);

CREATE TABLE public.usr (
    chat_id bigint PRIMARY KEY,
    name TEXT,
    level bigint,
    passive_points bigint,
    current_exp bigint,
    current_health bigint,
    max_health bigint,
    current_stamina bigint,
    max_stamina bigint,
    current_mana bigint,
    max_mana bigint,
    current_user_state TEXT,
    last_action TIMESTAMP,
    stamina_restor TIMESTAMP,
    partner_chat_id bigint references public.usr(chat_id),
    class_id bigint references fixed.class(id),
    gold bigint,
    offline_points bigint
);

CREATE TABLE public.applied_skill (
	id serial PRIMARY KEY,
	skill_id bigint references fixed.skill(id),
	user_id bigint references public.usr(chat_id),
	skill_level bigint
);

CREATE TABLE fixed.item_types (
	id bigint PRIMARY KEY,
	type text
);

CREATE TABLE fixed.base_item (
	id bigint PRIMARY KEY,
	name text,
	description text,
	damage bigint,
	armor bigint,
	type_id bigint references fixed.item_types(id),
	buy_price bigint,
	sell_price bigint
);

CREATE TABLE fixed.solo_activity (
	id serial PRIMARY KEY,
	name text,
	state_name text,
	description text,
	required_level bigint,
	activity_duration bigint

);

CREATE TABLE fixed.activity_result_message (
    id serial PRIMARY KEY,
    solo_activity_id bigint references fixed.solo_activity(id),
    result_message text
);

CREATE TABLE fixed.solo_activity_reward (
    solo_activity_id bigint PRIMARY KEY references fixed.solo_activity(id),
    gold_reward bigint,
    exp_reward bigint,
    item_reward bigint references fixed.base_item(id)
);

CREATE TABLE public.ingame_item (
	id serial PRIMARY KEY,
	item_id bigint references fixed.base_item(id),
	sharpness bigint
);

CREATE TABLE public.inventory_cell (
	item_id bigint PRIMARY KEY references public.ingame_item(id),
	user_id bigint references public.usr(chat_id)
);

CREATE TABLE public.equipment (
	user_id bigint PRIMARY KEY references public.usr(chat_id),
	helmet bigint references public.inventory_cell(item_id),
	chest_armor bigint references public.inventory_cell(item_id),
	leg_armor bigint references public.inventory_cell(item_id),
	boots bigint references public.inventory_cell(item_id),
	left_hand bigint references public.inventory_cell(item_id),
	right_hand bigint references public.inventory_cell(item_id)
);

CREATE TABLE public.party (
	user_id bigint PRIMARY KEY references public.usr(chat_id),
	name TEXT UNIQUE,
	current_state TEXT
);















