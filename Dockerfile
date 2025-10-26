# ---- build ----
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle installDist --no-daemon

# ---- run ----
FROM amazoncorretto:21-alpine3.20-jdk
WORKDIR /app
COPY --from=build /app/build/install/* /app/
EXPOSE 10000
ENV PORT=10000
CMD ["/app/bin/gourmet-mafia"]
