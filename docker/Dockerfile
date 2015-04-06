FROM java:8

EXPOSE 4000

ADD tonic.jar tonic.jar

ENV UPSTREAM_URL http://localhost:8000

CMD java -jar tonic.jar 4000 http://localhost:8000
