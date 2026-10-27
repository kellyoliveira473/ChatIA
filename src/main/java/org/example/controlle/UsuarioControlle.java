package org.example.controlle;

import org.example.model.Usuario;
import org.example.service.UsuarioService;

public class UsuarioControlle {

    private final UsuarioService usuarioService;

    public UsuarioControlle(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    public void cadastrar(Usuario usuario, String confirmacao) {
        usuarioService.cadastrar(usuario, confirmacao);
    }

    public Usuario autenticar(String email, String senha) {
        return usuarioService.autenticar(email, senha);
    }

    public void atualizarNome(Usuario usuario, String novoNome) {
        usuarioService.atualizarNome(usuario, novoNome);
    }
}