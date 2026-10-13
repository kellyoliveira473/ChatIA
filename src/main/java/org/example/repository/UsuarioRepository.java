package org.example.repository;

import org.example.model.Usuario;

public interface UsuarioRepository {
    void salvar(Usuario usuario);
    Usuario buscarPorEmail(String email);
}