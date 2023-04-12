drop table if exists  public.usr;

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
                            class_id bigint,
                            gold bigint,
                            offline_points bigint
);