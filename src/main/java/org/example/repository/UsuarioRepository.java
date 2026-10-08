package org.example.repository;

import org.example.model.Usuario;
import org.example.service.UsuarioService;

import java.util.Optional;

public interface UsuarioRepository  {
    void salvar(Usuario usuario);
    Optional<Usuario>findByEmail(String email);
    boolean existeEmail(String email);
}


