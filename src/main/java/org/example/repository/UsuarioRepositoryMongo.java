package org.example.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.example.config.DatabaseConfig;
import org.example.model.Usuario;

public class UsuarioRepositoryMongo implements UsuarioRepository {

    private final MongoCollection<Usuario> collection;

    public UsuarioRepositoryMongo() {
        this.collection = DatabaseConfig.getDatabase().getCollection("users", Usuario.class);
    }

    @Override
    public void salvar(Usuario usuario) {
        if (usuario.getEmail() != null) {
            usuario.setEmail(usuario.getEmail().trim().toLowerCase());
        }
        collection.insertOne(usuario);
    }

    @Override
    public Usuario buscarPorEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        // Faz a busca no MongoDB ignorando maiúsculas/minúsculas e espaços
        String emailFormatado = email.trim().toLowerCase();

        return collection.find(Filters.eq("email", emailFormatado)).first();
    }
}