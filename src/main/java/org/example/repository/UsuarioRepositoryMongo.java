package org.example.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
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

    @Override
    public void atualizarNome(String email, String novoNome) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("E-mail inválido");
        }

        String emailFormatado = email.trim().toLowerCase();

        // Atualiza somente o campo "nome"; senha e demais dados ficam intactos
        UpdateResult resultado = collection.updateOne(
                Filters.eq("email", emailFormatado),
                Updates.set("nome", novoNome)
        );

        if (resultado.getMatchedCount() == 0) {
            throw new RuntimeException("Usuário não encontrado");
        }
    }
}