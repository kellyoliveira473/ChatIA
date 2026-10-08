package org.example.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DatabaseConfig {

    private static final String DEFAULT_DATABASE_NAME = "chat_ia_db";
    // URI local padrão para quando MONGODB_URI não estiver no ambiente
    private static final String DEFAULT_URI = "mongodb://localhost:27017/" + DEFAULT_DATABASE_NAME;

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            String uri = System.getenv("MONGODB_URI");

            // Fallback: se a variável de ambiente estiver vazia, usa a URI padrão local
            if (uri == null || uri.isBlank()) {
                System.out.println("⚠️ MONGODB_URI não definida no ambiente. Utilizando URI local padrão.");
                uri = DEFAULT_URI;
            }

            // Configura o mapeamento automático de POJOs
            CodecRegistry pojoCodecRegistry = fromProviders(
                    PojoCodecProvider.builder().automatic(true).build()
            );

            CodecRegistry codecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    pojoCodecRegistry
            );

            ConnectionString connectionString = new ConnectionString(uri);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .codecRegistry(codecRegistry)
                    .build();

            mongoClient = MongoClients.create(settings);

            String databaseName = connectionString.getDatabase() != null
                    ? connectionString.getDatabase()
                    : DEFAULT_DATABASE_NAME;

            database = mongoClient.getDatabase(databaseName);
        }
        return database;
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }
}