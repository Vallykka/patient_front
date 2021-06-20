FROM clojure:openjdk-16-lein-alpine as builder

WORKDIR /usr/src/patient_front/
COPY project.clj /usr/src/patient_front/
COPY resources/ /usr/src/patient_front/resources
COPY src/ /usr/src/patient_front/src

RUN lein with-profile prod cljsbuild once

FROM nginx:1.17.5
RUN rm /etc/nginx/nginx.conf
COPY nginx.conf /etc/nginx

RUN cd /etc/nginx \
    && chmod +x /etc/nginx/nginx.conf

WORKDIR /var/www/patient_front
COPY --from=builder /usr/src/patient_front/resources/public/css/ /var/www/patient_front/css/
COPY --from=builder /usr/src/patient_front/resources/public/js/ /var/www/patient_front/js/
COPY --from=builder /usr/src/patient_front/resources/public/index.html /var/www/patient_front/

# start nginx container
CMD /bin/bash -c "nginx -g 'daemon off;'"
