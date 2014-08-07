# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "ADMINS" ("ID" UUID NOT NULL PRIMARY KEY,"EMAIL" VARCHAR(254) NOT NULL,"PASSMD5" VARCHAR(254) NOT NULL);
create table "FILES" ("ID" UUID NOT NULL PRIMARY KEY,"USER_ID" BIGINT NOT NULL,"TRANS_ID" UUID NOT NULL,"REVISION" VARCHAR(254) NOT NULL,"FILENAME" VARCHAR(254) NOT NULL,"PATH" VARCHAR(254) NOT NULL,"HUMAN_SIZE" VARCHAR(254) NOT NULL,"SIZE" BIGINT NOT NULL,"MOD_DATE" DATE NOT NULL,"STATUS" VARCHAR(254) NOT NULL);
create table "TRANSFERS" ("ID" UUID NOT NULL PRIMARY KEY,"USER_ID" BIGINT NOT NULL,"TITLE" VARCHAR(254) NOT NULL,"XFER_DATE" DATE NOT NULL,"STATUS" INTEGER NOT NULL,"ACCESSION_ID" VARCHAR(254) NOT NULL,"NOTE" VARCHAR(254) NOT NULL);
create table "USERS" ("ID" SERIAL NOT NULL PRIMARY KEY,"EMAIL" VARCHAR(254) NOT NULL,"NAME" VARCHAR(254) NOT NULL,"ORG" VARCHAR(254) NOT NULL,"PASSMD5" VARCHAR(254) NOT NULL,"TOKEN" VARCHAR(254) NOT NULL);
alter table "FILES" add constraint "XFR_FK" foreign key("TRANS_ID") references "TRANSFERS"("ID") on update NO ACTION on delete NO ACTION;
alter table "FILES" add constraint "USR_FK" foreign key("USER_ID") references "USERS"("ID") on update NO ACTION on delete NO ACTION;
alter table "TRANSFERS" add constraint "USR_FK" foreign key("USER_ID") references "USERS"("ID") on update NO ACTION on delete NO ACTION;

# --- !Downs

alter table "FILES" drop constraint "XFR_FK";
alter table "FILES" drop constraint "USR_FK";
alter table "TRANSFERS" drop constraint "USR_FK";
drop table "ADMINS";
drop table "FILES";
drop table "TRANSFERS";
drop table "USERS";

