package org.example.service;

import org.example.model.Usuario;
import org.example.repository.UsuarioRepository;
import org.mindrot.jbcrypt.BCrypt;

public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public void cadastrar(Usuario usuario, String confirmacao) {
        if (usuario.getNome() == null || usuario.getNome().isBlank()) {
            throw new RuntimeException("Nome é obrigatório");
        }
        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new RuntimeException("E-mail é obrigatório");
        }
        if (usuario.getSenha() == null || usuario.getSenha().isBlank()) {
            throw new RuntimeException("Senha é obrigatória");
        }
        if (!usuario.getSenha().equals(confirmacao)) {
            throw new RuntimeException("As senhas não coincidem");
        }

        // Verifica se o e-mail já está cadastrado no banco
        Usuario usuarioExistente = usuarioRepository.buscarPorEmail(usuario.getEmail());
        if (usuarioExistente != null) {
            throw new RuntimeException("Este e-mail já está cadastrado.");
        }

        // Criptografa a senha com jBCrypt antes de salvar no MongoDB
        String senhaHash = BCrypt.hashpw(usuario.getSenha(), BCrypt.gensalt());
        usuario.setSenha(senhaHash);

        usuarioRepository.salvar(usuario);
    }

    public Usuario autenticar(String email, String senha) {
        if (email == null || email.isBlank()) {
            throw new RuntimeException("E-mail é obrigatório");
        }
        if (senha == null || senha.isBlank()) {
            throw new RuntimeException("Senha é obrigatória");
        }

        Usuario usuario = usuarioRepository.buscarPorEmail(email);

        // Valida se o usuário existe e se a senha confere com o hash do banco
        if (usuario == null || !BCrypt.checkpw(senha, usuario.getSenha())) {
            throw new RuntimeException("E-mail ou senha inválidos");
        }

        return usuario;
    }
}