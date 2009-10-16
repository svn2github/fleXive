CREATE SEQUENCE FXSEQ_SYS_CONTENT START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_CONTENT',(SELECT MAX(ID)+1 FROM FX_CONTENT));
CREATE SEQUENCE FXSEQ_SYS_ACCOUNT START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_ACCOUNT',(SELECT MAX(ID)+1 FROM FXS_ACCOUNTS));
CREATE SEQUENCE FXSEQ_SYS_GROUP START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_GROUP',(SELECT MAX(ID)+1 FROM FXS_USERGROUPS));
CREATE SEQUENCE FXSEQ_SYS_ACL START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_ACL',(SELECT MAX(ID)+1 FROM FXS_ACL));
CREATE SEQUENCE FXSEQ_SYS_MANDATOR START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_MANDATOR',(SELECT MAX(ID)+1 FROM FXS_MANDATOR));
CREATE SEQUENCE FXSEQ_SYS_TYPEDEF START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_TYPEDEF',(SELECT MAX(ID)+1 FROM FXS_TYPEDEF));
CREATE SEQUENCE FXSEQ_SYS_TYPEGROUP START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_TYPEGROUP',(SELECT MAX(ID)+1 FROM FXS_TYPEGROUPS));
CREATE SEQUENCE FXSEQ_SYS_TYPEPROP START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_TYPEPROP',(SELECT MAX(ID)+1 FROM FXS_TYPEPROPS));
CREATE SEQUENCE FXSEQ_SYS_ASSIGNMENT START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_ASSIGNMENT',(SELECT MAX(ID)+1 FROM FXS_ASSIGNMENTS));
CREATE SEQUENCE FXSEQ_SYS_STEPDEFINITION START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_STEPDEFINITION',(SELECT MAX(ID)+1 FROM FXS_WF_STEPDEFS));
CREATE SEQUENCE FXSEQ_SYS_STEP START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_STEP',(SELECT MAX(ID)+1 FROM FXS_WF_STEPS));
CREATE SEQUENCE FXSEQ_SYS_ROUTE START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_ROUTE',(SELECT MAX(ID)+1 FROM FXS_WF_ROUTES));
CREATE SEQUENCE FXSEQ_SYS_WORKFLOW START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_WORKFLOW',(SELECT MAX(ID)+1 FROM FXS_WORKFLOWS));
CREATE SEQUENCE FXSEQ_SYS_BINARY START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_BINARY',(SELECT MAX(ID)+1 FROM FX_BINARY));
CREATE SEQUENCE FXSEQ_SYS_SCRIPTS START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_SCRIPTS',(SELECT MAX(ID)+1 FROM FXS_SCRIPTS));
CREATE SEQUENCE FXSEQ_SYS_BRIEFCASE START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_BRIEFCASE',(SELECT MAX(ID)+1 FROM FXS_BRIEFCASE));
CREATE SEQUENCE FXSEQ_SYS_SEARCHCACHE_MEMORY START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_SEARCHCACHE_MEMORY',(SELECT MAX(SEARCH_ID)+1 FROM FXS_SEARCHCACHE_MEMORY));
CREATE SEQUENCE FXSEQ_SYS_SEARCHCACHE_PERM START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_SEARCHCACHE_PERM',(SELECT MAX(SEARCH_ID)+1 FROM FXS_SEARCHCACHE_PERM));
CREATE SEQUENCE FXSEQ_SYS_SELECTLIST START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_SELECTLIST',(SELECT MAX(ID)+1 FROM FXS_SELECTLIST));
CREATE SEQUENCE FXSEQ_SYS_SELECTLIST_ITEM START 1 INCREMENT BY 1 CACHE 16;
SELECT SETVAL('FXSEQ_SYS_SELECTLIST_ITEM',(SELECT MAX(ID)+1 FROM FXS_SELECTLIST_ITEM));
CREATE SEQUENCE FXSEQ_SYS_TREE_EDIT START 1 INCREMENT BY 1 CACHE 1;
SELECT SETVAL('FXSEQ_SYS_TREE_EDIT',(SELECT MAX(ID)+1 FROM FXS_TREE));
CREATE SEQUENCE FXSEQ_SYS_TREE_LIVE START 1 INCREMENT BY 1 CACHE 1;
SELECT SETVAL('FXSEQ_SYS_TREE_LIVE',(SELECT MAX(ID)+1 FROM FXS_TREE_LIVE));