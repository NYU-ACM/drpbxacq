# --- Created by Slick DDL
# To stop Slick DDL generation, remove this comment and start using Evolutions

# --- !Ups

create table "USER" ("ID" SERIAL NOT NULL PRIMARY KEY,"EMAIL" VARCHAR(254) NOT NULL,"PASSMD5" VARCHAR(254) NOT NULL,"TOKEN" VARCHAR(254) NOT NULL);

# --- !Downs

drop table "USER";

