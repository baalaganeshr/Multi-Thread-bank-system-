FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY src/ src/
RUN mkdir -p out && \
    javac -d out \
    src/com/banksystem/util/*.java \
    src/com/banksystem/model/*.java \
    src/com/banksystem/threading/*.java \
    src/com/banksystem/service/*.java \
    src/com/banksystem/web/*.java \
    src/com/banksystem/WebMain.java

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/out/ out/
COPY web/ web/
EXPOSE 8080
CMD ["java", "-cp", "out", "com.banksystem.WebMain"]
