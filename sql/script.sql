drop database if exists RPG_telegrambot;
CREATE DATABASE RPG_telegrambot;
drop schema if exists public cascade;
drop schema if exists fixed cascade;
CREATE SCHEMA public;
CREATE SCHEMA fixed;

CREATE TABLE fixed.class
(
    id             serial PRIMARY KEY,
    name           text,
    description    text,
    required_level bigint,
    base_class     bigint references fixed.class (id)
);

CREATE TABLE fixed.skill
(
    id           serial PRIMARY KEY,
    name         text,
    class_id     bigint references fixed.class (id),
    damage_bonus bigint,
    armor_bonus  bigint,
    health_bonus bigint,
    mana_bonus   bigint
);

CREATE TABLE fixed.solo_activity
(
    id                serial PRIMARY KEY,
    name              text,
    state_name        text,
    description       text,
    required_level    bigint,
    required_stamina  bigint,
    activity_duration bigint
);
CREATE TABLE fixed.raid_boss
(
    id                serial PRIMARY KEY,
    name              text,
    life              bigint,
    damage            bigint,
    armor             bigint,
    recommended_level bigint,
    stamina_required  bigint,
    gold_reward       bigint,
    exp_reward        bigint
);
CREATE TABLE public.party
(
    id            serial primary key,
    name          TEXT UNIQUE,
    boss_fighting bigint references fixed.raid_boss (id),
    boss_life     bigint
);

CREATE TABLE public.usr
(
    chat_id         bigint PRIMARY KEY,
    name            TEXT,
    level           bigint,
    passive_points  bigint,
    current_health  bigint,
    max_health      bigint,
    current_stamina bigint,
    max_stamina     bigint,
    current_mana    bigint,
    max_mana        bigint,
    user_state      integer,
    activity_ends   TIMESTAMP,
    activity_id     bigint references fixed.solo_activity (id),
    stamina_restor  TIMESTAMP,
    partner_chat_id bigint references public.usr (chat_id),
    class_id        bigint references fixed.class (id),
    party_id        bigint references public.party (id),
    host_party_id   bigint references public.party (id),
    gold            bigint,
    exp             bigint,
    is_game_master  boolean,
    offline_points  bigint
);

CREATE TABLE public.applied_skill
(
    id          serial PRIMARY KEY,
    skill_id    bigint references fixed.skill (id),
    user_id     bigint references public.usr (chat_id),
    skill_level bigint
);

CREATE TABLE fixed.base_item
(
    id                serial PRIMARY KEY,
    name              text,
    description       text,
    damage            bigint,
    armor             bigint,
    type              bigint,
    max_in_stack      bigint,
    is_for_sale       boolean,
    class_required_id bigint references fixed.class (id) default (1),
    buy_price         bigint
);
CREATE TABLE fixed.base_item_craft
(
    id                    serial PRIMARY KEY,
    crafted_base_item_id  bigint references fixed.base_item (id),
    material_base_item_id bigint references fixed.base_item (id),
    countOfMaterial       bigint
);
CREATE TABLE fixed.consumable_item_effect
(
    id           serial PRIMARY KEY,
    base_item_id bigint references fixed.base_item (id),
    add_life     bigint,
    add_mana     bigint,
    add_stamina  bigint
);

CREATE TABLE fixed.solo_activity_reward
(
    id               serial PRIMARY KEY,
    solo_activity_id bigint references fixed.solo_activity (id),
    gold_reward      bigint,
    exp_reward       bigint,
    item_reward      bigint references fixed.base_item (id),
    result_message   text
);



CREATE TABLE fixed.raid_boss_item_reward
(
    id      serial PRIMARY KEY,
    boss_id bigint references fixed.raid_boss (id),
    item_id bigint references fixed.base_item (id)
);

CREATE TABLE public.ingame_item
(
    id             serial PRIMARY KEY,
    item_id        bigint references fixed.base_item (id),
    items_in_stack bigint,
    user_id        bigint not null references public.usr (chat_id),
    is_equipped    boolean,
    sharpness      bigint
);



CREATE TABLE public.fight
(
    id          serial primary key,
    user1_id    bigint references public.usr (chat_id),
    user2_id    bigint references public.usr (chat_id),
    fight_state TEXT
);


CREATE TABLE public.move
(
    id         serial primary key,
    user_id    bigint references public.usr (chat_id),
    fight_id   bigint references public.fight (id),
    defense    int,
    attack     int,
    move_state int,
    hp         bigint,
    end_time   timestamp,
    num        bigint
);


CREATE TABLE public.group_chat
(
    id           bigint primary key,
    user_invited bigint references public.usr (chat_id)
);

CREATE TABLE public.offline_event
(
    id                    serial primary key,
    creator               bigint references public.usr (chat_id),
    event_name            text,
    event_goal            text,
    offline_points_reward bigint,
    event_type            bigint,
    event_state           integer
);



insert into fixed.class(name, description, required_level, base_class)
VALUES ('⚒ Работяга', 'Обычный работяга без каких либо бонусов.', 1, null);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('⚔ Воин', 'Боец. Умело обращается с холодным оружием всех видов.', 7, 1);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('⚔ Берсерк', 'Жадный до крови воин. Использует медленные, но беспощадные и сокрушительные атаки.', 15, 2);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('🏹 Рейнджер', 'Ловкий странник. Владеет навыками стрельбы из лука и всегда носит с собой пару кинжалов.', 7, 1);


insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Базовое владение оружием.', 1, 1, 0, 0, 0),
       ('Базовое владение броней.', 1, 0, 1, 0, 0),
       ('Повышение здоровья.', 1, 0, 0, 5, 0),
       ('Повышение маны.', 1, 0, 0, 0, 5),
       ('Живучесть воина.', 2, 0, 1, 5, 0),
       ('Улучшенное владение броней.', 2, 0, 2, 0, 0);



insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Деревянный меч', 'Тренировочный меч, победить кого с ним настоящая удача.', 2, null, 0, null, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Старый железный меч', 'Старый железный меч, немного острый.', 2, null, 0, null, 15, true, 2);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Деревянный щит', 'Тренировочный щит, победить кого с ним настоящая удача.', null, 2, 2, null, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Камень', 'Обычный камень чтобы что то сделать', null, null, 8, 25, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Палка', 'Обычная деревянная палка для создания предметов.', null, null, 8, 25, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Посох', 'Обычная деревянная палка для убийства людей.', 1, null, 1, null, 25, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кожаный шлем', '.', null, 2, 5, null, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Точильный камень', 'С его помощью можно заточить оружие или доспехи.', null, null, 9, 25, 50, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Малое зелье здоровья', 'Восстанавливает 50 здоровья.', null, null, 7, 5, 50, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Энергетик 0.25', 'Восстанавливает 1 ед. выносливости.', null, null, 7, 5, 50, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Драколит', 'Руда.', null, null, 8, 15, 50, false);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Древний камень', 'Камень.', null, null, 8, 15, 50, false);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Душа', 'Душа Древнего Драконида.', null, null, 8, 15, 50, false);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кровь драконида', 'Кровь Древнего Драконида.', null, null, 8, 15, 50, false);

insert into fixed.consumable_item_effect
    (base_item_id, add_life, add_mana, add_stamina)
VALUES (9, 50, 0, 0),
       (10, 0, 0, 1);

insert into fixed.solo_activity(name, state_name, description, required_level, required_stamina, activity_duration)
values ('Руины',
        'Исследование руин',
        'Заброшеные руины недалеко от города. Чего-то редкого там не найдешь, но наберешься опыта для более сложных приключений и, если повезет, пару монет.',
        1, 1, 1),
       ('Музей последнего тестировщика',
        'Исследование музея последнего тестировщика',
        'Музей последнего тестеровщика ОЭЗ. Говорят там осталось много денег.',
        5, 1, 2);

insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (1, 2, 3, null, 'Ты побродил по руинам пару часов, было очень скучно.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (2, 8, 10, 10, 'Ты обшарил старый заброшенный музей, тебе повезло найти *Энергетик 0.25*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (2, 8, 10, null,
        'Ты обшарил старый заброшенный музей, там давно никто не убирался, но ничего кроме пары монет не нашел.');

insert into fixed.raid_boss (name, life, damage, armor, recommended_level, gold_reward, exp_reward, stamina_required)
VALUES ('Древний Драконид', 1000, 50, 40, 10, 100, 150, 5);
insert into fixed.raid_boss_item_reward (boss_id, item_id)
values (1, 11),(1, 13),(1, 13),(1, 14);



insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (1, null, 1436473525, false, 10000);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (2, null, 1436473525, false, 0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (3, null, 1436473525, false, 0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (4, 1, 1436473525, false, null);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (5, 1, 1436473525, false, null);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (6, null, 1436473525, false, 0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (7, null, 1436473525, false, 1000);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (8, 250, 1436473525, false, null);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (10, 15, 1436473525, false, null);


insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (1, null, 651071979, false, 0);
insert into public.ingame_item(item_id, items_in_stack, user_id, is_equipped, sharpness)
VALUES (8, 250, 935293113, false, null);