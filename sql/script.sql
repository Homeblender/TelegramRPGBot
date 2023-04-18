drop database if exists RPG_telegrambot;
CREATE DATABASE  RPG_telegrambot;
drop schema if exists public cascade;
drop schema if exists  fixed cascade;
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
drop table if exists public.fight cascade;
drop table if exists public.move cascade;

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
    skill_bonus_id serial primary key,
    skill_id bigint references fixed.skill(id),
    damage_bonus bigint,
    armor_bonus bigint,
    health_bonus bigint,
    mana_bonus bigint
);
CREATE TABLE fixed.solo_activity (
	id serial PRIMARY KEY,
	name text,
	state_name text,
	description text,
	required_level bigint,
	required_stamina bigint,
	activity_duration bigint
);

CREATE TABLE public.usr (
    chat_id bigint PRIMARY KEY,
    name TEXT,
    level bigint,
    passive_points bigint,
    current_health bigint,
    max_health bigint,
    current_stamina bigint,
    max_stamina bigint,
    current_mana bigint,
    max_mana bigint,
    user_state integer,
    activity_ends TIMESTAMP,
    activity_id bigint references fixed.solo_activity(id),
    stamina_restor TIMESTAMP,
    partner_chat_id bigint references public.usr(chat_id),
    class_id bigint references fixed.class(id),
    gold bigint,
    exp bigint,
    offline_points bigint
);

CREATE TABLE public.applied_skill (
	id serial PRIMARY KEY,
	skill_id bigint references fixed.skill(id),
	user_id bigint references public.usr(chat_id),
	skill_level bigint
);

CREATE TABLE fixed.base_item (
	id serial PRIMARY KEY,
	name text,
	description text,
	damage bigint,
	armor bigint,
	type bigint,
	max_in_stack bigint,
	is_for_sale boolean,
	class_required_id bigint references fixed.class(id) default (1),
	buy_price bigint
);
CREATE TABLE fixed.base_item_craft (
    id serial PRIMARY KEY,
	crafted_base_item_id bigint references fixed.base_item(id),
	material_base_item_id bigint references fixed.base_item(id),
	countOfMaterial bigint
);

CREATE TABLE fixed.solo_activity_reward (
    id serial PRIMARY KEY,
    solo_activity_id bigint references fixed.solo_activity(id),
    gold_reward bigint,
    exp_reward bigint,
    item_reward bigint references fixed.base_item(id),
    result_message text
);

CREATE TABLE public.ingame_item (
	id serial PRIMARY KEY,
	item_id bigint references fixed.base_item(id),
	items_in_stack bigint,
	user_id bigint not null references public.usr(chat_id),
	is_equipped boolean,
	sharpness bigint
);

-- CREATE TABLE public.equipment (
--     equipment_id serial PRIMARY KEY,
-- 	user_id bigint references public.usr(chat_id),
-- 	helmet bigint references public.inventory_cell(item_id),
-- 	chest_armor bigint references public.inventory_cell(item_id),
-- 	leg_armor bigint references public.inventory_cell(item_id),
-- 	boots bigint references public.inventory_cell(item_id),
-- 	left_hand bigint references public.inventory_cell(item_id),
-- 	right_hand bigint references public.inventory_cell(item_id)
-- );

CREATE TABLE public.party (
    party_id serial primary key,
	user_id bigint references public.usr(chat_id),
	name TEXT UNIQUE,
	current_state TEXT
);

CREATE TABLE public.fight (
    id serial primary key,
    user1_id bigint references public.usr(chat_id),
	user2_id bigint references public.usr(chat_id),
	fight_state TEXT
);


CREATE TABLE public.move (
    id serial primary key,
    user_id bigint references public.usr(chat_id),
    fight_id bigint references public.fight(id),
	defense int,
	attack int,
	move_state int,
	hp bigint,
	num bigint
);



insert into fixed.solo_activity(name, state_name, description, required_level,required_stamina, activity_duration)
values ('Руины',
        'Исследование руин',
        'Заброшеные руины недалеко от города. Чего-то редкого там не найдешь, но наберешься опыта для более сложных приключений и, если повезет, пару монет.',1, 1, 1);

insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward,result_message) values (1, 2, 3, null,'Ты побродил по руинам пару часов, было очень скучно.');

insert into fixed.class(name, description, required_level, base_class)
    VALUES ('Работяга', 'Обычный работяга без каких либо бонусов.', 1, null);
insert into fixed.class(name, description, required_level, base_class)
    VALUES ('Воин', 'Воин, который может хорошо обращаться с холодным оружием', 1, 1);


insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
    VALUES ('Деревянный меч', 'Тренировочный меч, победить кого с ним настоящая удача.', 2, null,0,null, 15,true);
insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
    VALUES ('Старый железный меч', 'Старый железный меч, немного острый.', 2, null,0,null, 15,true,2);
insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
    VALUES ('Деревянный щит', 'Тренировочный щит, победить кого с ним настоящая удача.', null, 2,2,null, 15,true);
insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
    VALUES ('Камень', 'Обычный камень чтобы что то сделать', null, null,8,25, 15,true);
insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
    VALUES ('Палка', 'Обычная деревянная палка для создания предметов.', null, null,8,25, 15,true);
insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
    VALUES ('Посох', 'Обычная деревянная палка для убийства людей.', 1, null,1,null, 25,true);
insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
    VALUES ('Кожаный шлем', '.', null, 2,5,null, 15,true);
insert into fixed.base_item
    (name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
    VALUES ('Точильный камень', 'С его помощью можно заточить оружие или доспехи.', null, null,9,25, 50,true);



insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (1,null,1436473525,false,0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (2,null,1436473525,false,0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (3,null,1436473525,false,0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (4,1,1436473525,false,null);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (5,1,1436473525,false,null);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (6,null,1436473525,false,0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (7,null,1436473525,false,0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
    VALUES (8,25,1436473525,false,null);