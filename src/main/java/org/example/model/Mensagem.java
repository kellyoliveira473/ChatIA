package org.example.model;



public class Mensagem {
  private Object id;
  private String texto;
  private boolean isUsuario;

    public Mensagem() {}

    public Mensagem(String texto, boolean isUsuario) {
        this.texto = texto;
        this.isUsuario = isUsuario;
    }
    public Object getId(){
        return id;
    }
    public void setId(Object ID ){
        this.id=id;
    }
    public String getTexto(){
        return texto;
    }
    public void setTexto(String texto){
        this.texto= texto;
    }
    public boolean isUsuario(){
        return  isUsuario;
    }
    public void setUsuario(boolean usuario){
        isUsuario=usuario;
    }



}
