# Learning Log

One entry per unit (see playbook §2.11), written by me, in my own words, after the checkpoint — not copied from the agent's explanation. The point of this file is producing the recall myself, not having a record of what was said.

## Phase 0
Unit I
-In this phase we started this project with instructing the agent to read all the docs and understand the project structure.
-The setup checklist was documented to keep the required components and credentials for the project.
Unit II :
-Here we declared a parent POM which consists of packaging dependencyManagement and why not single POM for each modules .
Unit III :
-Created child POM which actual tells what it needs as parent POM locked in on versions , it also fixed the scope of postgres as runtime to avoid raw bypass and test to not include them in JAR.
Unit IV :
-Created the main directory for the application and the main class which is the entry point for the application . NexusApplication.java and application.yml .
Unit V :
-Created the docker-compose.yml file to run postgres+pgvector , redis and kafka as 3 different containers with named volumes for persistant data and health checks are defined to actually ensure the containers are up and running. Also create .env example file .
Unit VI :
-Created local profile of dev with a separate application-dev.yml file. There are some shared config in the parent application.yml and dev specific config such as flyway clean and sql format required for dev only . 
Unit VII :
-Created ArchUnit Test to ensure domain purity i.e., no spring or jpa imports in domain classes. This was done to keep the code clean and fast to test. This is done only for prod code not for test as for test SpringBootTest is required .
Unit VIII :
-
