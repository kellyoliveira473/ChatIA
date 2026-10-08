package org.example.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.example.config.DatabaseConfig;
import org.example.model.Usuario;

import java.util.Optional;

public class UsuarioRepositoryMongo implements UsuarioRepository {

    private final MongoCollection<Usuario> collection;

    public UsuarioRepositoryMongo() {

        this.collection = DatabaseConfig.getDatabase().getCollection("users", Usuario.class);
    }

    @Override
    public void salvar(Usuario usuario) {
        collection.insertOne(usuario);
    }

    @Override
    public Optional<Usuario> findByEmail(String email) {
        Usuario usuario = collection.find(Filters.eq("email", email)).first();
        return Optional.ofNullable(usuario);
    }

    @Override
    public boolean existeEmail(String email) {
        return false;
    }


}