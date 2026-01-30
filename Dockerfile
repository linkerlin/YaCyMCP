FROM openjdk:17-jdk-slim

WORKDIR /app

# Copy the jar file
COPY target/yacy-mcp-1.0.0-SNAPSHOT.jar app.jar

# Create data directory
RUN mkdir -p /app/data

# Expose port
EXPOSE 8080

# Set environment variables
ENV JAVA_OPTS=""

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
