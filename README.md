# openfire-roomservice-plugin

Openfire plugin to allows administration of rooms via HTTP requests.

### Install

```
cd MAVEN_OPENFIRE_PLUGIN_HOME
mvn install

cd OPENFIRE_SRC_HOME/build
ant
cd ..
mvn install:install-file -DgroupId=org.igniterealtime.openfire -DartifactId=openfire -Dversion=3.8.2 -Dpackaging=jar -DgeneratePom=true  -Dfile=target/openfire/lib/openfire.jar

cd OPENFIRE_ROOMSERVICE_PLUGIN_HOME
mvn package
```

`$OPENFIRE_ROOMSERVICE_PLUGIN_HOME/target/roomservice.jar` is what you
need.

### Usage

Configuration page included into plugin like userservice plugin.
The service address is [hostname]/plugins/roomservice/roomservice
Parameters:
* jid
* roomname
* secret
* subdomain
* type

Supported type: 'add' or 'delete'.

Reference http://www.igniterealtime.org/projects/openfire/plugins/userservice/readme.html

Remember to configure plugin in Openfire admin console.
