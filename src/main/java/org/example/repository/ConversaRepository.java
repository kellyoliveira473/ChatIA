package org.example.repository;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.example.config.DatabaseConfig;
import org.example.model.Conversas;
import org.example.model.Mensagem;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ConversaRepository {

    private MongoCollection<Document> getCollection() {
        return DatabaseConfig.getDatabase().getCollection("conversas");
    }

    public void salvar(Conversas conversa) {
        Document doc = new Document()
                .append("usuarioEmail", conversa.getUsuarioEmail())
                .append("titulo", conversa.getTitulo())
                .append("favorito", conversa.isFavorito())
                .append("mensagens", conversa.getMensagens().stream().map(m ->
                        new Document("texto", m.getTexto()).append("isUsuario", m.isUsuario())
                ).collect(Collectors.toList()));

        if (conversa.getId() == null) {
            conversa.setId(new ObjectId());
            doc.append("_id", conversa.getId());
            getCollection().insertOne(doc);
        } else {
            getCollection().replaceOne(Filters.eq("_id", conversa.getId()), doc);
        }
    }

    public List<Conversas> buscarPorUsuario(String email) {
        List<Conversas> conversas = new ArrayList<>();
        for (Document doc : getCollection().find(Filters.eq("usuarioEmail", email))) {
            Conversas c = new Conversas();
            c.setId(doc.getObjectId("_id"));
            c.setUsuarioEmail(doc.getString("usuarioEmail"));
            c.setTitulo(doc.getString("titulo"));
            c.setFavorito(doc.getBoolean("favorito", false));

            List<Document> msgDocs = doc.getList("mensagens", Document.class);
            if (msgDocs != null) {
                List<Mensagem> mensagens = new ArrayList<>();
                for (Document md : msgDocs) {
                    mensagens.add(new Mensagem(md.getString("texto"), md.getBoolean("isUsuario")));
                }
                c.setMensagens(mensagens);
            }
            conversas.add(c);
        }
        return conversas;
    }

    public void deletar(ObjectId id) {
        getCollection().deleteOne(Filters.eq("_id", id));
    }
}