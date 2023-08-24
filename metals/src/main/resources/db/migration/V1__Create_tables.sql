-- The relationship between library dependency sources under .metals/readonly/**
-- and build targets they belong to. Required to know what classpath to use
-- for compiling dependency sources.
create table dependency_source(
  text_document_uri varchar primary key,
  build_target_uri varchar not null
);

-- The relationship between library dependency sources under .metals/readonly/**
-- and worksheets they belong to. Required to know what classpath to use
-- for compiling dependency sources.
create table worksheet_dependency_source(
  text_document_uri varchar primary key,
  worksheet_uri varchar not null
);

-- The relationship between what library dependency sources under .metals/readonly/**
-- map to which build targets.
create table sbt_digest(
  md5 varchar,
  status tinyint not null,
  when_recorded timestamp
);

-- Which window/showMessage and window/showMessageRequest dialogues have been dismissed
-- by the user via "Don't show again" or closed by clicking on "x".
create table dismissed_notification(
  id int,
  when_dismissed timestamp,
  when_expires timestamp
);

-- The choice of build tool when multiple build tool files are found in a workspace
create table chosen_build_tool(
  build_tool varchar primary key
);

-- Choice of project root
create table project_root(
  relative_path varchar primary key
);
