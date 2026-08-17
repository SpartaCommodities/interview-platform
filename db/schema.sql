-- schema for the ports service.
--
-- the application does not apply this itself; it expects the table to already
-- exist.

create table if not exists ports (
    code    varchar(10) primary key,
    name    varchar(200) not null,
    country varchar(100) not null
);

insert into ports (code, name, country) values
    ('NLRTM', 'Rotterdam', 'Netherlands'),
    ('SGSIN', 'Singapore', 'Singapore'),
    ('USHOU', 'Houston', 'United States'),
    ('AEFJR', 'Fujairah', 'United Arab Emirates')
on conflict (code) do nothing;
