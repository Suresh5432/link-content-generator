CREATE TABLE links(
                      id UUID primary key,
                      url varchar(2048) not null ,
                      user_id varchar(255) not null ,
                      status varchar(40) not null ,
                      created_at timestamptz not null,
                      updated_at timestamptz not null
);

Create index idx_links_user_id on links(user_id);

CREATE  index idx_links_status on links(status);

Create index idx_links_created_at on links(created_at);