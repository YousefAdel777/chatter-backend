alter table users
    add status VARCHAR(20) NOT NULL DEFAULT 'UNVERIFIED';

update users set status = 'VERIFIED';