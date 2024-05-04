#docker build -t werleja1/vorlesungsbeispiel . 
#docker run -p 9000:8080 -d werleja1/vorlesungsbeispiel
#az group create --name mdm-spring-playground --location switzerlandnorth
#az appservice plan create --name mdm-spring-playground --resource-group mdm-spring-playground --sku F1 --is-linux
#az webapp create --resource-group mdm-spring-playground --plan mdm-spring-playground --name mdm-werle1-playground --deployment-container-image-name werleja1/vorlesungsbeispiel:latest
#az webapp config appsettings set --resource-group mdm-spring-playground --name mdm-werle1-playground --setting WEBSITES_PORT=8080

FROM openjdk:21-jdk-slim

# Copy Files
WORKDIR /usr/src/app

# Install FFmpeg
RUN apt-get update && \
    apt-get install -y ffmpeg
    
COPY . .



# Install
RUN ./mvnw -Dmaven.test.skip=true package

# Docker Run Command
EXPOSE 8080
CMD ["java","-jar","/usr/src/app/target/vorlesungsbeispiel-0.0.1-SNAPSHOT.jar"]