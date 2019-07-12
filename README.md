# ElasticSearch 7 Spring-Boot Client
Project uses Java High Level REST Client of Elastic Search to perform CRUD operation.
The Java High Level REST Client works on top of the Java Low Level REST client. 
Its main goal is to expose API specific methods, that accept request objects as an argument and return response objects, so that request marshalling and response un-marshalling is handled by the client itself.

Fir further reference please check :
https://www.elastic.co/guide/en/elasticsearch/client/java-rest/current/java-rest-high.html

elasticsearch.host : change it to point to hostname of the cluster in application.properties
server.port=8102

It contains the below examples ::
1. Create Index
2. Insert Document
2. Update Document
4. Delete Document
5. Search Document
6. Create Autocomplete Index with Mappings
7. Autocomplete Search


