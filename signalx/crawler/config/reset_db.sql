drop table if exists record;
drop table if exists event;
drop table if exists source_keyword_offset;

create table event
(
    id          bigint auto_increment
        primary key,
    keyword_id  bigint                             not null comment '关键词id',
    name        varchar(255) unique                not null comment '事件名',
    heat        double   default 0                 null comment '热度',
    sentiment   float                              null comment '情感值',
    create_time datetime default CURRENT_TIMESTAMP comment '创建时间',
    update_time datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create index idx_keyword_id_heat
    on event (keyword_id, heat);

create index idx_keyword_id_update_time
    on event (keyword_id, update_time);

create table record
(
    id               bigint auto_increment
        primary key,
    keyword_id       bigint                             not null comment '关键词id',
    event_id         bigint                             not null comment '事件id',
    source           varchar(32)                        not null comment '渠道',
    author           varchar(128)                       not null comment '作者',
    title            varchar(128)                       not null comment '标题',
    content          varchar(1024)                      null comment '内容',
    heat             double   default 0                 null comment '热度',
    sentiment        float                              null comment '情感值',
    publish_time     datetime not null comment '发布时间',
    url              varchar(512)                       not null comment '链接',
    ext              text                               null comment '拓展字段',
    create_time      datetime default CURRENT_TIMESTAMP null comment '创建时间',
    update_time      datetime default CURRENT_TIMESTAMP null on update CURRENT_TIMESTAMP comment '更新时间'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

create index idx_event_id_publish_time
    on record (event_id, publish_time);

create index idx_event_id_heat
    on record (event_id, heat);

create table source_keyword_offset
(
    id          bigint auto_increment
        primary key,
    keyword_id       bigint                             not null comment '关键词id',
    source           varchar(32)                        not null comment '渠道',
    create_time datetime default '1000-01-01 00:00:00' null comment '创建时间',
    update_time datetime default '1000-01-01 00:00:00' null comment '更新时间'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
comment '渠道-关键词 去重偏移量表';

create index idx_keyword_id_source
    on source_keyword_offset (keyword_id, source);
