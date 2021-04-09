# bahmni-mart

[![Build Status](https://travis-ci.org/bahmni-msf/bahmni-mart.svg?branch=master)](https://travis-ci.org/bahmni-msf/bahmni-mart) &nbsp;&nbsp;[![Codacy Badge](https://api.codacy.com/project/badge/Grade/67a328ac886445bf88e808becc35dece)](https://www.codacy.com/app/sumanmaity112/bahmni-mart?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=bahmni-msf/bahmni-mart&amp;utm_campaign=Badge_Grade)

Standalone Batch Application based on spring-batch. This application will create various table in **analytics** DB like Patient Information, Program Enrollment Information, Drug Orders (TB and Non-TB), Various forms filled by the users (Observation Templates), Bacteriology forms information

---
## Setting up Dev Environment

### Prerequisites
* **java 8**

### Setting up Git Hooks and Test DB
To setup git hooks please run the following command
```bash
sh scripts/dev/setup.sh
```
We have 2 githooks configured so far
1. pre-commit hook to check the message formatting
2. pre-push hook to prompt developer to increment the build version on master branch with every new feature change

---
### Build 
#### JAR
To build JAR run the following command
```bash
./gradlew clean build
```

---
### Checks/ Verifications
#### Test
To run tests run the following command
```bash
./gradlew test
```

#### Coverage
To check code coverage run the following command
```bash
./gradlew clean jacocoTestCoverageVerification
```
 
#### Style
To check code style run the following command
```bash
./gradlew clean checkstyleMain checkstyleTest
```

#### All
To run all chesks present in bahmni mart run the following command
```bash
./gradlew clean check
```

---
## Deployment Steps
### Prerequisites
* **Ansible Playbook (v2.2)**. To install it, run the following command 
    ```bash
    yum install ansible-2.2.0.0
    ```
### Installation Steps

To install bahmni-mart follow the steps given below
* Download Bahmni-mart playbook from Github
    ```bash
    wget -O /tmp/bahmni-mart-playbook.zip https://github.com/bahmni-msf/bahmni-mart-playbook/archive/master.zip && unzip -o /tmp/bahmni-mart-playbook.zip -d /tmp && sudo rm -rf /etc/bahmni-mart-playbook && sudo mv /tmp/bahmni-mart-playbook-master /etc/bahmni-mart-playbook && rm -rf /tmp/bahmni-mart-playbook.zip
    ```
* Update **/etc/bahmni-mart-playbook/inventories/bahmni-mart** inventory file as per your requirement
* Update the values presents in **/etc/bahmni-mart-playbook/setup.yml** inventory file as per your requirement
* Before running the installtion please add the following parameters in setup.yml file

  * analytics_db_password
  * openmrs_db_password
  * metabase_db_password
  
  **Note** : Password should be a Non Empty string. 
* HTTPS for metabase (optional)
  * Let's encrypt
    * Certificates generated from [let's encrypt](https://bahmni.atlassian.net/wiki/spaces/BAH/pages/35586093/Configure+Valid+SSL+Certificates) can be used for metabase by converting them into jks format.
    Update the following properties in setup.yml to run metabase with https.
    
    |Property | Comment |
    |:-----------|:---------|
    | metabase_with_ssl | Set to true to add ssl certificate for metabase. When this is true, the properties **bahmni_lets_encrypt_cert_dir, metabase_keystore_password** should be provided. Default value is 'false'
    | bahmni_lets_encrypt_cert_dir | Let's encrypt certificates directory, it is mandatory if **metabase_with_ssl** set to true. Eg: /etc/letsencrypt/live/demo.bahmni.org
    | metabase_keystore_password | Some password to generate jks file, it is mandatory if **metabase_with_ssl** set to true
   
   Since let's encrypt certificates expires after 90 days, you need to regenerate jks file after renewing bahmni certificates. Use following command to regenerate jks file
    
    ```bash
    /opt/bahmni-mart/bin/pemtojks.sh <bahmni_lets_encrypt_cert_dir> <metabase_keystore_password>
    ```
    
    Stop metabase container and update metabase docker container
    
    ```bash
    docker-compose -f /opt/bahmni-mart/metabase-ssl-docker-compose.yml up -d
    ```
  * Custom ssl 
    * If you use other than let's encrypt certificates, generate jks(Java Key Store) file from your ssl certificate and provide jks file path in **custom_keystore_location** and provide the **metabase_keystore_password**(password which was used to generating jks file)  
### Command to deploy mart
 #### Metabase without ssl
```bash 
ansible-playbook -i /etc/bahmni-mart-playbook/inventories/bahmni-mart /etc/bahmni-mart-playbook/all.yml --extra-vars '@/etc/bahmni-mart-playbook/setup.yml' --skip-tags "custom_ssl,lets_encrypt_ssl,authentication" -vv
```
#### Metabase with let's encrypt ssl(optional)
```bash
ansible-playbook -i /etc/bahmni-mart-playbook/inventories/bahmni-mart /etc/bahmni-mart-playbook/all.yml --extra-vars '@/etc/bahmni-mart-playbook/setup.yml' --skip-tags "without_ssl,custom_ssl,authentication" -vv
```
#### Metabase with custom ssl(optional)
```bash
ansible-playbook -i /etc/bahmni-mart-playbook/inventories/bahmni-mart /etc/bahmni-mart-playbook/all.yml --extra-vars '@/etc/bahmni-mart-playbook/setup.yml' --skip-tags "without_ssl,lets_encrypt_ssl,authentication" -vv
```

Note: Above playbook deploys [bahmni-mart](https://github.com/bahmni-msf/bahmni-mart) along with [metabase](https://metabase.com)(docker container) and [spring cloud data flow server](https://cloud.spring.io/spring-cloud-dataflow/)(docker container)
* Update **bahmni-mart** config. The config will be present in **/var/www/bahmni_config/bahmni-mart/bahmni-mart.json** 

### Access the Application
**Bahmni-mart** application should be installed on you system. To create flattened DB from CLI, run ```bahmni-mart``` command.

You can access **Bahmni-mart** and other application from browser also.

|Application | URL | Comment | 
|:-----------|:------|:---------|
|Bahmni mart  |http://\<HOST NAME>:9393/dashboard/#/tasks/definitions/launch/create-bahmni-mart| Only if bahmni-mart-scdf is installed|
|Metabase|http://\<HOST NAME>:9003|Only if metabase is installed|

---
### Commands to Remember
* ```docker ps -a``` (Check status of docker containers)
* ```docker logs -f metabase``` (Check logs of metabase container)
* ```docker logs -f dataflow-server``` (Check logs of dataflow-server container)
* ```docker stop <CONATAINER NAME>``` (Stop specific docker container)
* ```docker start <CONTAINER NAME>``` (Start specific docker container)
* ```curl -X POST <HOST NAME>:9393/tasks/executions\?name\=create-bahmni-mart``` (Once Spring Cloud Dataflow Server is up, you can launch task using UI. But if you want to do it from command line you can run this command. It will launch **create-bahmni-mart** task)
* ```tail -100f /var/log/bahmni-mart/bahmmni-mart.log``` (Log of **create-bahmni-mart** task will be present in **/var/log/bahmni-mart/bahmni-mart.log** of your host machine. Use this command to get last 100 lines of the file)

### Implementers Note
For implementers note please check [here](https://docs.google.com/document/d/1NClHML9rabkS6KwXXUMOgLhJmo9kOvJkMTgewXLyPTo/edit?usp=sharing).
### Bahmni Mart dev setup
### To run bahmni-mart from IDE and debug the code please follow the below steps
* Update ANALYTICS_DB_PASSWORD,  OPENMRS_DB_PASSWORD and vagrant IP in ```application-dev.properties```
* Set VM options to ```"-Dspring.profiles.active=dev"``` in the run configurations in IDE
* Replace conf/app.json, conf/extension.json, conf/bahmni-mart.json with respective implementation specific config file
* Connect to both openmrs and analytics databases from IDE. Before that do below configurations to connect to respective databases
    * Grant privileges from vagrant to be able to connect to openmrs db as root user
    
        ```mysql -uroot -ppassword openmrs```
    
        ```GRANT ALL PRIVILEGES ON openmrs.* to 'root'@'%' identified by 'password';```
    
        ```flush privileges;```
    * Edit pg_hba.conf to be able to access analytics db from outside
    
        ``` vi /var/lib/pgsql/9.2/data/pg_hba.conf ```
    
        ``` host  all    all   192.168.33.11/0 trust ```
    
* Once connections to both db’s are successful run the application from IDE
    
### To run the Integration tests from IDE
* Update ANALYTICS_DB_PASSWORD,  OPENMRS_DB_PASSWORD and vagrant IP in application-test.properties
* Create below databases from inside vagrant 
* mysqlTesytDb : (testMysql.sql) 

    ```mysql -uroot -ppasword ```
    
    ```CREATE DATABASE IF NOT EXISTS test_openmrs;```
    
    ```CREATE USER 'test_user'@'%' IDENTIFIED BY 'password';```
* psqlTestDb :  (psqlTestSetup.sql)

    ```psql	-Uanalytics```
    
    ```CREATE USER test_user WITH PASSWORD 'password' NOCREATEROLE SUPERUSER;```
    
    ```CREATE DATABASE test_analytics WITH OWNER test_user;```
