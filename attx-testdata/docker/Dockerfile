# NGINX for serving static data for ATTX integration tests.

FROM nginx:alpine

COPY data /usr/share/nginx/html
RUN chmod a+r /usr/share/nginx/html/*
