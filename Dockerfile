# Analysis Dashboard imaji. Iki asama: JDK'da derlenir, yalnizca JRE ve jar ile calisir.
# Testler burada kosmaz (CI'da ve gelistirmede Testcontainers ile kosar); imaj derlemesi Docker-in-Docker istemesin.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /app
# Once yalnizca Maven dosyalari: bagimliliklar kaynak degismedikce onbellekten gelir
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:17-jre
# Veritabani saatleri UTC; ekran ve vardiya sinirlari sunucunun saat dilimine gore (fabrika saati)
ENV TZ=Europe/Istanbul
# Uygulama root olarak calismaz
RUN useradd --system --no-create-home --uid 10001 dashboard
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
USER dashboard
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
