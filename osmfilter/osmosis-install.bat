@call mvn install:install-file -DgroupId=org.openstreetmap.osmosis -DartifactId=osmosis-core -Dversion=0.40.1 -Dpackaging=jar -Dfile=D:\OSM\osmosis-0.40.1\lib\default\osmosis-core-0.40.1.jar
@jar -cf D:\OSM\osmosis-0.40.1\src\core\src\main\sources.jar -C D:\OSM\osmosis-0.40.1\src\core\src\main\java org
@call mvn install:install-file -DgroupId=org.openstreetmap.osmosis -DartifactId=osmosis-core -Dversion=0.40.1 -Dpackaging=jar -Dclassifier=sources -Dfile=D:\OSM\osmosis-0.40.1\src\core\src\main\sources.jar
