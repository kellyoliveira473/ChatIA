package org.example.model;


import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class Conversas {
  private Object id;
  private  String usuarioEmail;
  private String titulo;
  private boolean favorito;
  private List<Mensagem>mensagems= new ArrayList<>();

    public Conversas() {}

    public Conversas(String usuarioEmail, String titulo) {
        this.usuarioEmail = usuarioEmail;
        this.titulo = titulo;
        this.favorito = false;
    }

    public ObjectId getId() { return (ObjectId) id; }
    public void setId(ObjectId id) { this.id = id; }

    public String getUsuarioEmail() { return usuarioEmail; }
    public void setUsuarioEmail(String usuarioEmail) { this.usuarioEmail = usuarioEmail; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public boolean isFavorito() { return favorito; }
    public void setFavorito(boolean favorito) { this.favorito = favorito; }

    public List<Mensagem> getMensagens() { return mensagems; }
    public void setMensagens(List<Mensagem> mensagens) { this.mensagems = mensagens; }
}

