package org.example.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public class DatabaseConfig {

    private static final String DEFAULT_DATABASE_NAME = "chat_ia_db";

    private static MongoClient mongoClient;
    private static MongoDatabase database;

    public static MongoDatabase getDatabase() {
        if (database == null) {
            String uri = System.getenv("MONGODB_URI");

            if (uri == null || uri.isBlank()) {
                throw new IllegalStateException(
                        "MONGODB_URI não definida. Configure a variável de ambiente.");
            }

            // Mapeamento automático de POJOs
            CodecRegistry pojoCodecRegistry = fromProviders(
                    PojoCodecProvider.builder().automatic(true).build()
            );

            CodecRegistry codecRegistry = fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    pojoCodecRegistry
            );

            ConnectionString connectionString = new ConnectionString(uri);

            // Mostra o usuário usado (a senha nunca é impressa)
            System.out.println("-> Mongo usando usuario: "
                    + (connectionString.getCredential() != null
                    ? connectionString.getCredential().getUserName()
                    : "nenhum"));

            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .codecRegistry(codecRegistry)
                    .build();

            mongoClient = MongoClients.create(settings);

            String databaseName = connectionString.getDatabase() != null
                    ? connectionString.getDatabase()
                    : DEFAULT_DATABASE_NAME;

            database = mongoClient.getDatabase(databaseName);

            testarConexao();
        }
        return database;
    }

    // Faz uma operação real para forçar a autenticação e mostrar o erro verdadeiro
    private static void testarConexao() {
        try {
            database.runCommand(new Document("ping", 1));
            System.out.println("-> MongoDB: conectou e autenticou com sucesso!");
        } catch (Exception e) {
            Throwable causa = e;
            while (causa.getCause() != null) {
                causa = causa.getCause();
            }
            System.out.println("-> MongoDB FALHOU: " + causa.getMessage());
        }
    }

    public static void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
        }
    }
}