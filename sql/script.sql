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

CREATE TABLE fixed.active_skill
(
    id           serial PRIMARY KEY,
    name         text unique ,
    class_id     bigint references fixed.class (id),
    damage_bonus double precision,
    mana_cost    bigint
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
    count_of_material       bigint
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
    fight_state TEXT,
    bet bigint
);


CREATE TABLE public.move
(
    id         serial primary key,
    user_id    bigint references public.usr (chat_id),
    fight_id   bigint references public.fight (id),
    defense    int,
    attack     int,
    active_skill_id bigint,
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
VALUES ('⚔ Воин', 'Этот закаленный в сражениях боец всегда храбро сражается до конца.', 5, 1);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('💫 Маг', 'Лишь его эффектное появление вгоняет врагов в ужас.', 5, 1);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('🏹 Рейнджер', 'Ловкий странник. Его стиль - бесшумно убить врага и остаться незамеченым.', 5, 1);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('⚔ Берсерк', 'Ни судьи, ни присяжных. Только палач.', 15, 2);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('⚔ Гладиатор', 'Подними руки вверх под крики толпы и поклянись в верности и славе', 15, 2);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('🏹 Снайпер', 'Попробуй противостоять тому, кого даже не видишь.', 15, 4);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('🏹 Ассасин', 'Ничто не истинно. Всё дозволено.', 15, 4);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('💫 Чернокнижник', 'Не каждый сможет подчинить себе тёмные силы, но достойный сможет управлять миром.', 15, 3);
insert into fixed.class(name, description, required_level, base_class)
VALUES ('💫 Жрец', 'Узри же силу богов и поклонись ей или умри.', 15, 3);

insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Базовое владение оружием', 1, 1, 0, 0, 0),
       ('Базовое владение броней', 1, 0, 1, 0, 0),
       ('Повышение здоровья', 1, 0, 0, 5, 0),
       ('Повышение маны', 1, 0, 0, 0, 5);
insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Умелое владение оружием', 2, 2, 0, 0, 0),
       ('Умелое владение броней', 2, 0, 2, 0, 0),
       ('Живучесть воина', 2, 0, 1, 10, 0);
insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Ловкое владение оружием', 4, 1, 1, 0, 0),
       ('Везение', 4, 0, 1, 5, 5),
       ('Скрытая смертельная атака', 4, 1, 0, 0, 5);
insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Магическая защита', 3, 0, 2, 5, 0),
       ('Магический пулемет', 3, 0, 0, 0, 10),
       ('Сила духов', 3, 2, 0, 0, 0);

insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Мастер меча', 5, 4, 0, 0, 5),
       ('Бронированный', 5, 0, 4, 5, 0),
       ('Выживший', 5, 0, 2, 15, 5);
insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Убийца', 6, 3, 1, 0, 5),
       ('Шахматист', 6, 0, 3, 10, 5),
       ('Чемпион', 6, 1, 2, 10, 0);

insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Соколиный глаз', 7, 2, 0, 0, 10),
       ('Невидимка', 7, 0, 2, 10, 0),
       ('Ловкач', 7, 1, 1, 5, 5);
insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Ас фехтования', 8, 2, 1, 0, 0),
       ('Мастер маскировки', 8, 0, 2, 5, 10),
       ('Трикстер', 8, 1, 0, 10, 5);

insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Гнев демона', 9, 3, 0, 0, 10),
       ('Связь с потусторонним миром', 9, 0, 2, 10, 10),
       ('Книга мертвых', 9, 2, 1, 0, 0);
insert into fixed.skill(name, class_id, damage_bonus, armor_bonus, health_bonus, mana_bonus)
VALUES ('Проклятье богов', 10, 3, 1, 0, 0),
       ('Рука Бога', 10, 1, 3, 5, 10),
       ('Сила природы', 10, 2, 0, 5, 10);



insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Байкал 0.25', 'Восстанавливает 20 здоровья.', null, null, 7, 5, 50, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Байкал 0.5', 'Восстанавливает 50 здоровья.', null, null, 7, 3, 100, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Байкал 1.0', 'Восстанавливает 100 здоровья.', null, null, 7, 2, 200, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Энергетик 0.25', 'Восстанавливает 1 ед. выносливости.', null, null, 7, 5, 50, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Энергетик 0.5', 'Восстанавливает 2 ед. выносливости.', null, null, 7, 3, 100, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Энергетик 1.0', 'Восстанавливает 4 ед. выносливости.', null, null, 7, 2, 200, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Квас 0.25', 'Восстанавливает 20 ед. маны.', null, null, 7, 5, 50, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Квас 0.5', 'Восстанавливает 50 ед. маны.', null, null, 7, 3, 100, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Квас 1.0', 'Восстанавливает 100 ед. маны.', null, null, 7, 2, 200, true);

insert into fixed.consumable_item_effect
    (base_item_id, add_life, add_mana, add_stamina)
VALUES (1, 20, 0, 0),
       (2, 50, 0, 0),
       (3, 100, 0, 0),
       (4, 0, 0, 1),
       (5, 0, 0, 2),
       (6, 0, 0, 4),
       (7, 0, 20, 0),
       (8, 0, 50, 0),
       (9, 0, 100, 0);



insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Точильный камень', 'С его помощью можно заточить оружие или доспехи.', null, null, 9, 25, 50, true);

insert into fixed.solo_activity(name, state_name, description, required_level, required_stamina, activity_duration)
values ('Руины Зоны',
        'Исследование руин Зоны',
        'Заброшеные руины недалеко от города. Судя по обрывкам надписей, найденых среди этих руин, древние называли это место словом "Зона". Не известно, что это за зона, но людей это слово пугает, хотя там можно найти что-нибудь полезное.',
        1, 1, 1),
       ('Пещеры Тестировщиков',
        'Исследование пещер Тестировщиков',
        'Происхождение этих пещер неизвестно. Однако, там часто находят клочки бумаги с надписью "тестировщик". Может быть древние так называли воинов... К сожалению древнего оружия там не осталось, но можно отыскать что-нибудь полезное.',
        5, 2, 2),
       ('Долины Программистов',
        'Исследование долин Программистов',
        'Название этим долинам мы дали из-за обломка таблички с такой надписью. Судя по месторасположению программисты это фермеры или ремесленники. Здесь можно найти довольно много различных зелий.',
        10, 4, 3),
        ('Храм "Конгресс-центр"',
        'Исследование храма "Конгресс-центр"',
        'Храм, в котором по всей видимости древние молились богу Конгрессу. Здесь хранятся зелья в большом объеме.',
        15, 6, 4);

insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (1, 2, 3, null, 'Ты побродил по руинам пару часов. Было очень скучно.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (1, 3, 2, null, 'Ты побродил по руинам пару часов. Нашел какие-то монеты.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (1, 1, 2, 1, 'Ты побродил по руинам пару часов. Тебе повезло найти *Байкал 0.25*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (1, 2, 2, 10, 'Ты побродил по руинам пару часов. Тебе повезло найти *Точильный камень*.');

insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (2, 8, 5, 4, 'Ты обыскал пещеры. Тебе повезло найти *Энергетик 0.25*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (2, 7, 6, 1, 'Ты обыскал пещеры. Тебе повезло найти *Байкал 0.25*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (2, 6, 7, 7, 'Ты обыскал пещеры. Тебе повезло найти *Квас 0.25*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (2, 6, 7, null, 'Ты обыскал пещеры. Ты вышел весь в грязи, найдя лишь пару монет.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (2, 6, 6, 10, 'Ты обыскал пещеры. Тебе повезло найти *Точильный камень*.');

insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (3, 20, 15, 2, 'Ты обошел все долины. Тебе повезло найти *Байкал 0.5*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (3, 22, 14, 5, 'Ты обошел все долины. Тебе повезло найти *Энергетик 0.5*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (3, 21, 16, 8, 'Ты обошел все долины. Тебе повезло найти *Квас 0.5*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (3, 21, 16, 4, 'Ты обошел все долины. Ты нашел лишь *Энергетик 0.25*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (3, 20, 16, 10, 'Ты обошел все долины. Тебе повезло найти *Точильный камень*.');

insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (4, 30, 22, 3, 'Ты перерыл весь храм. Тебе повезло найти *Байкал 1.0*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (4, 31, 21, 6, 'Ты перерыл весь храм. Тебе повезло найти *Энергетик 1.0*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (4, 30, 22, 9, 'Ты перерыл весь храм. Тебе повезло найти *Квас 1.0*.');
insert into fixed.solo_activity_reward(solo_activity_id, gold_reward, exp_reward, item_reward, result_message)
values (4, 31, 20, 4, 'Ты перерыл весь храм. Ты нашел лишь *Энергетик 0.25*.');

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Драколит', 'Руда.', null, null, 8, 15, 50, false);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Душа', 'Душа Древнего Драконида.', null, null, 8, 15, 50, false);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кровь драконида', 'Кровь Древнего Драконида.', null, null, 8, 15, 50, false);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Руда демонов', 'Невроятно сложная в обработке руда', null, null, 8, 10, 100, false);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кожа демона', 'Почти не пробиваемая кожа принца демонов.', null, null, 8, 10, 100, false);


insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Рог единорога', 'Магический рог единорога', null, null, 8, 3, 200, false);

insert into fixed.raid_boss (name, life, damage, armor, recommended_level, gold_reward, exp_reward, stamina_required)
VALUES ('Древний Драконид', 1000, 50, 40, 10, 100, 75, 5);
insert into fixed.raid_boss (name, life, damage, armor, recommended_level, gold_reward, exp_reward, stamina_required)
VALUES ('Принц демонов', 3000, 100, 100, 15, 250, 125, 10);
insert into fixed.raid_boss (name, life, damage, armor, recommended_level, gold_reward, exp_reward, stamina_required)
VALUES ('Летучий единорог', 5000, 150, 150, 20, 500, 200, 20);

insert into fixed.raid_boss_item_reward (boss_id, item_id)
values (1, 11),(1, 12),(1, 13),
       (2, 14),(2, 15),
       (3, 16);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Деревянный меч (одноручный)', 'Тренировочный меч, победить кого с ним настоящая удача.', 2, null, 0, null, 3, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Деревянный щит', 'Тренировочный щит, победить кого с ним настоящая удача.', null, 2, 2, null, 3, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Старый железный меч (одноручный)', 'Старый железный меч. Этот меч отслужил свое и уже не так хорош.', 8, null, 0, null, 20, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Старый железный двуручный меч', 'Старый железный двуручный меч. Этот меч отслужил свое и уже не так хорош.', 12, null, 1, null, 25, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Старый железный щит', 'Старый железный щит. Этот щит отслужил свое и уже не так хорош.', null, 8, 2, null, 20, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Железный меч (одноручный)', 'Железный меч. Отличное оружие за соответствующую цену.', 12, null, 0, null, 25, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Железный двуручный меч', 'Железный двуручный меч. Отличное оружие за соответствующую цену.', 18, null, 1, null, 30, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Железный щит', 'Железный щит. Отличный щит за соответствующую цену.', null, 12, 2, null, 25, true);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Элитный двуручный меч', 'Элитный меч. Таким оружием пользуется большинство доблестных войнов.', 20, null, 1, null, 100, true, 2);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Копье (двуручное)', 'Копье требует особого навыка, зато наносит огромный урон противнику.', 30, null, 1, null, 180, true, 2);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Щит воина', 'Этот щит предназначен для того, чтобы выдерживать удары любых противников.', null, 20, 2, null, 100, true, 2);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Посох (двуручное)', 'Посох - древнее оружие магов.', 30, null, 1, null, 180, true, 3);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Изумрудный меч (одноручное)', 'Изумрудный меч является легким, но смертоносным оружием, а так же помогает концентрировать магию.', 15, null, 0, null, 100, true, 3);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Щит с рунами', 'Это обычный железный щит, усиленный руническими заклятьями.', null, 20, 2, null, 100, true, 3);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Стилет (одноручное)', 'Стилет - идеальное оружие для тихого убийства', 13, null, 0, null, 70, true, 4);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Лёгкий меч (одноручное)', 'Лёгкий меч является легким, но смертоносным оружием.', 15, null, 0, null, 100, true, 4);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Охотничий лук (двуручное)', 'Лук - дальнобойное легкое оружие', 25, null, 1, null, 150, true, 4);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Меч короля артура (двуручный)', 'Королевский меч может уничтожать целые города.', 40, null, 1, null, 200, true, 5);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Алебарда (двуручное)', 'Это оружие позволит вам крошить врагов даже не подпуская к себе.', 50, null, 1, null, 240, true, 5);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Рыцарский щит', 'Этот щит предназначен для того, чтобы выдерживать удары магических чудищ.', null, 50, 2, null, 300, true, 5);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Турнирный меч (двуручный)', 'Лучший меч. Такой брали на турниры только с самыми сильными врагами.', 40, null, 1, null, 200, true, 6);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Трезубец (двуручный)', 'В правильных руках это оружие внушает ужас.', 50, null, 1, null, 240, true, 6);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Турнирный щит', 'Лучший щит. Такой брали на турниры только с самыми сильными врагами.', null, 50, 2, null, 300, true, 6);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Нож Иуды (одноручный)', 'Враги до последнего не поймут, кто их атакует.', 30, null, 0, null, 200, true, 7);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Золотая рапира (одноручная)', 'Даже убийство может быть элегантным', 35, null, 0, null, 240, true, 7);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Снайперский лук (двуручное)', 'Лук с прицелом обладает высочайшей точностью.', null, 50, 1, null, 300, true, 7);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Скрытый клинок (одноручный)', 'Позволяет скрыться с мета убийства незамеченным.', 30, null, 0, null, 200, true, 8);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Шэнбяо (одноручное)', 'Этот нож на вервке становится прекрасным оружием в правильных руках.', 32, null, 0, null, 240, true, 8);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Кастет с шипами (одноручное)', 'Просто крошит черепа.', null, 35, 0, null, 300, true, 8);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Проклятый клинок (одноручное)', 'Питается душами убитых', 30, null, 0, null, 200, true, 9);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Посох дьяввола (двуручное)', 'Материал этого посоха выдерживает адские температуры.', 50, null, 1, null, 240, true, 9);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Перчатка стихий. (одноручное)', 'Эта перчатка повеливает стихиями.', null, 35, 0, null, 300, true, 9);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Клинок гнева (одноручное)', 'Гнев богов во плоти.', 30, null, 0, null, 200, true, 10);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Посох богов (двуручное)', 'Этот посох концентрирует силу богов.', 50, null, 1, null, 240, true, 10);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Атакующий щит. (одноручное)', 'Этот щит плох в защите, но хорош в использовании атакующей магии.', 20, 20, 2, null, 300, true, 10);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Демонический меч', 'Этот одноручный меч внушает страх одним своим видом', 80, null, 0, null, 300, false, 5);

insert into fixed.base_item_craft(crafted_base_item_id, material_base_item_id, count_of_material)
VALUES (52, 11, 3), (52, 14, 2), (52, 16, 1);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Меч короля Ада', 'Этот двуручный меч может лишить жизни одним движением.', 100, null, 1, null, 300, false, 6);

insert into fixed.base_item_craft(crafted_base_item_id, material_base_item_id, count_of_material)
VALUES (53, 12, 3), (53, 15, 2), (53, 16, 1);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Драконидовый лук', 'Этот лук сравним с древнем оружием, которое могло стрелять за километр', 100, null, 1, null, 300, false, 7);

insert into fixed.base_item_craft(crafted_base_item_id, material_base_item_id, count_of_material)
VALUES (54, 13, 3), (54, 15, 2), (54, 16, 1);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Рог единорога', 'Этот клинок опасен так же как и сам единорог.', 70, null, 0, null, 300, false, 8);

insert into fixed.base_item_craft(crafted_base_item_id, material_base_item_id, count_of_material)
VALUES (55, 11, 1), (55, 16, 1);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Бузинная палочка', 'Эта волшебная палочка - самое мощное магическое оружие в природе.', 80, null, 0, null, 300, false, 9);

insert into fixed.base_item_craft(crafted_base_item_id, material_base_item_id, count_of_material)
VALUES (56, 12, 3), (56, 14, 2), (56, 16, 1);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Молния Конгресса', 'Оружие бога, и этим все сказано.', 100, null, 1, null, 300, false, 10);

insert into fixed.base_item_craft(crafted_base_item_id, material_base_item_id, count_of_material)
VALUES (67, 13, 3), (67, 14, 2), (67, 16, 1);



insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кожаный шлем', 'Защищает не очень, но лучше, чем ничего', null, 2, 5, null, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кожаный нагрудник', 'Защищает не очень, но лучше, чем ничего', null, 2, 3, null, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кожаные поножи', 'Защищает не очень, но лучше, чем ничего', null, 2, 4, null, 15, true);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale)
VALUES ('Кожаные ботинки', 'Защищает не очень, но лучше, чем ничего', null, 2, 6, null, 15, true);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Железный шлем', 'Хорошая защита.', null, 5, 5, null, 30, true, 2);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Железный нагрудник', 'Хорошая защита.', null, 5, 3, null, 30, true, 2);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Железные поножи', 'Хорошая защита.', null, 5, 4, null, 30, true, 2);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Железные ботинки', 'Хорошая защита.', null, 5, 6, null, 30, true, 2);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Легкий шлем', 'Легкая броня.', null, 4, 5, null, 30, true, 4);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Легкий нагрудник', 'Легкая броня.', null, 4, 3, null, 30, true, 4);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Легкие поножи', 'Легкая броня.', null, 4, 4, null, 30, true, 4);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Легкие ботинки', 'Легкая броня.', null, 4, 6, null, 30, true, 4);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Рунический шлем', 'Броня усилена рунами.', null, 3, 5, null, 30, true, 3);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Рунический нагрудник', 'Броня усилена рунами.', null, 3, 3, null, 30, true, 3);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Рунические поножи', 'Броня усилена рунами.', null, 3, 4, null, 30, true, 3);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Рунические ботинки', 'Броня усилена рунами.', null, 3, 6, null, 30, true, 3);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Боевой шлем', 'Броня, закаленная в легендарных боях.', null, 20, 5, null, 60, true, 5);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Боевой нагрудник', 'Броня, закаленная в легендарных боях.', null, 20, 3, null, 60, true, 5);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Боевые поножи', 'Броня, закаленная в легендарных боях.', null, 20, 4, null, 60, true, 5);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Боевые ботинки', 'Броня, закаленная в легендарных боях.', null, 20, 6, null, 60, true, 5);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Гладиаторский шлем', 'Броня, в которой гладиаторы сокрушают врагов.', null, 19, 5, null, 60, true, 6);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Гладиаторский нагрудник', 'Броня, в которой гладиаторы сокрушают врагов.', null, 19, 3, null, 60, true, 6);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Гладиаторские поножи', 'Броня, в которой гладиаторы сокрушают врагов.', null, 19, 4, null, 60, true, 6);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Гладиаторские ботинки', 'Броня, в которой гладиаторы сокрушают врагов.', null, 19, 6, null, 60, true, 6);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Маскировочный шлем', 'Эта броня сбивает врага с толку своим видом.', null, 15, 5, null, 45, true, 7);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Маскировочный нагрудник', 'Эта броня сбивает врага с толку своим видом.', null, 15, 3, null, 45, true, 7);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Маскировочные поножи', 'Эта броня сбивает врага с толку своим видом.', null, 15, 4, null, 45, true, 7);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Маскировочные ботинки', 'Эта броня сбивает врага с толку своим видом.', null, 15, 6, null, 45, true, 7);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Шлем ассасина', 'Подвижная, но надежная древняя броня.', null, 16, 5, null, 45, true, 8);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Нагрудник ассасина', 'Подвижная, но надежная древняя броня.', null, 16, 3, null, 45, true, 8);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Поножи ассасина', 'Подвижная, но надежная древняя броня.', null, 16, 4, null, 45, true, 8);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Ботинки ассасина', 'Подвижная, но надежная древняя броня.', null, 16, 6, null, 45, true, 8);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Зачарованный шлем', 'Чары этой брони дают дополнительную силу атаки.', 7, 10, 5, null, 45, true, 9);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Зачарованный нагрудник', 'Чары этой брони дают дополнительную силу атаки.', 7, 10, 3, null, 45, true, 9);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Зачарованные поножи', 'Чары этой брони дают дополнительную силу атаки.', 7, 10, 4, null, 45, true, 9);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Зачарованные ботинки', 'Чары этой брони дают дополнительную силу атаки.', 7, 10, 6, null, 45, true, 9);

insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Небесный шлем', 'В этой броне вера в богов дает их силу.', 8, 9, 5, null, 45, true, 10);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Небесный нагрудник', 'В этой броне вера в богов дает их силу.', 9, 10, 3, null, 45, true, 10);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Небесные поножи', 'В этой броне вера в богов дает их силу.', 8, 9, 4, null, 45, true, 10);
insert into fixed.base_item
(name, description, damage, armor, type, max_in_stack, buy_price, is_for_sale, class_required_id)
VALUES ('Небесные ботинки', 'В этой броне вера в богов дает их силу.', 8, 9, 6, null, 45, true, 10);

insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Сильный удар', 1, 1.2, 10);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Разъяренный волк', 2, 1.6, 30);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Сильнее сильного', 2, 2, 40);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Огненный шар', 3, 2.2, 20);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Рунический удар', 3, 2.5, 30);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Крысиный бой', 4, 2, 25);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Жало пчелы', 4, 2.3, 45);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Карусель ярости', 5, 2, 30);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Тотальное превосходство', 5, 2.5, 45);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Силовая комбинация', 6, 2.2, 35);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Мощь гладиатора', 6, 2.5, 55);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Рояль в кустах', 7, 2, 35);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Тройной выстрел', 7, 2.5, 55);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Тихое убийство', 8, 2.5, 40);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Тайный замысел', 8, 3, 55);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Ведьмино проклятье', 9, 3, 50);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Испепеляющий луч', 9, 3.5, 35);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Божественное начало', 10, 3, 50);
insert into fixed.active_skill(name, class_id, damage_bonus, mana_cost)
VALUES ('Удар Бога', 10, 4, 75);