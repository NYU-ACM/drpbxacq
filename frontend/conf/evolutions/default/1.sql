# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "FILES" ("ID" UUID NOT NULL PRIMARY KEY,"USER_ID" BIGINT NOT NULL,"TRANS_ID" UUID NOT NULL,"FILENAME" VARCHAR(254) NOT NULL,"PATH" VARCHAR(254) NOT NULL,"HUMAN_SIZE" VARCHAR(254) NOT NULL,"SIZE" BIGINT NOT NULL,"MOD_DATE" DATE NOT NULL,"STATUS" VARCHAR(254) NOT NULL);
create table "USERS" ("ID" SERIAL NOT NULL PRIMARY KEY,"EMAIL" VARCHAR(254) NOT NULL,"PASSMD5" VARCHAR(254) NOT NULL,"TOKEN" VARCHAR(254) NOT NULL);
alter table "FILES" add constraint "ACC_FK" foreign key("USER_ID") references "USERS"("ID") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "FILES" drop constraint "ACC_FK";
drop table "FILES";
drop table "USERS";

